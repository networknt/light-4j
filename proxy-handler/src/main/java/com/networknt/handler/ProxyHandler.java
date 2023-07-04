/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.networknt.handler;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.security.cert.CertificateEncodingException;

import com.networknt.handler.config.MethodRewriteRule;
import com.networknt.handler.config.QueryHeaderRewriteRule;
import com.networknt.handler.config.UrlRewriteRule;
import com.networknt.handler.thread.LightThreadExecutor;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.metrics.AbstractMetricsHandler;
import com.networknt.metrics.MetricsHandler;
import com.networknt.utility.CollectionUtils;
import com.networknt.utility.StringUtils;
import io.undertow.UndertowLogger;
import io.undertow.UndertowMessages;
import io.undertow.server.*;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.ProxyCallback;
import io.undertow.server.handlers.proxy.ProxyClient;
import io.undertow.server.handlers.proxy.ProxyConnection;
import io.undertow.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelExceptionHandler;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.IoUtils;
import org.xnio.StreamConnection;
import org.xnio.XnioExecutor;
import org.xnio.channels.StreamSinkChannel;
import io.undertow.attribute.ExchangeAttribute;
import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.client.ProxiedRequestAttachments;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.predicate.IdempotentPredicate;
import io.undertow.predicate.Predicate;
import io.undertow.server.protocol.http.HttpAttachments;
import io.undertow.server.protocol.http.HttpContinue;

import java.util.stream.Collectors;

/**
 * An HTTP handler which proxies content to a remote server.
 * <p>
 * This handler acts like a filter. The {@link ProxyClient} has a chance to decide if it
 * knows how to proxy the request. If it does then it will provide a connection that can
 * used to connect to the remote server, otherwise the next handler will be invoked and the
 * request will proceed as normal.
 * <p>
 * This handler uses non blocking IO
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ProxyHandler implements HttpHandler {

    private static final int DEFAULT_MAX_RETRY_ATTEMPTS = Integer.getInteger("maxRetries", 1);
    private static final int DEFAULT_MAX_QUEUE_SIZE = Integer.getInteger("maxQueueSize", 0);

    private static final Logger LOG = LoggerFactory.getLogger(ProxyHandler.class);

    public static final String UTF_8 = StandardCharsets.UTF_8.name();

    private LightThreadExecutor lightThreadExecutor;

    public static final AttachmentKey<ProxyConnection> CONNECTION = AttachmentKey.create(ProxyConnection.class);
    private static final AttachmentKey<HttpServerExchange> EXCHANGE = AttachmentKey.create(HttpServerExchange.class);
    private static final AttachmentKey<XnioExecutor.Key> TIMEOUT_KEY = AttachmentKey.create(XnioExecutor.Key.class);

    private final ProxyClient proxyClient;
    private int maxRequestTime;
    private final Map<String, Integer> pathPrefixMaxRequestTime;
    /**
     * Map of additional headers to add to the request.
     */
    private final Map<HttpString, ExchangeAttribute> requestHeaders = new CopyOnWriteMap<>();

    private final HttpHandler next;

    private volatile boolean rewriteHostHeader;
    private volatile boolean reuseXForwarded;
    private volatile int maxConnectionRetries;
    private volatile int maxQueueSize;
    private volatile List<UrlRewriteRule> urlRewriteRules;
    private volatile List<MethodRewriteRule> methodRewriteRules;

    private volatile Map<String, List<QueryHeaderRewriteRule>> queryParamRewriteRules;

    private volatile Map<String, List<QueryHeaderRewriteRule>> headerRewriteRules;

    private final Predicate idempotentRequestPredicate;

    private ProxyHandler(Builder builder) {
        this.proxyClient = builder.proxyClient;
        this.maxRequestTime = builder.maxRequestTime;
        this.pathPrefixMaxRequestTime = builder.pathPrefixMaxRequestTime;
        this.next = builder.next;
        this.rewriteHostHeader = builder.rewriteHostHeader;
        this.reuseXForwarded = builder.reuseXForwarded;
        this.maxConnectionRetries = builder.maxConnectionRetries;
        this.maxQueueSize = builder.maxQueueSize;
        this.urlRewriteRules = builder.urlRewriteRules;
        this.methodRewriteRules = builder.methodRewriteRules;
        this.queryParamRewriteRules = builder.queryParamRewriteRules;
        this.headerRewriteRules = builder.headerRewriteRules;
        this.idempotentRequestPredicate = builder.idempotentRequestPredicate;
        for (Map.Entry<HttpString, ExchangeAttribute> e : builder.requestHeaders.entrySet()) {
            requestHeaders.put(e.getKey(), e.getValue());
        }
    }

    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        this.lightThreadExecutor = new LightThreadExecutor(exchange);

        final ProxyClient.ProxyTarget target = proxyClient.findTarget(exchange);
        if (target == null) {

            if (LOG.isDebugEnabled())
                LOG.debug("No proxy target for request to {}", exchange.getRequestURL());
            next.handleRequest(exchange);
            return;
        }
        if (exchange.isResponseStarted()) {
            if (LOG.isErrorEnabled())
                LOG.error("Cannot proxy a request that has already started.");
            //we can't proxy a request that has already started, this is basically a server configuration error
            UndertowLogger.REQUEST_LOGGER.cannotProxyStartedRequest(exchange);
            exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
            exchange.endExchange();
            return;
        }
        // check the path prefix for the timeout and then fall back to maxRequestTime.
        String reqPath = exchange.getRequestPath();
        long timeout = maxRequestTime > 0 ? System.currentTimeMillis() + maxRequestTime : 0;
        if (pathPrefixMaxRequestTime != null) {
            for (Map.Entry<String, Integer> entry : pathPrefixMaxRequestTime.entrySet()) {
                String key = entry.getKey();
                if (StringUtils.matchPathToPattern(reqPath, key)) {
                    maxRequestTime = entry.getValue();
                    timeout = System.currentTimeMillis() + maxRequestTime;
                    if (LOG.isTraceEnabled())
                        LOG.trace("Overwritten maxRequestTime {} and timeout {}.", maxRequestTime, timeout);
                    break;
                }
            }
        }

        int maxRetries = maxConnectionRetries;

        if (target instanceof ProxyClient.MaxRetriesProxyTarget)
            maxRetries = Math.max(maxRetries, ((ProxyClient.MaxRetriesProxyTarget) target).getMaxRetries());

        final ProxyClientHandler clientHandler = new ProxyClientHandler(exchange, target, timeout, maxRetries, idempotentRequestPredicate);

        if (timeout > 0) {
            final XnioExecutor.Key key = WorkerUtils.executeAfter(exchange.getIoThread(), () -> clientHandler.cancel(exchange), maxRequestTime, TimeUnit.MILLISECONDS);
            exchange.putAttachment(TIMEOUT_KEY, key);
            exchange.addExchangeCompleteListener((exchange1, nextListener) -> {
                key.remove();
                nextListener.proceed();
            });
        }


        if (exchange.isInIoThread()) exchange.dispatch(lightThreadExecutor, clientHandler);

        else exchange.dispatch(exchange.getIoThread(), clientHandler);
    }

    /**
     * Copies headers from one HeaderMap to another.
     *
     * @param to    - destination HeaderMap
     * @param from  - source HeaderMap
     * @param rules - rules for which headers to copy/change
     */
    static void copyHeaders(final HeaderMap to, final HeaderMap from, final List<QueryHeaderRewriteRule> rules) {
        long f = from.fastIterateNonEmpty();
        HeaderValues values;
        while (f != -1L) {
            values = from.fiCurrent(f);
            if (!to.contains(values.getHeaderName())) {
                if (rules != null && rules.size() > 0) {
                    for (QueryHeaderRewriteRule rule : rules) {
                        if (rule.getOldK().equals(values.getHeaderName().toString())) {
                            HttpString key = values.getHeaderName();
                            if (rule.getNewK() != null) {
                                // newK is not null, it means the key has to be changed. Create a new HeaderValues object.
                                key = new HttpString(rule.getNewK());
                            }
                            // check if we need to replace the value with the oldV and newV
                            if (rule.getOldV() != null && rule.getNewV() != null) {
                                boolean add = false;
                                Iterator<String> it = values.iterator();
                                while (it.hasNext()) {
                                    String value = it.next();
                                    if (rule.getOldV().equals(value)) {
                                        it.remove();
                                        add = true;
                                    }
                                }
                                if (add) values.addFirst(rule.getNewV());
                            }
                            to.putAll(key, values);
                        }
                    }
                } else {
                    //don't over write existing headers, normally the map will be empty, if it is not we assume it is not for a reason
                    to.putAll(values.getHeaderName(), values);
                }
            }
            f = from.fiNextNonEmpty(f);
        }
    }

    public ProxyClient getProxyClient() {
        return proxyClient;
    }

    @Override
    public String toString() {
        var proxyTargets = proxyClient.getAllTargets();

        if (proxyTargets.isEmpty())
            return "ProxyHandler - " + proxyClient.getClass().getSimpleName();

        if (proxyTargets.size() == 1 && !rewriteHostHeader)
            return "reverse-proxy( '" + proxyTargets.get(0).toString() + "' )";

        else {
            var outputResult = "reverse-proxy( { '" + proxyTargets.stream().map(s -> s.toString()).collect(Collectors.joining("', '")) + "' }";

            if (this.rewriteHostHeader)
                outputResult += ", rewrite-host-header=true";

            return outputResult + " )";
        }
    }

    private final class ProxyClientHandler implements ProxyCallback<ProxyConnection>, Runnable {
        private int tries;
        private final long timeout;
        private final int maxRetryAttempts;
        private final HttpServerExchange exchange;
        private final Predicate idempotentPredicate;
        private ProxyClient.ProxyTarget target;

        ProxyClientHandler(HttpServerExchange exchange, ProxyClient.ProxyTarget target, long timeout, int maxRetryAttempts, Predicate idempotentPredicate) {
            this.exchange = exchange;
            this.timeout = timeout;
            this.maxRetryAttempts = maxRetryAttempts;
            this.target = target;
            this.idempotentPredicate = idempotentPredicate;
        }

        @Override
        public void run() {
            proxyClient.getConnection(this.target, this.exchange, this, -1, TimeUnit.MILLISECONDS);
        }

        @Override
        public void completed(final HttpServerExchange exchange, final ProxyConnection connection) {
            exchange.putAttachment(CONNECTION, connection);
            exchange.dispatch(lightThreadExecutor, new ProxyAction(connection, exchange, requestHeaders, rewriteHostHeader, reuseXForwarded, exchange.isRequestComplete() ? this : null, idempotentPredicate, urlRewriteRules, methodRewriteRules, queryParamRewriteRules, headerRewriteRules));
        }

        @Override
        public void failed(final HttpServerExchange exchange) {

            if (LOG.isDebugEnabled())
                LOG.debug("Failed calling backend with tries = " + this.tries + " maxRetryAttempts = " + this.maxRetryAttempts);

            final long time = System.currentTimeMillis();

            if (this.tries++ < this.maxRetryAttempts) {

                if (this.timeout > 0 && time > this.timeout) {

                    if (LOG.isTraceEnabled())
                        LOG.trace("Current time = " + time + " passes timeout " + this.timeout);

                    cancel(exchange);
                } else {
                    this.target = proxyClient.findTarget(exchange);

                    if (LOG.isTraceEnabled())
                        LOG.trace("Retry target = " + target);

                    if (this.target != null) {
                        final long remaining = this.timeout > 0 ? this.timeout - time : -1;

                        if (LOG.isTraceEnabled())
                            LOG.trace("Retry with remaining = " + remaining);

                        proxyClient.getConnection(target, exchange, this, remaining, TimeUnit.MILLISECONDS);
                    } else {

                        if (LOG.isTraceEnabled())
                            LOG.trace("Target is null, cannot resolve the backend");

                        couldNotResolveBackend(exchange); // The context was registered when we started, so return 503
                    }
                }
            } else {

                if (LOG.isTraceEnabled())
                    LOG.trace("Max number fo retry attempts reached.");

                couldNotResolveBackend(exchange);
            }
        }

        @Override
        public void queuedRequestFailed(HttpServerExchange exchange) {
            failed(exchange);
        }

        @Override
        public void couldNotResolveBackend(HttpServerExchange ex) {

            if (ex.isResponseStarted())
                IoUtils.safeClose(ex.getConnection());

            else {
                ex.setStatusCode(StatusCodes.SERVICE_UNAVAILABLE);
                AbstractMetricsHandler metricsHandler = (AbstractMetricsHandler) exchange.getAttachment(AttachmentConstants.METRICS_HANDLER);
                if(metricsHandler != null) {
                    String metricsName = exchange.getAttachment(AttachmentConstants.DOWNSTREAM_METRICS_NAME);
                    if (metricsName != null) {
                        long startTime = exchange.getAttachment(AttachmentConstants.DOWNSTREAM_METRICS_START);
                        metricsHandler.injectMetrics(exchange, startTime, metricsName, null);
                    }
                }
                ex.endExchange();
            }
        }

        void cancel(final HttpServerExchange exchange) {

            //NOTE: this method is called only in context of timeouts.
            final ProxyConnection connectionAttachment = exchange.getAttachment(CONNECTION);

            if (connectionAttachment != null) {
                ClientConnection clientConnection = connectionAttachment.getConnection();
                UndertowLogger.PROXY_REQUEST_LOGGER.timingOutRequest(clientConnection.getPeerAddress() + "" + exchange.getRequestURI());
                IoUtils.safeClose(clientConnection);

            } else UndertowLogger.PROXY_REQUEST_LOGGER.timingOutRequest(exchange.getRequestURI());

            if (exchange.isResponseStarted())
                IoUtils.safeClose(exchange.getConnection());

            else {
                exchange.setStatusCode(StatusCodes.GATEWAY_TIME_OUT);
                AbstractMetricsHandler metricsHandler = (AbstractMetricsHandler) exchange.getAttachment(AttachmentConstants.METRICS_HANDLER);
                if(metricsHandler != null) {
                    String metricsName = exchange.getAttachment(AttachmentConstants.DOWNSTREAM_METRICS_NAME);
                    if (metricsName != null) {
                        long startTime = exchange.getAttachment(AttachmentConstants.DOWNSTREAM_METRICS_START);
                        metricsHandler.injectMetrics(exchange, startTime, metricsName, null);
                    }
                }
                exchange.endExchange();
            }
        }

    }

    private static class ProxyAction implements Runnable {
        private final ProxyConnection clientConnection;
        private final HttpServerExchange exchange;
        private final LightThreadExecutor lightThreadExecutor;
        private final Map<HttpString, ExchangeAttribute> requestHeaders;
        private final boolean rewriteHostHeader;
        private final boolean reuseXForwarded;
        private final ProxyClientHandler proxyClientHandler;
        private final Predicate idempotentPredicate;
        private final List<UrlRewriteRule> urlRewriteRules;
        private final List<MethodRewriteRule> methodRewriteRules;
        private final Map<String, List<QueryHeaderRewriteRule>> queryParamRewriteRules;
        private final Map<String, List<QueryHeaderRewriteRule>> headerRewriteRules;

        ProxyAction(final ProxyConnection clientConnection, final HttpServerExchange exchange, Map<HttpString, ExchangeAttribute> requestHeaders,
                    boolean rewriteHostHeader, boolean reuseXForwarded, ProxyClientHandler proxyClientHandler, Predicate idempotentPredicate,
                    List<UrlRewriteRule> urlRewriteRules, List<MethodRewriteRule> methodRewriteRules,
                    Map<String, List<QueryHeaderRewriteRule>> queryParamRewriteRules, Map<String, List<QueryHeaderRewriteRule>> headerRewriteRules) {
            this.clientConnection = clientConnection;
            this.exchange = exchange;
            this.requestHeaders = requestHeaders;
            this.rewriteHostHeader = rewriteHostHeader;
            this.reuseXForwarded = reuseXForwarded;
            this.proxyClientHandler = proxyClientHandler;
            this.idempotentPredicate = idempotentPredicate;
            this.urlRewriteRules = urlRewriteRules;
            this.methodRewriteRules = methodRewriteRules;
            this.queryParamRewriteRules = queryParamRewriteRules;
            this.headerRewriteRules = headerRewriteRules;
            this.lightThreadExecutor = new LightThreadExecutor(exchange);
        }

        @Override
        public void run() {
            final var request = new ClientRequest();

            final var targetURI = this.createProxyRequestTargetURI();

            final var path = this.createProxyRequestURI(targetURI);
            request.setPath(path);

            final var method = this.createProxyRequestMethod(targetURI);
            request.setMethod(method);

            final var remoteHost = this.createProxyRequestRemoteHost(request);
            request.putAttachment(ProxiedRequestAttachments.REMOTE_HOST, remoteHost);

            if (LOG.isTraceEnabled())
                LOG.trace("targetURI = " + targetURI + " requestURI = " + path + " method = " + method);

            this.rewriteHeaders(request, targetURI, remoteHost);

            /* Set the protocol header and attachment */
            this.attachProtocol(request);

            /* Set our remote host/x-forwarded headers */
            this.attachRemoteHost(request);

            /* Set the port */
            this.attachPort(request);

            /* Attach SSL Info to proxy request */
            this.attachSslInfo(request);

            if (LOG.isDebugEnabled())
                LOG.debug("Sending request {} to target {} for exchange {}", request, this.clientConnection.getConnection().getPeerAddress(), exchange);

            this.sendWithCallback(request, remoteHost);
        }

        /**
         * Determines what the remote host string should be for our proxy request.
         *
         * @param r - client request.
         * @return - new remote host string.
         */
        private String createProxyRequestRemoteHost(ClientRequest r) {
            final String remoteHost;
            final InetSocketAddress address = this.exchange.getSourceAddress();

            if (address != null) {
                remoteHost = address.getHostString();

                if (!address.isUnresolved())
                    r.putAttachment(ProxiedRequestAttachments.REMOTE_ADDRESS, address.getAddress().getHostAddress());

                //should never happen, unless this is some form of mock request
            } else remoteHost = "localhost";

            return remoteHost;
        }

        /**
         * Builds a complete URI string for our proxy request.
         *
         * @param target - targetURI
         * @return - requestURI string
         */
        private String createProxyRequestURI(String target) {
            var uriBuilder = new StringBuilder();
            var hasTargetPath = !this.clientConnection.getTargetPath().isEmpty();
            var hasEmptyTargetString = !this.clientConnection.getTargetPath().equals("/") || target.isEmpty();

            if (hasTargetPath && hasEmptyTargetString)
                uriBuilder.append(this.clientConnection.getTargetPath());

            this.rewriteUrl(uriBuilder, target);
            this.rewriteQueryParams(uriBuilder, target);

            return uriBuilder.toString();
        }

        /**
         * Creates proxy request targetURI.
         *
         * @return - new targetURI string
         */
        private String createProxyRequestTargetURI() {
            String targetURI = this.exchange.getRequestURI();

            if (this.exchange.isHostIncludedInRequestURI()) {

                // this part of the code will never reach in the light-router
                int uriPart = targetURI.indexOf("//");

                if (uriPart != -1) {
                    uriPart = targetURI.indexOf("/", uriPart + 2);

                    if (uriPart != -1)
                        targetURI = targetURI.substring(uriPart);
                }
            }

            if (!this.exchange.getResolvedPath().isEmpty() && targetURI.startsWith(this.exchange.getResolvedPath()))
                targetURI = targetURI.substring(this.exchange.getResolvedPath().length());

            return targetURI;
        }

        /**
         * Rewrites the headers for our proxy request.
         *
         * @param r          - client request
         * @param target     - targetURI
         * @param remoteHost - remoteHost
         */
        private void rewriteHeaders(ClientRequest r, String target, String remoteHost) {
            final var inboundRequestHeaders = this.exchange.getRequestHeaders();
            final var outboundRequestHeaders = r.getRequestHeaders();

            copyHeaders(outboundRequestHeaders, inboundRequestHeaders, (List) CollectionUtils.matchEndpointKey(target, (Map) this.headerRewriteRules));

            /* even if client is non-persistent, we don't close connection to backend. */
            if (!this.exchange.isPersistent())
                outboundRequestHeaders.put(Headers.CONNECTION, "keep-alive");

            /* prevent h2c upgrades from hitting backend.  */
            if ("h2c".equals(this.exchange.getRequestHeaders().getFirst(Headers.UPGRADE))) {
                this.exchange.getRequestHeaders().remove(Headers.UPGRADE);
                outboundRequestHeaders.put(Headers.CONNECTION, "keep-alive");
            }

            /* remove null/empty headers, otherwise push to outbound */
            for (var entry : this.requestHeaders.entrySet()) {

                var headerValue = entry.getValue().readAttribute(this.exchange);

                if (headerValue == null || headerValue.isEmpty())
                    outboundRequestHeaders.remove(entry.getKey());
                else outboundRequestHeaders.put(entry.getKey(), headerValue.replace('\n', ' '));
            }

            /* check to see if we can reuse x-forwarded if request has one. */
            if (this.reuseXForwarded && r.getRequestHeaders().contains(Headers.X_FORWARDED_FOR)) {
                final String current = r.getRequestHeaders().getFirst(Headers.X_FORWARDED_FOR);

                if (current == null || current.isEmpty())
                    r.getRequestHeaders().put(Headers.X_FORWARDED_FOR, remoteHost);

                else r.getRequestHeaders().put(Headers.X_FORWARDED_FOR, current + "," + remoteHost);

            } else r.getRequestHeaders().put(Headers.X_FORWARDED_FOR, remoteHost);

            // If we don't support push, set a new header saying so.
            // This is non-standard and a problem with the HTTP2 spec, but they did not want to listen
            if (!this.exchange.getConnection().isPushSupported() && this.clientConnection.getConnection().isPushSupported())
                r.getRequestHeaders().put(Headers.X_DISABLE_PUSH, "true");

            /* rewrite host header and update x-forwarded-host. */
            if (this.rewriteHostHeader) {
                InetSocketAddress targetAddress = this.clientConnection.getConnection().getPeerAddress(InetSocketAddress.class);
                r.getRequestHeaders().put(Headers.HOST, targetAddress.getHostString() + ":" + targetAddress.getPort());
                r.getRequestHeaders().put(Headers.X_FORWARDED_HOST, this.exchange.getRequestHeaders().getFirst(Headers.HOST));
            }

            // If the frontend is HTTP/2, then we may need to add a Transfer-Encoding header to indicate to the backend
            // that there is content.
            if (!r.getRequestHeaders().contains(Headers.TRANSFER_ENCODING) && !r.getRequestHeaders().contains(Headers.CONTENT_LENGTH) && !this.exchange.isRequestComplete())
                r.getRequestHeaders().put(Headers.TRANSFER_ENCODING, Headers.CHUNKED.toString());

            // remove the serviceId and serviceUrl so that they won't be sent to the downstream API as it might be a gateway or sidecar.
            // the serviceId and serviceUrl might impact the pathPrefixServiceHandler to detect the serviceId.
            r.getRequestHeaders().remove(HttpStringConstants.SERVICE_URL);
            r.getRequestHeaders().remove(HttpStringConstants.SERVICE_ID);
        }

        /**
         * Creates an HttpString of the method from the request.
         * We rewrite the method if there is a rule defined.
         *
         * @param target - targetURI
         * @return - HttpString of the method for our proxy request.
         */
        private HttpString createProxyRequestMethod(String target) {

            // handler the method rewrite here.
            var m = this.exchange.getRequestMethod();

            if (this.methodRewriteRules != null && this.methodRewriteRules.size() > 0)
                for (var rule : this.methodRewriteRules)
                    if (StringUtils.matchPathToPattern(target, rule.getRequestPath()) && m.toString().equals(rule.getSourceMethod())) {

                        if (LOG.isDebugEnabled())
                            LOG.debug("Rewrite HTTP method from {} to {} with path {} and pathPattern {}", rule.getSourceMethod(), rule.getTargetMethod(), target, rule.getRequestPath());

                        m = new HttpString(rule.getTargetMethod());
                    }

            return m;
        }

        /**
         * Rewrites the proxy request url based on defined rules.
         *
         * @param uriBuilder - new url
         * @param target     - targetURI
         */
        private void rewriteUrl(StringBuilder uriBuilder, String target) {

            /* Rewrites the url. Uses original if there are no rules matches/no rules defined. */
            if (this.urlRewriteRules != null && this.urlRewriteRules.size() > 0) {
                var matched = false;

                for (var rule : this.urlRewriteRules) {
                    var matcher = rule.getPattern().matcher(target);

                    if (matcher.matches()) {
                        matched = true;
                        uriBuilder.append(matcher.replaceAll(rule.getReplace()));
                        break;
                    }
                }
                if (!matched)
                    uriBuilder.append(target);

            } else uriBuilder.append(target);
        }

        /**
         * Rewrites query parameters from our proxy request based on defined rules.
         *
         * @param urlBuilder - new url
         * @param target     - targetURI
         */
        private void rewriteQueryParams(StringBuilder urlBuilder, String target) {

            if (this.queryParamRewriteRules != null && this.queryParamRewriteRules.size() > 0) {
                var rules = (List<QueryHeaderRewriteRule>) CollectionUtils.matchEndpointKey(target, (Map) this.queryParamRewriteRules);

                if (rules != null && rules.size() > 0) {

                    var params = this.exchange.getQueryParameters();

                    for (var rule : rules) {

                        if (params.get(rule.getOldK()) != null) {
                            var values = params.get(rule.getOldK());

                            // we only iterate the values if oldV and newV are defined.
                            if (rule.getOldV() != null && rule.getNewV() != null) {
                                var it = values.iterator();
                                boolean add = false;

                                while (it.hasNext()) {
                                    if (it.next().equals(rule.getOldV())) {
                                        it.remove();
                                        add = true;
                                    }
                                }

                                if (add)
                                    values.addFirst(rule.getNewV());
                            }

                            if (rule.getNewK() != null) {
                                params.remove(rule.getOldK());
                                params.put(rule.getNewK(), values);

                            } else {
                                params.put(rule.getOldK(), values);
                            }
                        }
                    }

                    var qs = QueryParameterUtils.buildQueryString(params);

                    if (!qs.isEmpty()) {
                        urlBuilder.append('?');
                        urlBuilder.append(qs);
                    }
                }
            } else {
                var qs = exchange.getQueryString();

                if (qs != null && !qs.isEmpty()) {
                    urlBuilder.append('?');
                    urlBuilder.append(qs);
                }
            }
        }

        private void attachProtocol(ClientRequest r) {
            String p;

            if (this.reuseXForwarded && this.exchange.getRequestHeaders().contains(Headers.X_FORWARDED_PROTO))
                p = this.exchange.getRequestHeaders().getFirst(Headers.X_FORWARDED_PROTO);

            else {
                p = this.exchange.getRequestScheme().equals("https") ? "https" : "http";
                r.getRequestHeaders().put(Headers.X_FORWARDED_PROTO, p);
            }

            r.putAttachment(ProxiedRequestAttachments.IS_SSL, p.equals("https"));
        }

        private void attachPort(ClientRequest r) {
            int port;

            if (this.reuseXForwarded && this.exchange.getRequestHeaders().contains(Headers.X_FORWARDED_PORT)) {

                try {
                    port = Integer.parseInt(exchange.getRequestHeaders().getFirst(Headers.X_FORWARDED_PORT));
                    r.putAttachment(ProxiedRequestAttachments.SERVER_PORT, port);

                } catch (NumberFormatException e) {
                    port = this.exchange.getConnection().getLocalAddress(InetSocketAddress.class).getPort();
                    r.getRequestHeaders().put(Headers.X_FORWARDED_PORT, port);
                    r.putAttachment(ProxiedRequestAttachments.SERVER_PORT, port);
                }

            } else {
                port = this.exchange.getHostPort();
                r.getRequestHeaders().put(Headers.X_FORWARDED_PORT, port);
                r.putAttachment(ProxiedRequestAttachments.SERVER_PORT, port);
            }
        }

        private void attachRemoteHost(ClientRequest r) {
            String host;

            if (reuseXForwarded && this.exchange.getRequestHeaders().contains(Headers.X_FORWARDED_SERVER))
                host = this.exchange.getRequestHeaders().getFirst(Headers.X_FORWARDED_SERVER);

            else {
                host = this.exchange.getHostName();
                r.getRequestHeaders().put(Headers.X_FORWARDED_SERVER, host);
            }

            r.putAttachment(ProxiedRequestAttachments.SERVER_NAME, host);

            if (!this.exchange.getRequestHeaders().contains(Headers.X_FORWARDED_HOST)) {
                host = this.exchange.getHostName();

                if (host != null)
                    r.getRequestHeaders().put(Headers.X_FORWARDED_HOST, NetworkUtils.formatPossibleIpv6Address(host));

            }
        }

        private void sendWithCallback(ClientRequest r, String host) {
            this.clientConnection.getConnection().sendRequest(r, new ClientCallback<>() {

                @Override
                public void completed(final ClientExchange result) {

                    if (LOG.isDebugEnabled())
                        LOG.debug("Sent request {} to target {} for exchange {}", r, host, exchange);

                    result.putAttachment(EXCHANGE, exchange);

                    boolean requiresContinueResponse = HttpContinue.requiresContinueResponse(exchange);

                    if (requiresContinueResponse)
                        this.prepContinueHandler(result);

                    if (exchange.getConnection().isPushSupported() && result.getConnection().isPushSupported())
                        this.handleServerPush(result);

                    result.setResponseListener(new ResponseCallback(exchange, proxyClientHandler, idempotentPredicate, headerRewriteRules));

                    final IoExceptionHandler handler = new IoExceptionHandler(exchange, clientConnection.getConnection());

                    if (requiresContinueResponse) {
                        this.prepRequestChannelForContinue(result, handler);
                        return;
                    }

                    HTTPTrailerChannelListener trailerListener = new HTTPTrailerChannelListener(exchange, result, exchange, proxyClientHandler, idempotentPredicate);

                    if (!exchange.isRequestComplete())
                        Transfer.initiateTransfer(exchange.getRequestChannel(), result.getRequestChannel(), ChannelListeners.closingChannelListener(), trailerListener, handler, handler, exchange.getConnection().getByteBufferPool());

                    else trailerListener.handleEvent(result.getRequestChannel());

                }

                @Override
                public void failed(IOException e) {
                    handleFailure(exchange, proxyClientHandler, idempotentPredicate, e);
                }

                private void prepContinueHandler(ClientExchange result) {
                    result.setContinueHandler(clientExchange -> {

                        if (LOG.isDebugEnabled())
                            LOG.debug("Received continue response to request {} to target {} for exchange {}", r, clientConnection.getConnection().getPeerAddress(), exchange);

                        HttpContinue.sendContinueResponse(exchange, new IoCallback() {
                            @Override
                            public void onComplete(final HttpServerExchange exchange, final Sender sender) {
                                //don't care
                            }

                            @Override
                            public void onException(final HttpServerExchange exchange, final Sender sender, final IOException exception) {
                                IoUtils.safeClose(clientConnection.getConnection());
                                exchange.endExchange();
                                UndertowLogger.REQUEST_IO_LOGGER.ioException(exception);
                            }
                        });
                    });
                }

                private void prepRequestChannelForContinue(ClientExchange ce, IoExceptionHandler io) {
                    try {
                        if (!ce.getRequestChannel().flush()) {
                            ce.getRequestChannel().getWriteSetter().set(ChannelListeners.flushingChannelListener((ChannelListener<StreamSinkChannel>) flushedChannel ->
                                            Transfer.initiateTransfer(
                                                    exchange.getRequestChannel(),
                                                    ce.getRequestChannel(),
                                                    ChannelListeners.closingChannelListener(),
                                                    new HTTPTrailerChannelListener(exchange, ce, exchange, proxyClientHandler, idempotentPredicate),
                                                    io,
                                                    io,
                                                    exchange.getConnection().getByteBufferPool()),
                                    io));
                            ce.getRequestChannel().resumeWrites();
                        }
                    } catch (IOException e) {
                        io.handleException(ce.getRequestChannel(), e);
                    }
                }

                private void handleServerPush(ClientExchange ce) {
                    ce.setPushHandler((originalRequest, pushedRequest) -> {

                        if (LOG.isDebugEnabled())
                            LOG.debug("Sending push request {} received from {} to target {} for exchange {}", pushedRequest.getRequest(), r, host, exchange);

                        final ClientRequest req = pushedRequest.getRequest();

                        exchange.getConnection().pushResource(req.getPath(), req.getMethod(), req.getRequestHeaders(), exchange -> {
                            String path = req.getPath();
                            int i = path.indexOf("?");

                            if (i > 0)
                                path = path.substring(0, i);

                            exchange.dispatch(lightThreadExecutor, new ProxyAction(new ProxyConnection(pushedRequest.getConnection(), path), exchange, requestHeaders, rewriteHostHeader, reuseXForwarded, null, idempotentPredicate, urlRewriteRules, methodRewriteRules, queryParamRewriteRules, headerRewriteRules));
                        });
                        return true;
                    });

                }
            });
        }

        private void attachSslInfo(ClientRequest r) {
            var sslSessionInfo = this.exchange.getConnection().getSslSessionInfo();

            if (sslSessionInfo != null) {
                Certificate[] peerCertificates;
                try {
                    peerCertificates = sslSessionInfo.getPeerCertificates();

                    if (peerCertificates.length > 0)
                        r.putAttachment(ProxiedRequestAttachments.SSL_CERT, Certificates.toPem(peerCertificates[0]));

                } catch (SSLPeerUnverifiedException | CertificateEncodingException | RenegotiationRequiredException e) {
                    //ignore
                }
                r.putAttachment(ProxiedRequestAttachments.SSL_CYPHER, sslSessionInfo.getCipherSuite());
                r.putAttachment(ProxiedRequestAttachments.SSL_SESSION_ID, sslSessionInfo.getSessionId());
                r.putAttachment(ProxiedRequestAttachments.SSL_KEY_SIZE, sslSessionInfo.getKeySize());
            }
        }
    }

    static void handleFailure(HttpServerExchange exchange, ProxyClientHandler proxyClientHandler, Predicate idempotentRequestPredicate, IOException e) {
        UndertowLogger.PROXY_REQUEST_LOGGER.proxyRequestFailed(exchange.getRequestURI(), e);

        if (exchange.isResponseStarted())
            IoUtils.safeClose(exchange.getConnection());

        else if (idempotentRequestPredicate.resolve(exchange) && proxyClientHandler != null)
            proxyClientHandler.failed(exchange); //this will attempt a retry if configured to do so

        else {
            exchange.setStatusCode(StatusCodes.SERVICE_UNAVAILABLE);
            exchange.endExchange();
        }
    }

    private static final class ResponseCallback implements ClientCallback<ClientExchange> {

        private final HttpServerExchange exchange;
        private final ProxyClientHandler proxyClientHandler;
        private final Predicate idempotentPredicate;
        private final Map<String, List<QueryHeaderRewriteRule>> headerRewriteRules;

        private ResponseCallback(HttpServerExchange exchange, ProxyClientHandler proxyClientHandler, Predicate idempotentPredicate, Map<String, List<QueryHeaderRewriteRule>> headerRewriteRules) {
            this.exchange = exchange;
            this.proxyClientHandler = proxyClientHandler;
            this.idempotentPredicate = idempotentPredicate;
            this.headerRewriteRules = headerRewriteRules;
        }

        @Override
        public void completed(final ClientExchange result) {

            final ClientResponse response = result.getResponse();

            if (LOG.isDebugEnabled())
                LOG.debug("Received response {} for request {} for exchange {}", response, result.getRequest(), exchange);

            final HeaderMap inbound = response.getResponseHeaders();
            final HeaderMap outbound = exchange.getResponseHeaders();
            exchange.setStatusCode(response.getResponseCode());

            copyHeaders(outbound, inbound, (List) CollectionUtils.matchEndpointKey(exchange.getRequestPath(), (Map) headerRewriteRules));

            if (exchange.isUpgrade())
                this.handleUpgradeChannelOnComplete(result);

            final IoExceptionHandler handler = new IoExceptionHandler(exchange, result.getConnection());

            Transfer.initiateTransfer(result.getResponseChannel(), exchange.getResponseChannel(), ChannelListeners.closingChannelListener(), new HTTPTrailerChannelListener(result, exchange, exchange, proxyClientHandler, idempotentPredicate), handler, handler, exchange.getConnection().getByteBufferPool());

            AbstractMetricsHandler metricsHandler = (AbstractMetricsHandler) exchange.getAttachment(AttachmentConstants.METRICS_HANDLER);
            if(metricsHandler != null) {
                String metricsName = exchange.getAttachment(AttachmentConstants.DOWNSTREAM_METRICS_NAME);
                if (metricsName != null) {
                    long startTime = exchange.getAttachment(AttachmentConstants.DOWNSTREAM_METRICS_START);
                    metricsHandler.injectMetrics(exchange, startTime, metricsName, null);
                }
            }
        }

        private void handleUpgradeChannelOnComplete(ClientExchange result) {
            exchange.upgradeChannel((streamConnection, exchange) -> {

                if (LOG.isDebugEnabled())
                    LOG.debug("Upgraded request {} to for exchange {}", result.getRequest(), exchange);

                StreamConnection clientChannel = null;

                try {
                    clientChannel = result.getConnection().performUpgrade();
                    final ClosingExceptionHandler handler = new ClosingExceptionHandler(streamConnection, clientChannel);

                    Transfer.initiateTransfer(clientChannel.getSourceChannel(), streamConnection.getSinkChannel(), ChannelListeners.closingChannelListener(), ChannelListeners.writeShutdownChannelListener(ChannelListeners.<StreamSinkChannel>flushingChannelListener(ChannelListeners.closingChannelListener(), ChannelListeners.closingChannelExceptionHandler()), ChannelListeners.closingChannelExceptionHandler()), handler, handler, result.getConnection().getBufferPool());
                    Transfer.initiateTransfer(streamConnection.getSourceChannel(), clientChannel.getSinkChannel(), ChannelListeners.closingChannelListener(), ChannelListeners.writeShutdownChannelListener(ChannelListeners.<StreamSinkChannel>flushingChannelListener(ChannelListeners.closingChannelListener(), ChannelListeners.closingChannelExceptionHandler()), ChannelListeners.closingChannelExceptionHandler()), handler, handler, result.getConnection().getBufferPool());

                } catch (IOException e) {
                    IoUtils.safeClose(streamConnection, clientChannel);
                }
            });
        }

        @Override
        public void failed(IOException e) {
            handleFailure(exchange, proxyClientHandler, idempotentPredicate, e);
        }
    }

    private static final class HTTPTrailerChannelListener implements ChannelListener<StreamSinkChannel> {

        private final Attachable source;
        private final Attachable target;
        private final HttpServerExchange exchange;
        private final ProxyClientHandler proxyClientHandler;
        private final Predicate idempotentPredicate;

        private HTTPTrailerChannelListener(final Attachable source, final Attachable target, HttpServerExchange exchange, ProxyClientHandler proxyClientHandler, Predicate idempotentPredicate) {
            this.source = source;
            this.target = target;
            this.exchange = exchange;
            this.proxyClientHandler = proxyClientHandler;
            this.idempotentPredicate = idempotentPredicate;
        }

        @Override
        public void handleEvent(final StreamSinkChannel channel) {
            var trailers = source.getAttachment(HttpAttachments.REQUEST_TRAILERS);

            if (trailers != null)
                target.putAttachment(HttpAttachments.RESPONSE_TRAILERS, trailers);

            try {
                channel.shutdownWrites();

                if (!channel.flush()) {
                    channel.getWriteSetter().set(ChannelListeners.flushingChannelListener((ChannelListener<StreamSinkChannel>) flushedChannel -> {
                        flushedChannel.suspendWrites();
                        flushedChannel.getWriteSetter().set(null);
                    }, ChannelListeners.closingChannelExceptionHandler()));

                    channel.resumeWrites();

                } else {
                    channel.getWriteSetter().set(null);
                    channel.shutdownWrites();
                }

            } catch (IOException e) {

                if (LOG.isErrorEnabled())
                    LOG.error("IOException: ", e);

                handleFailure(exchange, proxyClientHandler, idempotentPredicate, e);

            } catch (Exception e) {

                if (LOG.isErrorEnabled())
                    LOG.error("Exception: ", e);

                handleFailure(exchange, proxyClientHandler, idempotentPredicate, new IOException(e));
            }

        }
    }

    private static final class IoExceptionHandler implements ChannelExceptionHandler<Channel> {

        private final HttpServerExchange exchange;
        private final ClientConnection clientConnection;

        private IoExceptionHandler(HttpServerExchange exchange, ClientConnection clientConnection) {
            this.exchange = exchange;
            this.clientConnection = clientConnection;
        }

        @Override
        public void handleException(Channel channel, IOException e) {
            IoUtils.safeClose(channel);
            IoUtils.safeClose(clientConnection);

            if (exchange.isResponseStarted()) {
                UndertowLogger.REQUEST_IO_LOGGER.debug("Exception reading from target server", e);

                if (!exchange.isResponseStarted()) {
                    exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                    exchange.endExchange();

                } else IoUtils.safeClose(exchange.getConnection());

            } else {
                UndertowLogger.REQUEST_IO_LOGGER.ioException(e);
                exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                exchange.endExchange();
            }
        }
    }

    private static final class ClosingExceptionHandler implements ChannelExceptionHandler<Channel> {

        private final Closeable[] toClose;

        private ClosingExceptionHandler(Closeable... toClose) {
            this.toClose = toClose;
        }


        @Override
        public void handleException(Channel channel, IOException exception) {
            IoUtils.safeClose(channel);
            IoUtils.safeClose(toClose);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private ProxyClient proxyClient;
        private int maxRequestTime = -1;
        private Map<String, Integer> pathPrefixMaxRequestTime;
        private final Map<HttpString, ExchangeAttribute> requestHeaders = new CopyOnWriteMap<>();
        private HttpHandler next = ResponseCodeHandler.HANDLE_404;
        private boolean rewriteHostHeader;
        private boolean reuseXForwarded;
        private int maxConnectionRetries = DEFAULT_MAX_RETRY_ATTEMPTS;
        private int maxQueueSize = DEFAULT_MAX_QUEUE_SIZE;
        private Predicate idempotentRequestPredicate = IdempotentPredicate.INSTANCE;
        private List<UrlRewriteRule> urlRewriteRules;
        private List<MethodRewriteRule> methodRewriteRules;
        private Map<String, List<QueryHeaderRewriteRule>> queryParamRewriteRules;
        private Map<String, List<QueryHeaderRewriteRule>> headerRewriteRules;

        Builder() {
        }


        public Builder setProxyClient(ProxyClient proxyClient) {

            if (proxyClient == null)
                throw UndertowMessages.MESSAGES.argumentCannotBeNull("proxyClient");

            this.proxyClient = proxyClient;
            return this;
        }

        public Builder setMaxRequestTime(int max) {
            this.maxRequestTime = max;
            return this;
        }

        public Builder setPathPrefixMaxRequestTime(Map<String, Integer> maxReqTime) {
            this.pathPrefixMaxRequestTime = maxReqTime;
            return this;
        }

        public Map<HttpString, ExchangeAttribute> getRequestHeaders() {
            return Collections.unmodifiableMap(requestHeaders);
        }

        public Builder addRequestHeader(HttpString k, ExchangeAttribute v) {
            this.requestHeaders.put(k, v);
            return this;
        }

        public HttpHandler getNext() {
            return next;
        }

        public Builder setNext(HttpHandler next) {
            this.next = next;
            return this;
        }

        public Builder setRewriteHostHeader(boolean rewriteHostHeader) {
            this.rewriteHostHeader = rewriteHostHeader;
            return this;
        }

        public Builder setReuseXForwarded(boolean reuseXForwarded) {
            this.reuseXForwarded = reuseXForwarded;
            return this;
        }

        public List<UrlRewriteRule> getUrlRewriteRules() {
            return urlRewriteRules;
        }

        public Builder setUrlRewriteRules(List<UrlRewriteRule> urlRewriteRules) {
            this.urlRewriteRules = urlRewriteRules;
            return this;
        }

        public Builder setMethodRewriteRules(List<MethodRewriteRule> methodRewriteRules) {
            this.methodRewriteRules = methodRewriteRules;
            return this;
        }

        public Builder setQueryParamRewriteRules(Map<String, List<QueryHeaderRewriteRule>> queryParamRewriteRules) {
            this.queryParamRewriteRules = queryParamRewriteRules;
            return this;
        }

        public Builder setHeaderRewriteRules(Map<String, List<QueryHeaderRewriteRule>> headerRewriteRules) {
            this.headerRewriteRules = headerRewriteRules;
            return this;
        }

        public Builder setMaxConnectionRetries(int maxConnectionRetries) {
            this.maxConnectionRetries = maxConnectionRetries;
            return this;
        }

        public Builder setMaxQueueSize(int maxQueueSize) {
            this.maxQueueSize = maxQueueSize;
            return this;
        }

        public Builder setIdempotentRequestPredicate(Predicate idempotentRequestPredicate) {

            if (idempotentRequestPredicate == null)
                throw UndertowMessages.MESSAGES.argumentCannotBeNull("idempotentRequestPredicate");

            this.idempotentRequestPredicate = idempotentRequestPredicate;
            return this;
        }

        public ProxyHandler build() {
            return new ProxyHandler(this);
        }
    }
}
