package com.networknt.proxy.salesforce;

import com.networknt.body.BodyHandler;
import com.networknt.client.ClientConfig;
import com.networknt.client.Http2Client;
import com.networknt.client.oauth.TokenResponse;
import com.networknt.client.ssl.TLSConfig;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.config.TlsUtil;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
import com.networknt.status.Status;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.StringUtils;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is a customized Salesforce handler for authentication and authorization with cloud Salesforce service
 * within an enterprise environment. It is converted from a customized flow for the business logic.
 *
 * For any external Salesforce request, this handler will check if there is a cached salesforce token that is
 * not expired. If true, put the token into the header. If false, get a new token by following the flow and put
 * it into the header and cache it.
 *
 * The way to get a salesforce token is to sign a token with your private key and salesforce will have the public
 * key to verify your token. Send a request with grant_type and assertion as body to the url defined in the config
 * file. Salesforce will issue an access token for API access.
 *
 * For the token caching, we only cache the salesforce token. The jwt token we created only last 5 minutes, and it
 * is not cached.
 *
 * @author Steve Hu
 */
public class SalesforceHandler implements MiddlewareHandler {
    private static final Logger logger = LoggerFactory.getLogger(SalesforceHandler.class);
    private static final String TLS_TRUSTSTORE_ERROR = "ERR10055";
    private static final String OAUTH_SERVER_URL_ERROR = "ERR10056";
    private static final String ESTABLISH_CONNECTION_ERROR = "ERR10053";
    private static final String GET_TOKEN_ERROR = "ERR10052";
    private static final String METHOD_NOT_ALLOWED  = "ERR10008";
    private static final String REQUEST_BODY_MISSING = "ERR10059";

    private volatile HttpHandler next;
    private SalesforceConfig config;
    // the cached jwt token so that we can use the same token for different requests.

    private HttpClient client;

    public SalesforceHandler() {
        config = SalesforceConfig.load();
        if(logger.isInfoEnabled()) logger.info("SalesforceAuthHandler is loaded.");
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
        masks.add("certPassword");
        ModuleRegistry.registerModule(SalesforceHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(SalesforceConfig.CONFIG_NAME), masks);
    }

    @Override
    public void reload() {
        config.reload();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
        exchange.startBlocking();
        String requestPath = exchange.getRequestPath();
        if(logger.isTraceEnabled()) logger.trace("requestPath = " + requestPath);
        // make sure that the request path is in the key set. remember that key set only contains prefix not the full request path.
        for(PathPrefixAuth pathPrefixAuth: config.getPathPrefixAuths()) {
            if(requestPath.startsWith(pathPrefixAuth.getPathPrefix())) {
                if(logger.isTraceEnabled()) logger.trace("found with requestPath = " + requestPath + " prefix = " + pathPrefixAuth.getPathPrefix());
                // matched the prefix found. handler it with the config for this prefix.
                if(System.currentTimeMillis() >= (pathPrefixAuth.getExpiration() - 5000)) { // leave 5 seconds room and default value is 0
                    String jwt = createJwt(pathPrefixAuth.getAuthIssuer(), pathPrefixAuth.getAuthSubject(), pathPrefixAuth.getAuthAudience());
                    Result<TokenResponse> result = getAccessToken(pathPrefixAuth.getTokenUrl(), jwt);
                    if(result.isSuccess()) {
                        pathPrefixAuth.setExpiration(System.currentTimeMillis() + 300 * 1000);
                        pathPrefixAuth.setAccessToken(result.getResult().getAccessToken());
                    } else {
                        setExchangeStatus(exchange, result.getError());
                        return;
                    }
                }
                invokeApi(exchange, "Bearer " + pathPrefixAuth.getAccessToken(), pathPrefixAuth.getServiceHost());
                return;
            }
        }
        // not the Salesforce path, go to the next middleware handler
        if(logger.isDebugEnabled()) logger.debug("The requestPath is not matched to salesforce, go to the next handler in the chain.");
        Handler.next(exchange, next);
    }

    private String createJwt(String issuer, String subject, String audience) throws Exception {
        String certFileName = config.getCertFilename();
        String certPassword = config.getCertPassword();

        String header = "{\"alg\":\"RS256\"}";
        String claimTemplate = "'{'\"iss\": \"{0}\", \"sub\": \"{1}\", \"aud\": \"{2}\", \"exp\": \"{3}\"'}'";
        StringBuffer token = new StringBuffer();
        // Encode the JWT Header and add it to our string to sign
        token.append(org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(header.getBytes("UTF-8")));
        // Separate with a period
        token.append(".");

        String[] claimArray = new String[4];
        claimArray[0] = issuer;
        claimArray[1] = subject;
        claimArray[2] = audience;
        claimArray[3] =  Long.toString( ( System.currentTimeMillis()/1000 ) + 300);

        MessageFormat claims;
        claims = new MessageFormat(claimTemplate);
        String payload = claims.format(claimArray);

        // Add the encoded claims object
        token.append(org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(payload.getBytes("UTF-8")));

        KeyStore keystore = TlsUtil.loadKeyStore(certFileName, certPassword.toCharArray());
        PrivateKey privateKey = (PrivateKey) keystore.getKey(certFileName.substring(0, certFileName.indexOf(".")), certPassword.toCharArray());

        // Sign the JWT Header + "." + JWT Claims Object
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(token.toString().getBytes("UTF-8"));
        String signedPayload = org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(signature.sign());

        // Separate with a period
        token.append(".");

        // Add the encoded signature
        token.append(signedPayload);
        return token.toString();
    }

    private Result<TokenResponse> getAccessToken(String serverUrl, String jwt) throws Exception {
        TokenResponse tokenResponse = null;
        if(client == null) {
            try {
                HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofMillis(ClientConfig.get().getTimeout()))
                        .sslContext(Http2Client.createSSLContext());
                if(config.getProxyHost() != null) clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(config.getProxyHost(), config.getProxyPort() == 0 ? 443 : config.getProxyPort())));
                if(config.isEnableHttp2()) clientBuilder.version(HttpClient.Version.HTTP_2);
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
            if(serverUrl == null) {
                return Failure.of(new Status(OAUTH_SERVER_URL_ERROR, "tokenUrl"));
            }

            Map<String, String> parameters = new HashMap<>();
            parameters.put("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
            parameters.put("assertion", jwt);

            String form = parameters.entrySet()
                    .stream()
                    .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl))
                    .headers("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.statusCode() + " " + response.body().toString());
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
            return Failure.of(new Status(ESTABLISH_CONNECTION_ERROR, serverUrl));
        }
    }

    private void invokeApi(HttpServerExchange exchange, String authorization, String requestHost) throws Exception {
        // call the Salesforce API directly here with the token from the cache.
        String requestPath = exchange.getRequestPath();
        String method = exchange.getRequestMethod().toString();
        String queryString = exchange.getQueryString();
        String contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
        HttpRequest request = null;
        if(method.equalsIgnoreCase("GET")) {
            request = HttpRequest.newBuilder()
                    .uri(new URI(requestHost + requestPath + "?" + queryString))
                    .headers("Authorization", authorization, "Content-Type", contentType)
                    .GET()
                    .build();

        } else if(method.equalsIgnoreCase("DELETE")) {
            request = HttpRequest.newBuilder()
                    .uri(new URI(requestHost + requestPath + "?" + queryString))
                    .headers("Authorization", authorization, "Content-Type", contentType)
                    .DELETE()
                    .build();

        } else if(method.equalsIgnoreCase("POST")) {
            String bodyString = exchange.getAttachment(BodyHandler.REQUEST_BODY_STRING);
            if(bodyString == null) {
                InputStream inputStream = exchange.getInputStream();
                bodyString = StringUtils.inputStreamToString(inputStream, StandardCharsets.UTF_8);
            }
            if(logger.isTraceEnabled()) logger.trace("Post request body = " + bodyString);
            if(bodyString == null) {
                setExchangeStatus(exchange, REQUEST_BODY_MISSING);
                return;
            }
            request = HttpRequest.newBuilder()
                    .uri(new URI(requestHost + requestPath))
                    .headers("Authorization", authorization, "Content-Type", contentType)
                    .POST(HttpRequest.BodyPublishers.ofString(bodyString))
                    .build();
        } else if(method.equalsIgnoreCase("PUT")) {
            String bodyString = exchange.getAttachment(BodyHandler.REQUEST_BODY_STRING);
            if(bodyString == null) {
                InputStream inputStream = exchange.getInputStream();
                bodyString = StringUtils.inputStreamToString(inputStream, StandardCharsets.UTF_8);
            }
            if(logger.isTraceEnabled()) logger.trace("Put request body = " + bodyString);
            if(bodyString == null) {
                setExchangeStatus(exchange, REQUEST_BODY_MISSING);
                return;
            }
            request = HttpRequest.newBuilder()
                    .uri(new URI(requestHost + requestPath))
                    .headers("Authorization", authorization, "Content-Type", contentType)
                    .PUT(HttpRequest.BodyPublishers.ofString(bodyString))
                    .build();
        } else if(method.equalsIgnoreCase("PATCH")) {
            String bodyString = exchange.getAttachment(BodyHandler.REQUEST_BODY_STRING);
            if(bodyString == null) {
                InputStream inputStream = exchange.getInputStream();
                bodyString = StringUtils.inputStreamToString(inputStream, StandardCharsets.UTF_8);
            }
            if(logger.isTraceEnabled()) logger.trace("Patch request body = " + bodyString);
            if(bodyString == null) {
                setExchangeStatus(exchange, REQUEST_BODY_MISSING);
                return;
            }
            request = HttpRequest.newBuilder()
                    .uri(new URI(requestHost + requestPath))
                    .headers("Authorization", authorization, "Content-Type", contentType)
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(bodyString))
                    .build();
        } else {
            logger.error("wrong http method " + method + " for request path " + requestPath);
            setExchangeStatus(exchange, METHOD_NOT_ALLOWED, method, requestPath);
            return;
        }
        HttpResponse<String> response  = client.send(request, HttpResponse.BodyHandlers.ofString());
        HttpHeaders responseHeaders = response.headers();
        String responseBody = response.body();
        exchange.setStatusCode(response.statusCode());
        if(responseHeaders.firstValue(Headers.CONTENT_TYPE.toString()).isPresent()) {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, responseHeaders.firstValue(Headers.CONTENT_TYPE.toString()).get());
        }
        if(logger.isTraceEnabled()) logger.trace("response body = " + responseBody);
        exchange.getResponseSender().send(responseBody);
    }

}
