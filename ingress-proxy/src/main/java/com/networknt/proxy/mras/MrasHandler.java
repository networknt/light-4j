package com.networknt.proxy.mras;

import com.networknt.client.ClientConfig;
import com.networknt.client.Http2Client;
import com.networknt.client.oauth.TokenResponse;
import com.networknt.client.ssl.ClientX509ExtendedTrustManager;
import com.networknt.client.ssl.TLSConfig;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.config.TlsUtil;
import com.networknt.handler.Handler;
import com.networknt.handler.HandlerUtils;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.config.UrlRewriteRule;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.common.ContentType;
import com.networknt.metrics.MetricsConfig;
import com.networknt.metrics.AbstractMetricsHandler;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
import com.networknt.status.Status;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * https://www.munichre.com/automation-solutions/en.html
 * This is a third party service provider for insurance company and one of our customers is using this service.
 *
 * In order to access the service, the authentication flow is a customized one, and we are handling it in this
 * middleware handler. Like the safesforce, we will also invoke the API after the authentication is done in the
 * same context.
 *
 * @author Steve Hu
 */
public class MrasHandler implements MiddlewareHandler {
    private static final Logger logger = LoggerFactory.getLogger(MrasHandler.class);
    private static final String TLS_TRUSTSTORE_ERROR = "ERR10055";
    private static final String OAUTH_SERVER_URL_ERROR = "ERR10056";
    private static final String ESTABLISH_CONNECTION_ERROR = "ERR10053";
    private static final String GET_TOKEN_ERROR = "ERR10052";
    private static final String METHOD_NOT_ALLOWED  = "ERR10008";

    private static AbstractMetricsHandler metricsHandler;

    private volatile HttpHandler next;
    private static MrasConfig config;
    // the cached jwt token so that we can use the same token for different requests.
    private String accessToken;
    private String microsoft;
    // the expiration time of access token in millisecond to control if we need to renew the token.
    private long accessTokenExpiration = 0;
    private long microsoftExpiration = 0;

    private HttpClient client;
    private HttpClient clientMicrosoft;

    public MrasHandler() {
        config = MrasConfig.load();
        if(config.isMetricsInjection()) {
            // get the metrics handler from the handler chain for metrics registration. If we cannot get the
            // metrics handler, then an error message will be logged.
            Map<String, HttpHandler> handlers = Handler.getHandlers();
            metricsHandler = (AbstractMetricsHandler) handlers.get(MetricsConfig.CONFIG_NAME);
            if(metricsHandler == null) {
                logger.error("An instance of MetricsHandler is not configured in the handler.yml.");
            }
        }
        if(logger.isInfoEnabled()) logger.info("MrasHandler is loaded.");
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void register() {
        // As certPassword is in the config file, we need to mask them.
        List<String> masks = new ArrayList<>();
        masks.add("keyStorePass");
        masks.add("keyPass");
        masks.add("trustStorePass");
        masks.add("password");
        // use a new no cache instance to avoid the default config to be overwritten.
        ModuleRegistry.registerModule(MrasConfig.CONFIG_NAME, MrasHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(MrasConfig.CONFIG_NAME), masks);
    }

    @Override
    public void reload() {
        config.reload();
        if(config.isMetricsInjection()) {
            // get the metrics handler from the handler chain for metrics registration. If we cannot get the
            // metrics handler, then an error message will be logged.
            Map<String, HttpHandler> handlers = Handler.getHandlers();
            metricsHandler = (AbstractMetricsHandler) handlers.get(MetricsConfig.CONFIG_NAME);
            if(metricsHandler == null) {
                logger.error("An instance of MetricsHandler is not configured in the handler.yml.");
            }
        }
        List<String> masks = new ArrayList<>();
        masks.add("keyStorePass");
        masks.add("keyPass");
        masks.add("trustStorePass");
        masks.add("password");
        // use a new no cache instance to avoid the default config to be overwritten.
        ModuleRegistry.registerModule(MrasConfig.CONFIG_NAME, MrasHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(MrasConfig.CONFIG_NAME), masks);
        if(logger.isInfoEnabled()) logger.info("MrasHandler is reloaded.");
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("MrasHandler.handleRequest starts.");
        long startTime = System.nanoTime();
        String requestPath = exchange.getRequestPath();
        if(logger.isTraceEnabled()) logger.trace("original requestPath = " + requestPath);

        for(String key: config.getPathPrefixAuth().keySet()) {
            if(requestPath.startsWith(key)) {
                String endpoint = key + "@" + exchange.getRequestMethod().toString().toLowerCase();
                if(logger.isTraceEnabled()) logger.trace("endpoint = " + endpoint);
                // handle the url rewrite here.
                if(config.getUrlRewriteRules() != null && config.getUrlRewriteRules().size() > 0) {
                    boolean matched = false;
                    for(UrlRewriteRule rule : config.getUrlRewriteRules()) {
                        Matcher matcher = rule.getPattern().matcher(requestPath);
                        if(matcher.matches()) {
                            matched = true;
                            requestPath = matcher.replaceAll(rule.getReplace());
                            if(logger.isTraceEnabled()) logger.trace("rewritten requestPath = " + requestPath);
                            break;
                        }
                    }
                    // if no matched rule in the list, use the original requestPath.
                    if(!matched) requestPath = exchange.getRequestPath();
                } else {
                    // there is no url rewrite rules, so use the original requestPath
                    requestPath = exchange.getRequestPath();
                }
                // iterate the key set from the pathPrefixAuth map.
                if(config.getPathPrefixAuth().get(key).equals(config.ACCESS_TOKEN)) {
                    // private access token for authentication.
                    if(System.currentTimeMillis() >= (accessTokenExpiration - 5000)) { // leave 5 seconds room.
                        if(logger.isTraceEnabled()) logger.trace("accessToken is about or already expired. current time = " + System.currentTimeMillis() + " expiration = " + accessTokenExpiration);
                        Result<TokenResponse> result = getAccessToken();
                        if(result.isSuccess()) {
                            accessTokenExpiration = System.currentTimeMillis() + 300 * 1000;
                            accessToken = result.getResult().getAccessToken();
                        } else {
                            setExchangeStatus(exchange, result.getError());
                            if(logger.isDebugEnabled()) logger.debug("MrasHandler.handleRequest ends with an error.");
                            return;
                        }
                    }

                    // Audit log the endpoint info
                    HandlerUtils.populateAuditAttachmentField(exchange, Constants.ENDPOINT_STRING, endpoint);

                    invokeApi(exchange, (String)config.getAccessToken().get(config.SERVICE_HOST), requestPath, "Bearer " + accessToken, startTime, endpoint);
                    if(logger.isDebugEnabled()) logger.debug("MrasHandler.handleRequest ends.");
                    return;
                } else if(config.getPathPrefixAuth().get(key).equals(config.BASIC_AUTH)) {
                    // only basic authentication is used for the access.
                    invokeApi(exchange, (String)config.getBasicAuth().get(config.SERVICE_HOST), requestPath, "Basic " + encodeCredentials((String)config.getBasicAuth().get(config.USERNAME), (String)config.getBasicAuth().get(config.PASSWORD)), startTime, endpoint);
                    if(logger.isDebugEnabled()) logger.debug("MrasHandler.handleRequest ends.");
                    return;
                } else if(config.getPathPrefixAuth().get(key).equals(config.ANONYMOUS)) {
                    // no authorization header for this type of the request.
                    invokeApi(exchange, (String)config.getAnonymous().get(config.SERVICE_HOST), requestPath, null, startTime, endpoint);
                    if(logger.isDebugEnabled()) logger.debug("MrasHandler.handleRequest ends.");
                    return;
                } else if(config.getPathPrefixAuth().get(key).equals(config.MICROSOFT)) {
                    // microsoft access token for authentication.
                    if(System.currentTimeMillis() >= (microsoftExpiration - 50000)) { // leave 50 seconds room.
                        if(logger.isTraceEnabled()) logger.trace("microsoft token is about or already expired. current time = " + System.currentTimeMillis() + " expiration = " + microsoftExpiration);
                        Result<TokenResponse> result = getMicrosoftToken();
                        if(result.isSuccess()) {
                            microsoftExpiration = System.currentTimeMillis() + result.getResult().getExpiresIn() * 1000;
                            microsoft = result.getResult().getAccessToken();
                        } else {
                            setExchangeStatus(exchange, result.getError());
                            if(logger.isDebugEnabled()) logger.debug("MrasHandler.handleRequest ends with an error.");
                            return;
                        }
                    }
                    invokeApi(exchange, (String)config.getMicrosoft().get(config.SERVICE_HOST), requestPath, "Bearer " + microsoft, startTime, endpoint);
                    if(logger.isDebugEnabled()) logger.debug("MrasHandler.handleRequest ends.");
                    return;
                }
            }
        }
        // not the MRAS path, go to the next middleware handlers.

        if(logger.isDebugEnabled()) logger.debug("MrasHandler.handleRequest ends.");
        Handler.next(exchange, next);
    }

    private void invokeApi(HttpServerExchange exchange, String serviceHost, String requestPath, String authorization, long startTime, String endpoint) throws Exception {
        // call the MRAS API directly here with the token from the cache.
        String method = exchange.getRequestMethod().toString();
        String queryString = exchange.getQueryString();
        String contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
        if(contentType == null) contentType = ContentType.APPLICATION_JSON.value();
        if(logger.isTraceEnabled()) logger.trace("Access MRAS API with method = " + method + " requestHost = " + serviceHost + " queryString = " + queryString + " contentType = " + contentType);
        HttpRequest request = null;
        if(method.equalsIgnoreCase("GET")) {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(new URI(serviceHost + requestPath + "?" + queryString))
                    .GET();
            if(authorization != null) {
                builder.headers("Authorization", authorization, "Content-Type", contentType);
            } else {
                builder.header("Content-Type", contentType);
            }
            request = builder.build();
        } else if(method.equalsIgnoreCase("DELETE")) {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(new URI(serviceHost + requestPath + "?" + queryString))
                    .DELETE();
            if(authorization != null) {
                builder.headers("Authorization", authorization, "Content-Type", contentType);
            } else {
                builder.header("Content-Type", contentType);
            }
            request = builder.build();
        } else if(method.equalsIgnoreCase("POST")) {
            String bodyString = exchange.getAttachment(AttachmentConstants.REQUEST_BODY_STRING);
            if(bodyString == null && logger.isDebugEnabled()) logger.debug("The request body is null and the request path might be missing in request-injection.appliedBodyInjectionPathPrefixes.");
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(new URI(serviceHost + requestPath))
                    .POST(bodyString == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(bodyString));
            if(authorization != null) {
                builder.headers("Authorization", authorization, "Content-Type", contentType);
            } else {
                builder.header("Content-Type", contentType);
            }
            request = builder.build();
        } else if(method.equalsIgnoreCase("PUT")) {
            String bodyString = exchange.getAttachment(AttachmentConstants.REQUEST_BODY_STRING);
            if(bodyString == null && logger.isDebugEnabled()) logger.debug("The request body is null and the request path might be missing in request-injection.appliedBodyInjectionPathPrefixes.");
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(new URI(serviceHost + requestPath))
                    .PUT(bodyString == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(bodyString));
            if(authorization != null) {
                builder.headers("Authorization", authorization, "Content-Type", contentType);
            } else {
                builder.header("Content-Type", contentType);
            }
            request = builder.build();
        } else if(method.equalsIgnoreCase("PATCH")) {
            String bodyString = exchange.getAttachment(AttachmentConstants.REQUEST_BODY_STRING);
            if(bodyString == null && logger.isDebugEnabled()) logger.debug("The request body is null and the request path might be missing in request-injection.appliedBodyInjectionPathPrefixes.");
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(new URI(serviceHost + requestPath))
                    .method("PATCH", bodyString == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(bodyString));
            if(authorization != null) {
                builder.headers("Authorization", authorization, "Content-Type", contentType);
            } else {
                builder.header("Content-Type", contentType);
            }
            request = builder.build();
        } else {
            logger.error("wrong http method " + method + " for request path " + requestPath);
            setExchangeStatus(exchange, METHOD_NOT_ALLOWED, method, requestPath);
            return;
        }
        if(client == null) {
            try {
                HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofMillis(ClientConfig.get().getTimeout()))
                        // we cannot use the Http2Client SSL Context as we need two-way TLS here.
                        .sslContext(createSSLContext());
                if(config.getProxyHost() != null) clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(config.getProxyHost(), config.getProxyPort() == 0 ? 443 : config.getProxyPort())));
                if (config.isEnableHttp2()) {
                    clientBuilder.version(HttpClient.Version.HTTP_2);
                } else {
                    clientBuilder.version(HttpClient.Version.HTTP_1_1);
                }
                // this a workaround to bypass the hostname verification in jdk11 http client.
                Map<String, Object> tlsMap = (Map<String, Object>)ClientConfig.get().getMappedConfig().get(Http2Client.TLS);
                if(tlsMap != null && !Boolean.TRUE.equals(tlsMap.get(TLSConfig.VERIFY_HOSTNAME))) {
                    final Properties props = System.getProperties();
                    props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
                }
                client = clientBuilder.build();
            } catch (IOException e) {
                logger.error("Cannot create HttpClient:", e);
                setExchangeStatus(exchange, TLS_TRUSTSTORE_ERROR);
                return;
            }
        }
        HttpResponse<byte[]> response  = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        HttpHeaders responseHeaders = response.headers();
        byte[] responseBody = response.body();
        exchange.setStatusCode(response.statusCode());
        for (Map.Entry<String, List<String>> header : responseHeaders.map().entrySet()) {
            // remove empty key in the response header start with a colon.
            if(header.getKey() != null && !header.getKey().startsWith(":") && header.getValue().get(0) != null) {
                for(String s : header.getValue()) {
                    if(logger.isTraceEnabled()) logger.trace("copy response header key = " + header.getKey() + " value = " + s);
                    exchange.getResponseHeaders().add(new HttpString(header.getKey()), s);
                }
            }
        }
        exchange.getResponseSender().send(ByteBuffer.wrap(responseBody));
        if(config.isMetricsInjection() && metricsHandler != null) {
            if(logger.isTraceEnabled()) logger.trace("inject metrics for " + config.getMetricsName());
            metricsHandler.injectMetrics(exchange, startTime, config.getMetricsName(), endpoint);
        }
    }

    private Result<TokenResponse> getAccessToken() throws Exception {
        TokenResponse tokenResponse = null;
        if(client == null) {
            try {
                HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofMillis(ClientConfig.get().getTimeout()))
                        // we cannot use the Http2Client SSL Context as we need two-way TLS here.
                        .sslContext(createSSLContext());
                if(config.getProxyHost() != null) clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(config.getProxyHost(), config.getProxyPort() == 0 ? 443 : config.getProxyPort())));
                if (config.isEnableHttp2()) {
                    clientBuilder.version(HttpClient.Version.HTTP_2);
                } else {
                    clientBuilder.version(HttpClient.Version.HTTP_1_1);
                }
                // this a workaround to bypass the hostname verification in jdk11 http client.
                Map<String, Object> tlsMap = (Map<String, Object>)ClientConfig.get().getMappedConfig().get(Http2Client.TLS);
                if(tlsMap != null && !Boolean.TRUE.equals(tlsMap.get(TLSConfig.VERIFY_HOSTNAME))) {
                    final Properties props = System.getProperties();
                    props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
                }
                client = clientBuilder.build();
            } catch (IOException e) {
                logger.error("Cannot create HttpClient:", e);
                return Failure.of(new Status(TLS_TRUSTSTORE_ERROR));
            }
        }
        try {
            String serverUrl = (String)config.getAccessToken().get(config.TOKEN_URL);
            if(serverUrl == null) {
                return Failure.of(new Status(OAUTH_SERVER_URL_ERROR, "accessToken.tokenUrl"));
            }

            Map<String, String> parameters = new HashMap<>();
            parameters.put("grant_type", "client_credentials");

            String form = parameters.entrySet()
                    .stream()
                    .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl))
                    .headers("Content-Type", "application/x-www-form-urlencoded", "Authorization", "BASIC " + encodeCredentials((String)config.getAccessToken().get(config.USERNAME), (String)config.getAccessToken().get(config.PASSWORD)))
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            if(logger.isTraceEnabled()) logger.trace("request url = " + serverUrl + "request body = " + form + " request headers = " + request.headers().toString());
            HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if(logger.isTraceEnabled()) logger.trace(response.statusCode() + " " + response.body().toString());
            if(response.statusCode() == 200) {
                // construct a token response and return it.
                Map<String, Object> map = JsonMapper.string2Map(response.body().toString());
                if(map != null) {
                    tokenResponse = new TokenResponse();
                    tokenResponse.setAccessToken((String)map.get("access_token"));
                    tokenResponse.setTokenType((String)map.get("token_type"));
                    tokenResponse.setScope((String)map.get("scope"));
                    return Success.of(tokenResponse);
                } else {
                    return Failure.of(new Status(GET_TOKEN_ERROR, "response body is not a JSON"));
                }
            } else {
                logger.error("Error in getting the token with status code " + response.statusCode() + " and body " + response.body().toString());
                return Failure.of(new Status(GET_TOKEN_ERROR, response.body().toString()));
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
            return Failure.of(new Status(ESTABLISH_CONNECTION_ERROR, config.getAccessToken().get(config.TOKEN_URL)));
        }
    }

    private Result<TokenResponse> getMicrosoftToken() throws Exception {
        TokenResponse tokenResponse = null;
        if(clientMicrosoft == null) {
        if(logger.isTraceEnabled()) logger.trace("clientMicrosoft is null. Creating new HTTP2Client with sslContext for MRAS Microsoft.");
            try {
                HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofMillis(ClientConfig.get().getTimeout()))
                        // Token site only need one-way TLS with a public certificate.
                        .sslContext(Http2Client.createSSLContext());
                if(config.getProxyHost() != null) clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(config.getProxyHost(), config.getProxyPort() == 0 ? 443 : config.getProxyPort())));
                if (config.isEnableHttp2()) {
                    clientBuilder.version(HttpClient.Version.HTTP_2);
                } else {
                    clientBuilder.version(HttpClient.Version.HTTP_1_1);
                }
                // this a workaround to bypass the hostname verification in jdk11 http client.
                Map<String, Object> tlsMap = (Map<String, Object>)ClientConfig.get().getMappedConfig().get(Http2Client.TLS);
                if(tlsMap != null && !Boolean.TRUE.equals(tlsMap.get(TLSConfig.VERIFY_HOSTNAME))) {
                    final Properties props = System.getProperties();
                    props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
                }
                clientMicrosoft = clientBuilder.build();
            } catch (IOException e) {
                logger.error("Cannot create HttpClient:", e);
                return Failure.of(new Status(TLS_TRUSTSTORE_ERROR));
            }
        }
        try {
            String serverUrl = (String)config.getMicrosoft().get(config.TOKEN_URL);
            if(serverUrl == null) {
                return Failure.of(new Status(OAUTH_SERVER_URL_ERROR, "microsoft.tokenUrl"));
            }

            Map<String, String> parameters = new HashMap<>();
            parameters.put("grant_type", "client_credentials");
            parameters.put("resource", (String)config.getMicrosoft().get(config.RESOURCE));
            parameters.put("client_id", (String)config.getMicrosoft().get(config.CLIENT_ID));
            parameters.put("client_secret", (String)config.getMicrosoft().get(config.CLIENT_SECRET));

            String form = parameters.entrySet()
                    .stream()
                    .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl))
                    .headers("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<?> response = clientMicrosoft.send(request, HttpResponse.BodyHandlers.ofString());
            if(logger.isTraceEnabled()) logger.trace(response.statusCode() + " " + response.body().toString());
            if(response.statusCode() == 200) {
                // construct a token response and return it.
                Map<String, Object> map = JsonMapper.string2Map(response.body().toString());
                if(map != null) {
                    tokenResponse = new TokenResponse();
                    tokenResponse.setAccessToken((String)map.get("access_token"));
                    tokenResponse.setTokenType((String)map.get("token_type"));
                    tokenResponse.setExpiresIn(Long.valueOf((String)map.get("expires_in"))); // seconds
                    // tokenResponse.setScope((String)map.get("scope")); // microsoft response doesn't have scope in the JSON
                    return Success.of(tokenResponse);
                } else {
                    return Failure.of(new Status(GET_TOKEN_ERROR, "response body is not a JSON"));
                }
            } else {
                logger.error("Error in getting the token with status code " + response.statusCode() + " and body " + response.body().toString());
                return Failure.of(new Status(GET_TOKEN_ERROR, response.body().toString()));
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
            return Failure.of(new Status(ESTABLISH_CONNECTION_ERROR, config.getMicrosoft().get(config.TOKEN_URL)));
        }
    }

    private static String encodeCredentialsFullFormat(String username, String password, String separator) {
        String cred;
        if(password != null) {
            cred = username + separator + password;
        } else {
            cred = username;
        }
        String encodedValue;
        byte[] encodedBytes = Base64.encodeBase64(cred.getBytes(UTF_8));
        encodedValue = new String(encodedBytes, UTF_8);
        return encodedValue;
    }

    private static String encodeCredentials(String username, String password) {
        return encodeCredentialsFullFormat(username, password, ":");
    }

    private SSLContext createSSLContext() throws IOException {
        SSLContext sslContext = null;
        KeyManager[] keyManagers = null;
        try {
            // load key store for client certificate as two-way ssl is used.
            String keyStoreName = config.getKeyStoreName();
            String keyStorePass = config.getKeyStorePass();
            String keyPass = config.getKeyPass();
            if(logger.isTraceEnabled()) logger.trace("keyStoreName = " + keyStoreName + " keyStorePass = " + (keyStorePass == null ? null : keyStorePass.substring(0, 4)) + " keyPass = " + (keyPass == null ? null : keyPass.substring(0, 4)));
            if (keyStoreName != null && keyStorePass != null && keyPass != null) {
                KeyStore keyStore = TlsUtil.loadKeyStore(keyStoreName, keyStorePass.toCharArray());
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, keyPass.toCharArray());
                keyManagers = keyManagerFactory.getKeyManagers();
            }
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
            logger.error("Exception:", e);
            throw new IOException("Unable to initialise KeyManager[]", e);
        }

        TrustManager[] trustManagers = null;
        List<TrustManager> trustManagerList = new ArrayList<>();
        try {
            // temp loading the certificate from the keystore instead of truststore from the config.
            String trustStoreName = config.getKeyStoreName();
            String trustStorePass = config.getKeyStorePass();
            if(logger.isTraceEnabled()) logger.trace("trustStoreName = " + trustStoreName + " trustStorePass = " + (trustStorePass == null ? null : trustStorePass.substring(0, 4)));
            if (trustStoreName != null && trustStorePass != null) {
                KeyStore trustStore = TlsUtil.loadKeyStore(trustStoreName, trustStorePass.toCharArray());
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);
                trustManagers = trustManagerFactory.getTrustManagers();
            }
            if (trustManagers!=null && trustManagers.length>0) {
                trustManagerList.addAll(Arrays.asList(trustManagers));
            }
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            logger.error("Exception:", e);
            throw new IOException("Unable to initialise TrustManager[]", e);
        }

        try {
            sslContext = SSLContext.getInstance("TLSv1.2");
            if(trustManagers == null || trustManagers.length == 0) {
                logger.error("No trust store is loaded. Please check client.yml");
            } else {
                TrustManager[] extendedTrustManagers = {new ClientX509ExtendedTrustManager(trustManagerList)};
                sslContext.init(keyManagers, extendedTrustManagers, null);

            }
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("Exception:", e);
            throw new IOException("Unable to create and initialise the SSLContext", e);
        }

        return sslContext;
    }

}
