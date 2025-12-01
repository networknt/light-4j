package com.networknt.proxy;

import com.networknt.client.ClientConfig;
import com.networknt.client.Http2Client;
import com.networknt.client.ssl.TLSConfig;
import com.networknt.config.Config;
import com.networknt.handler.*;
import com.networknt.handler.config.UrlRewriteRule;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.metrics.AbstractMetricsHandler;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
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
    private static AbstractMetricsHandler metricsHandler;
    private volatile HttpHandler next;
    private static ExternalServiceConfig config;
    private HttpClient client;

    public ExternalServiceHandler() {
        config = ExternalServiceConfig.load();
        if(config.isMetricsInjection()) metricsHandler = AbstractMetricsHandler.lookupMetricsHandler();
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
        ModuleRegistry.registerModule(ExternalServiceConfig.CONFIG_NAME, ExternalServiceHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(ExternalServiceConfig.CONFIG_NAME), null);
    }

    @Override
    public void reload() {
        config.reload();
        if(config.isMetricsInjection()) metricsHandler = AbstractMetricsHandler.lookupMetricsHandler();
        ModuleRegistry.registerModule(ExternalServiceConfig.CONFIG_NAME, ExternalServiceHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(ExternalServiceConfig.CONFIG_NAME), null);
        if(logger.isInfoEnabled()) logger.info("ExternalServiceHandler is reloaded.");
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("ExternalServiceHandler.handleRequest starts.");
        long startTime = System.nanoTime();
        String requestPath = exchange.getRequestPath();
        if(logger.isTraceEnabled()) logger.trace("original requestPath = " + requestPath);
        if (config.getPathHostMappings() != null) {
            for(String[] parts: config.getPathHostMappings()) {
                if(requestPath.startsWith(parts[0])) {
                    String endpoint = parts[0] + "@" + exchange.getRequestMethod().toString().toLowerCase();
                    if(logger.isTraceEnabled()) logger.trace("endpoint = {}", endpoint);
                    // handle the url rewrite here. It has to be the right path that applied for external service to do the url rewrite.
                    if(config.getUrlRewriteRules() != null && config.getUrlRewriteRules().size() > 0) {
                        boolean matched = false;
                        for(UrlRewriteRule rule : config.getUrlRewriteRules()) {
                            Matcher matcher = rule.getPattern().matcher(requestPath);
                            if(matcher.matches()) {
                                matched = true;
                                requestPath = matcher.replaceAll(rule.getReplace());
                                if(logger.isTraceEnabled()) logger.trace("rewritten requestPath = {}", requestPath);
                                break;
                            }
                        }
                        // if no matched rule in the list, use the original requestPath.
                        if(!matched) requestPath = exchange.getRequestPath();
                    } else {
                        // there is no url rewrite rules, so use the original requestPath
                        requestPath = exchange.getRequestPath();
                    }

                    AuditAttachmentUtil.populateAuditAttachmentField(exchange, Constants.ENDPOINT_STRING, endpoint);
                    String method = exchange.getRequestMethod().toString();
                    String requestHost = parts[1];
                    String queryString = exchange.getQueryString();

                    logger.trace("External Service Request Info: host = '{}', method = '{}', requestPath = '{}', queryString = '{}'", requestHost, method, requestPath, queryString);

                    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
                    HttpRequest request = null;

                    this.handleHttpClientUrl(exchange, requestBuilder, requestPath, requestHost);

                    // copy the headers
                    copyHeaders(exchange.getRequestHeaders(), requestBuilder);

                    /* handle GET request. */
                    if(method.equalsIgnoreCase("GET")) {
                        request = requestBuilder.GET().build();

                        /* handle DELETE request. */
                    } else if(method.equalsIgnoreCase("DELETE")) {
                        request = requestBuilder.DELETE().build();

                        /* handle POST request that potentially has body data */
                    } else if(method.equalsIgnoreCase("POST")) {
                        request = this.handleBufferedRequestBody(exchange, requestBuilder, "POST");

                        /* handle PUT request that potentially has body data */
                    } else if(method.equalsIgnoreCase("PUT")) {
                        request = this.handleBufferedRequestBody(exchange, requestBuilder, "PUT");

                        /* handle PATCH request that potentially has body data */
                    } else if(method.equalsIgnoreCase("PATCH")) {
                        request = this.handleBufferedRequestBody(exchange, requestBuilder, "PATCH");

                    } else {
                        logger.error("wrong http method {} for request path {}", method, requestPath);
                        setExchangeStatus(exchange, METHOD_NOT_ALLOWED, method, requestPath);
                        logger.debug("ExternalServiceHandler.handleRequest ends with an error.");
                        if(config.isMetricsInjection()) {
                            if(metricsHandler == null) metricsHandler = AbstractMetricsHandler.lookupMetricsHandler();
                            if(metricsHandler != null) {
                                if (logger.isTraceEnabled()) logger.trace("Inject metrics for {}", config.getMetricsName());
                                metricsHandler.injectMetrics(exchange, startTime, config.getMetricsName(), endpoint);
                            }
                        }
                        exchange.endExchange();
                        return;
                    }

                    if (client == null && !this.createJavaHttpClient()) {
                        setExchangeStatus(exchange, ESTABLISH_CONNECTION_ERROR);
                        exchange.endExchange();
                        return;
                    }

                    int maxRetries = config.getMaxConnectionRetries();
                    int attempt = 0;
                    HttpResponse<byte[]> response = null;
                    while (attempt < maxRetries) {
                        try {
                            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                            break; // Exit loop if request is successful
                        } catch (IOException | InterruptedException e) {
                            attempt++;
                            if (attempt >= maxRetries) {
                                throw e; // Rethrow the exception if max retries reached
                            }
                            logger.warn("Attempt {} failed, retrying...", attempt, e);
                        }
                    }
                    var responseHeaders = response.headers();
                    byte[] responseBody = response.body();
                    if(response.statusCode() >= 400) {
                        // want to log the response body for 4xx and 5xx errors.
                        if(logger.isDebugEnabled() && responseBody != null && responseBody.length > 0)
                            logger.debug("External Service Response Error: status = '{}', body = '{}'", response.statusCode(), new String(responseBody));
                    }
                    exchange.setStatusCode(response.statusCode());
                    for (Map.Entry<String, List<String>> header : responseHeaders.map().entrySet()) {
                        // remove empty key in the response header start with a colon.
                        if (header.getKey() != null && !header.getKey().startsWith(":") && header.getValue().get(0) != null) {
                            for(String s : header.getValue()) {
                                if(logger.isTraceEnabled()) logger.trace("Add response header key = {} value = {}", header.getKey(), s);
                                exchange.getResponseHeaders().add(new HttpString(header.getKey()), s);
                            }
                        }
                    }

                    /* send response and close exchange */
                    exchange.getResponseSender().send(ByteBuffer.wrap(responseBody));
                    if(logger.isDebugEnabled()) logger.debug("ExternalServiceHandler.handleRequest ends.");
                    if(config.isMetricsInjection()) {
                        if(metricsHandler == null) metricsHandler = AbstractMetricsHandler.lookupMetricsHandler();
                        if(metricsHandler != null) {
                            if (logger.isTraceEnabled()) logger.trace("Inject metrics for {}", config.getMetricsName());
                            metricsHandler.injectMetrics(exchange, startTime, config.getMetricsName(), endpoint);
                        }
                    }
                    return;
                }
            }
        }
        if(logger.isDebugEnabled()) logger.debug("ExternalServiceHandler.handleRequest ends.");
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

    /**
     * Builds our Java HttpClient based on parameters from the Undertow exchange.
     *
     *
     * @return - returns true if the client was created successfully.
     */

    @SuppressWarnings("unchecked")
    private boolean createJavaHttpClient() {

        try {
            // this a workaround to bypass the hostname verification in jdk11 and jdk21 http client.
            var tlsMap = (Map<String, Object>) ClientConfig.get().getMappedConfig().get(Http2Client.TLS);
            final var props = System.getProperties();
            props.setProperty("jdk.httpclient.allowRestrictedHeaders", "Host");
            props.setProperty("jdk.httpclient.allowRestrictedHeaders", "Connection"); // this essentially overwrites the above JVM arg value "Host"

            var clientBuilder = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofMillis(ClientConfig.get().getTimeout()));

            if (tlsMap != null && !Boolean.TRUE.equals(tlsMap.get(TLSConfig.VERIFY_HOSTNAME))) {
                // setting jdk.internal.httpclient.disableHostnameVerification=true set it globally for the JVM,
                // however it is overwritten by the sslContext defined in the HttpClient builder.
                props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
            }
            else {
                clientBuilder
                        .sslContext(Http2Client.createSSLContext());
            }

            if (config.getProxyHost() != null && !config.getProxyHost().isEmpty()) {
                clientBuilder.proxy(ProxySelector.of(
                        InetSocketAddress.createUnresolved(
                                config.getProxyHost(),
                                config.getProxyPort() == 0 ? 443 : config.getProxyPort()
                        )
                ));
            }

            if (config.isEnableHttp2())
                clientBuilder.version(HttpClient.Version.HTTP_2);

            else clientBuilder.version(HttpClient.Version.HTTP_1_1);

            this.client = clientBuilder.build();

        } catch (IOException e) {
            logger.error("Cannot create HttpClient:", e);
            return false;
        }

        return true;
    }

    /**
     * Builds the request URL for an HttpRequest.
     *
     * @param exchange - current Undertow exchange.
     * @param builder - HttpRequest builder.
     * @param requestPath - Resolved request path.
     * @param requestHost - Resolved request host.
     *
     * @throws URISyntaxException - throws exception when URL is invalid (malformed request path or host).
     */
    private void handleHttpClientUrl(HttpServerExchange exchange, HttpRequest.Builder builder, String requestPath, String requestHost) throws URISyntaxException {
        String queryString = exchange.getQueryString();
        if(queryString != null && !queryString.trim().isEmpty()) {
            builder.uri(new URI(requestHost + requestPath + "?" + queryString));
        } else {
            builder.uri(new URI(requestHost + requestPath));
        }
    }

    /**
     * Copies the buffered request body from Undertow exchange to the Java HttpClient.
     *
     * @param exchange - current undertow exchange
     * @param builder - java http request builder
     * @param method - method of the request (post, put, patch, etc.)
     *
     * @return - returns complete http request ready to be sent.
     *
     * @throws IOException - throws exception if there is a failure during copy.
     */
    private HttpRequest handleBufferedRequestBody(final HttpServerExchange exchange, HttpRequest.Builder builder, String method) throws IOException {
        var bodyString = exchange.getAttachment(AttachmentConstants.REQUEST_BODY_STRING);

        if (bodyString == null) {
            logger.trace("The request bodyString is null and the request path might be missing in request-injection.appliedBodyInjectionPathPrefixes.");
            PooledByteBuffer[] buffer = exchange.getAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY);

            if (buffer != null) {

                /* Get underlying byte array from buffer. */
                byte[] bodyBytes = BuffersUtils.getByteArray(buffer);
                logger.trace("Grabbed byte[] with length = '{}' bytes from PooledByteBuffer", bodyBytes.length);
                return buildRequestForMethod(builder, HttpRequest.BodyPublishers.ofByteArray(bodyBytes), method);

            } else {

                // no body in the exchange, send an empty body.
                return buildRequestForMethod(builder, HttpRequest.BodyPublishers.noBody(), method);
            }

        } else {
            // bodyString is not null, send it directly.
            logger.trace("request body = {}", bodyString);
            return buildRequestForMethod(builder, HttpRequest.BodyPublishers.ofString(bodyString), method);
        }
    }

    /**
     * Handles the final step in building our HttpRequest.
     * Final build statement depends on what method is being used.
     *
     * @param builder - HttpRequest builder
     * @param bodyPublisher - HttpRequest.BodyPublisher
     * @param method - Undertow exchange method.
     *
     * @return - returns complete HttpRequest.
     */
    private HttpRequest buildRequestForMethod(HttpRequest.Builder builder, HttpRequest.BodyPublisher bodyPublisher, String method) {
        switch (method) {
            case "PUT":
                return builder.PUT(bodyPublisher).build();
            case "POST":
                return builder.POST(bodyPublisher).build();
            case "PATCH":
            default:
                return builder.method(method, bodyPublisher).build();
        }
    }
}
