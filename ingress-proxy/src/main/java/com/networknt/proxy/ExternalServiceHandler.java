package com.networknt.proxy;

import com.networknt.client.ClientConfig;
import com.networknt.client.Http2Client;
import com.networknt.client.ssl.TLSConfig;
import com.networknt.config.Config;
import com.networknt.exception.ExceptionHandler;
import com.networknt.handler.*;
import com.networknt.handler.config.UrlRewriteRule;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.metrics.AbstractMetricsHandler;
import com.networknt.server.ModuleRegistry;
import com.networknt.utility.Constants;
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private static volatile Boolean connectionCloseHeaderSupported = null;
    private static final AtomicBoolean connectionCloseHeaderFallbackLogged = new AtomicBoolean(false);
    private volatile HttpHandler next;
    private String configName = ExternalServiceConfig.CONFIG_NAME;
    private HttpClient client;

    /**
     * Construct ExternalServiceHandler with default config name
     */
    public ExternalServiceHandler() {
        ExternalServiceConfig.load(configName);
        if(logger.isInfoEnabled()) logger.info("ExternalServiceHandler is loaded.");
    }

    /**
     * Construct ExternalServiceHandler with config name
     * @param configName config name
     */
    public ExternalServiceHandler(String configName) {
        this.configName = configName;
        ExternalServiceConfig.load(configName);
        if(logger.isInfoEnabled()) logger.info("ExternalServiceHandler is loaded with {}.", configName);
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
        return ExternalServiceConfig.load(configName).isEnabled();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("ExternalServiceHandler.handleRequest starts.");
        ExternalServiceConfig config = ExternalServiceConfig.load(configName);
        int connectTimeout = config.getConnectTimeout();
        if(connectTimeout == 0) connectTimeout = ClientConfig.get().getRequest().getConnectTimeout(); // fallback to client.yml if not set in mras.yml
        int timeout = config.getTimeout();
        if(timeout == 0) timeout = ClientConfig.get().getRequest().getTimeout(); // fallback to client.yml if not set in mras.yml

        long startTime = System.nanoTime();
        String requestPath = exchange.getRequestPath();
        if(logger.isTraceEnabled()) logger.trace("original requestPath = {}", requestPath);
        String matchedPathPrefix = null;
        String requestHost = null;
        int currentTimeout = timeout;

        if (config.getPathPrefixes() != null && !config.getPathPrefixes().isEmpty()) {
            for (PathPrefix pp : config.getPathPrefixes()) {
                if (pp.getPathPrefix() != null && pp.getHost() != null && requestPath.startsWith(pp.getPathPrefix())) {
                    matchedPathPrefix = pp.getPathPrefix();
                    requestHost = pp.getHost();
                    if (pp.getTimeout() > 0) {
                        currentTimeout = pp.getTimeout();
                    }
                    if(logger.isTraceEnabled()) logger.trace("found matched pathPrefix = {} host = {} timeout = {}", matchedPathPrefix, requestHost, currentTimeout);
                    break;
                }
            }
        } else if (config.getPathHostMappings() != null) {
            for(String[] parts: config.getPathHostMappings()) {
                if(requestPath.startsWith(parts[0])) {
                    matchedPathPrefix = parts[0];
                    requestHost = parts[1];
                    if(logger.isTraceEnabled()) logger.trace("found matched pathHostMapping = {} host = {}", matchedPathPrefix, requestHost);
                    break;
                }
            }
        }

        if (matchedPathPrefix != null) {
            ForwardingContext context = new ForwardingContext(config, matchedPathPrefix,
                    requestHost, currentTimeout, connectTimeout, startTime);
            // Capture the match once: asynchronous body reads can resume on an I/O thread.
            if (exchange.isInIoThread()) {
                exchange.dispatch(workerExchange -> new ExceptionHandler()
                        .setNext(forwardExchange -> forwardMatchedRequest(forwardExchange, context))
                        .handleRequest(workerExchange));
            } else {
                forwardMatchedRequest(exchange, context);
            }
            return;
        }
        if(logger.isDebugEnabled()) logger.debug("ExternalServiceHandler.handleRequest ends.");
        Handler.next(exchange, next);
    }

    private record ForwardingContext(ExternalServiceConfig config, String pathPrefix,
                                     String host, int timeout, int connectTimeout, long startTime) { }

    private void forwardMatchedRequest(HttpServerExchange exchange, ForwardingContext context)
            throws IOException, InterruptedException, URISyntaxException {
        String endpoint = context.pathPrefix() + "@" + exchange.getRequestMethod().toString().toLowerCase();
        String requestPath = rewriteRequestPath(context.config(), exchange.getRequestPath());
        AuditAttachmentUtil.populateAuditAttachmentField(exchange, Constants.ENDPOINT_STRING, endpoint);
        HttpRequest request = buildForwardRequest(exchange, context, requestPath);
        if (request == null) {
            logger.error("wrong http method {} for request path {}", exchange.getRequestMethod(), requestPath);
            setExchangeStatus(exchange, METHOD_NOT_ALLOWED, exchange.getRequestMethod().toString(), requestPath);
            injectMetrics(exchange, context, endpoint);
            exchange.endExchange();
            return;
        }

        HttpClient forwardingClient = getOrCreateClient(context.config(), context.connectTimeout());
        HttpResponse<byte[]> response = sendWithRetries(forwardingClient, request,
                context.config().getMaxConnectionRetries());
        if (response == null) {
            setExchangeStatus(exchange, ESTABLISH_CONNECTION_ERROR);
            exchange.endExchange();
            return;
        }
        writeResponse(exchange, response);
        injectMetrics(exchange, context, endpoint);
    }

    private String rewriteRequestPath(ExternalServiceConfig config, String requestPath) {
        if (config.getUrlRewriteRules() != null) {
            for (UrlRewriteRule rule : config.getUrlRewriteRules()) {
                Matcher matcher = rule.getPattern().matcher(requestPath);
                if (matcher.matches()) return matcher.replaceAll(rule.getReplace());
            }
        }
        return requestPath;
    }

    private HttpRequest buildForwardRequest(HttpServerExchange exchange, ForwardingContext context,
                                           String requestPath) throws IOException, URISyntaxException {
        HttpRequest.Builder builder = HttpRequest.newBuilder().timeout(Duration.ofMillis(context.timeout()));
        handleHttpClientUrl(exchange, builder, requestPath, context.host());
        copyHeaders(exchange.getRequestHeaders(), builder);
        String method = exchange.getRequestMethod().toString().toUpperCase(Locale.ROOT);
        return switch (method) {
            case "GET" -> builder.GET().build();
            case "DELETE" -> builder.DELETE().build();
            case "POST", "PUT", "PATCH" -> handleBufferedRequestBody(exchange, builder, method);
            default -> null;
        };
    }

    private synchronized HttpClient getOrCreateClient(ExternalServiceConfig config, int connectTimeout)
            throws IOException {
        if (client == null) client = createJavaHttpClient(config, connectTimeout);
        return client;
    }

    private HttpResponse<byte[]> sendWithRetries(HttpClient forwardingClient, HttpRequest request, int maxRetries)
            throws IOException, InterruptedException {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            HttpRequest finalRequest = attempt == 0 ? request : buildRetryRequest(request, attempt);
            try {
                return forwardingClient.send(finalRequest, HttpResponse.BodyHandlers.ofByteArray());
            } catch (IOException | InterruptedException e) {
                if (attempt >= maxRetries - 1) throw e;
                logger.warn("Attempt {} failed ({}). Retrying...", attempt + 1, e.getMessage());
            }
        }
        logger.error("Failed after retrying {} times.", maxRetries);
        return null;
    }

    private void writeResponse(HttpServerExchange exchange, HttpResponse<byte[]> response) {
        byte[] responseBody = response.body();
        if (response.statusCode() >= 400 && logger.isDebugEnabled()
                && responseBody != null && responseBody.length > 0) {
            logger.debug("External Service Response Error: status = '{}', body = '{}'",
                    response.statusCode(), new String(responseBody));
        }
        exchange.setStatusCode(response.statusCode());
        copyResponseHeaders(exchange, response.headers().map());
        exchange.getResponseSender().send(ByteBuffer.wrap(responseBody));
    }

    private void copyResponseHeaders(HttpServerExchange exchange, Map<String, List<String>> headers) {
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            String name = header.getKey();
            if (name == null || name.startsWith(":") || header.getValue().get(0) == null) continue;
            for (String value : header.getValue()) {
                exchange.getResponseHeaders().add(new HttpString(name), value);
            }
        }
    }

    private void injectMetrics(HttpServerExchange exchange, ForwardingContext context, String endpoint) {
        if (!context.config().isMetricsInjection()) return;
        AbstractMetricsHandler metricsHandler = AbstractMetricsHandler.lookupMetricsHandler();
        if (metricsHandler != null) {
            metricsHandler.injectMetrics(exchange, context.startTime(), context.config().getMetricsName(), endpoint);
        }
    }

    private void copyHeaders(HeaderMap headerMap, HttpRequest.Builder builder) {
        long f = headerMap.fastIterateNonEmpty();
        HeaderValues values;
        while (f != -1L) {
            values = headerMap.fiCurrent(f);
            try {
                String key = values.getHeaderName().toString();
                // skip restricted headers
                if (!key.equalsIgnoreCase("Host")
                        && !key.equalsIgnoreCase("Connection")
                        && !key.equalsIgnoreCase("Content-Length")
                        && !key.equalsIgnoreCase("Transfer-Encoding")
                        && !key.equalsIgnoreCase("Upgrade")) {
                    builder.setHeader(key, values.getFirst());
                    if(logger.isTraceEnabled()) logger.trace("Copy header key = {} value = {}", key, values.getFirst());
                }
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
     * @return - returns HttpClient instance if the client was created successfully.
     */

    @SuppressWarnings("unchecked")
    private HttpClient createJavaHttpClient(ExternalServiceConfig config, int connectTimeout) throws IOException {
        // this a workaround to bypass the hostname verification in jdk11 and jdk21 http client.
        var tlsMap = (Map<String, Object>) ClientConfig.get().getMappedConfig().get(Http2Client.TLS);
        final var props = System.getProperties();

        var clientBuilder = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofMillis(connectTimeout));



        if (!config.isVerifyHostname()) {
            // setting jdk.internal.httpclient.disableHostnameVerification=true set it globally for the JVM,
            // however it is overwritten by the sslContext defined in the HttpClient builder.
            // this will not conflict with Http2Client due to different clients and different system properties
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

        return clientBuilder.build();
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
        return switch (method) {
            case "PUT" -> builder.PUT(bodyPublisher).build();
            case "POST" -> builder.POST(bodyPublisher).build();
            default -> builder.method(method, bodyPublisher).build();
        };
    }

    static HttpRequest buildRetryRequest(HttpRequest request, int attempt) {
        if (shouldSkipConnectionCloseRetryHeader()) {
            return request;
        }
        try {
            HttpRequest retryRequest = HttpRequest.newBuilder(request, (name, value) -> true)
                    .header("Connection", "close")
                    .build();
            logger.info("Retrying attempt {} with 'Connection: close' to force fresh connection.", attempt + 1);
            connectionCloseHeaderSupported = Boolean.TRUE;
            return retryRequest;
        } catch (IllegalArgumentException e) {
            connectionCloseHeaderSupported = Boolean.FALSE;
            if (connectionCloseHeaderFallbackLogged.compareAndSet(false, true)) {
                logger.info("Retry attempt {} could not set restricted header 'Connection: close'. Falling back to a normal retry. Configure -Djdk.httpclient.allowRestrictedHeaders=connection at JVM startup to enable fresh-connection retries.", attempt + 1);
                logger.debug("Unable to set restricted header 'Connection: close'.", e);
            }
            return request;
        }
    }

    private static boolean shouldSkipConnectionCloseRetryHeader() {
        return Boolean.FALSE.equals(connectionCloseHeaderSupported);
    }
}
