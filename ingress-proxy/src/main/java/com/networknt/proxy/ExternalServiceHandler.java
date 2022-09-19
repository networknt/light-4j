package com.networknt.proxy;

import com.networknt.body.BodyHandler;
import com.networknt.client.ClientConfig;
import com.networknt.client.Http2Client;
import com.networknt.client.ssl.TLSConfig;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.config.UrlRewriteRule;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.StringUtils;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;

/**
 * This is a generic handler to route request from a corporate network to external services
 * through the proxy/gateway.
 *
 * @author Steve Hu
 */
public class ExternalServiceHandler implements MiddlewareHandler {
    private static final Logger logger = LoggerFactory.getLogger(ExternalServiceHandler.class);
    private static final String ESTABLISH_CONNECTION_ERROR = "ERR10053";
    private static final String METHOD_NOT_ALLOWED  = "ERR10008";

    private volatile HttpHandler next;
    private ExternalServiceConfig config;
    private HttpClient client;

    public ExternalServiceHandler() {
        config = new ExternalServiceConfig();
        if(logger.isInfoEnabled()) logger.info("ExternalServiceConfig is loaded.");
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
        ModuleRegistry.registerModule(ExternalServiceConfig.class.getName(), Config.getInstance().getJsonMapConfigNoCache(ExternalServiceConfig.CONFIG_NAME), null);
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
        if(logger.isTraceEnabled()) logger.trace("original requestPath = " + requestPath);

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

        if (config.getPathHostMappings() != null) {
            for(String[] parts: config.getPathHostMappings()) {
                if(requestPath.startsWith(parts[0])) {
                    String method = exchange.getRequestMethod().toString();
                    String requestHost = parts[1];
                    String queryString = exchange.getQueryString();
                    if(logger.isTraceEnabled()) logger.trace("request host = " + requestHost + " method = " + method + " queryString = " + queryString);
                    HttpRequest.Builder builder = HttpRequest.newBuilder();
                    HttpRequest request = null;
                    if(queryString != null && !queryString.trim().isEmpty()) {
                        builder.uri(new URI(requestHost + requestPath + "?" + queryString));
                    } else {
                        builder.uri(new URI(requestHost + requestPath));
                    }
                    // copy the headers
                    copyHeaders(exchange.getRequestHeaders(), builder);

                    if(method.equalsIgnoreCase("GET")) {
                        request = builder.GET().build();
                    } else if(method.equalsIgnoreCase("DELETE")) {
                        request = builder.DELETE().build();
                    } else if(method.equalsIgnoreCase("POST")) {
                        // if body handler is in the chain before this handler, we should have it in the exchange attachment.
                        String bodyString = exchange.getAttachment(BodyHandler.REQUEST_BODY_STRING);
                        if(logger.isTraceEnabled() && bodyString != null) logger.trace("Get post body from the exchange attachment = " + bodyString);
                        if(bodyString == null) {
                            InputStream inputStream = exchange.getInputStream();
                            bodyString = StringUtils.inputStreamToString(inputStream, StandardCharsets.UTF_8);
                            if(logger.isTraceEnabled()) logger.trace("Get post body from input stream = " + bodyString);
                        }
                        request = builder.POST(HttpRequest.BodyPublishers.ofString(bodyString)).build();
                    } else if(method.equalsIgnoreCase("PUT")) {
                        String bodyString = exchange.getAttachment(BodyHandler.REQUEST_BODY_STRING);
                        if(logger.isTraceEnabled() && bodyString != null) logger.trace("Get put body from the exchange attachment = " + bodyString);
                        if(bodyString == null) {
                            InputStream inputStream = exchange.getInputStream();
                            bodyString = StringUtils.inputStreamToString(inputStream, StandardCharsets.UTF_8);
                            if(logger.isTraceEnabled()) logger.trace("Get put body from input stream = " + bodyString);
                        }
                        request = builder.PUT(HttpRequest.BodyPublishers.ofString(bodyString)).build();
                    } else if(method.equalsIgnoreCase("PATCH")) {
                        String bodyString = exchange.getAttachment(BodyHandler.REQUEST_BODY_STRING);
                        if(logger.isTraceEnabled() && bodyString != null) logger.trace("Get patch body from the exchange attachment = " + bodyString);
                        if(bodyString == null) {
                            InputStream inputStream = exchange.getInputStream();
                            bodyString = StringUtils.inputStreamToString(inputStream, StandardCharsets.UTF_8);
                            if(logger.isTraceEnabled()) logger.trace("Get patch body from input stream = " + bodyString);
                        }
                        request = builder.method("PATCH", HttpRequest.BodyPublishers.ofString(bodyString)).build();
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
                        } catch (Exception e) {
                            logger.error("Cannot create HttpClient:", e);
                            throw e;
                        }
                    }

                    HttpResponse<String> response  = client.send(request, HttpResponse.BodyHandlers.ofString());
                    HttpHeaders responseHeaders = response.headers();
                    String responseBody = response.body();
                    exchange.setStatusCode(response.statusCode());
                    if(responseHeaders.firstValue(Headers.CONTENT_TYPE.toString()).isPresent()) {
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, responseHeaders.firstValue(Headers.CONTENT_TYPE.toString()).get());
                    }
                    exchange.getResponseSender().send(responseBody);
                    return;
                }
            }
        }
        Handler.next(exchange, next);
    }

    private void copyHeaders(HeaderMap headerMap, HttpRequest.Builder builder) {
        long f = headerMap.fastIterateNonEmpty();
        HeaderValues values;
        while (f != -1L) {
            values = headerMap.fiCurrent(f);
            try {
                builder.setHeader(values.getHeaderName().toString(), values.getFirst());
                if(logger.isTraceEnabled()) logger.trace("Copy header key = " + values.getHeaderName().toString() + " value = " + values.getFirst());
            } catch (Exception e) {
                // for some headers, they cannot be modified.
                if(logger.isTraceEnabled()) logger.trace("Ignore the exception:", e);
            }
            f = headerMap.fiNextNonEmpty(f);
        }
    }
}
