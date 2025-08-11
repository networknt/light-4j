/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.metrics;

import com.networknt.config.JsonMapper;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.server.Server;
import com.networknt.server.ServerConfig;
import com.networknt.utility.Constants;
import com.networknt.utility.Util;
import io.dropwizard.metrics.*;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metrics middleware handler can be plugged into the request/response chain to capture metrics information
 * for all services. It is based on the dropwizard metric but customized to capture statistic info for a
 * period of time and then reset all the data in order to capture the next period. The capture period can
 * be configured in metrics.yml and normally should be 5 minutes, 10 minutes or 20 minutes depending on the
 * load of the service.
 * <p>
 * This is the generic implementation and all others will be extended from this handler. This handler will
 * be used by others to inject metrics info if enabled.
 *
 * @author Steve Hu
 */
public abstract class AbstractMetricsHandler implements MiddlewareHandler {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMetricsHandler.class);

    protected static final String CLIENT_ID_TAG = "clientId";
    protected static final String SCOPE_CLIENT_ID_TAG = "scopeClientId";
    protected static final String CALLER_ID_TAG = "callerId";
    protected static final String ISSUER_TAG = "issuer";
    protected static final String ENDPOINT_TAG = "endpoint";

    protected static final String HOST_COMMON_TAG = "host";
    protected static final String PORT_COMMON_TAG = "port";
    protected static final String API_COMMON_TAG = "api";
    protected static final String ENV_COMMON_TAG = "env";
    protected static final String ADDR_COMMON_TAG = "addr";

    // The metrics.yml configuration that supports reload.
    public static MetricsConfig config;
    static Pattern pattern;
    // The structure that collect all the metrics entries. Even others will be using this structure to inject.
    public static final MetricRegistry registry = new MetricRegistry();
    public Map<String, String> commonTags = new HashMap<>();

    protected static final String UNKNOWN_TAG_VALUE = "unknown";

    public AbstractMetricsHandler() {
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    protected abstract void createMetricsReporter(final TimeSeriesDbSender sender);

    public void createJVMMetricsReporter(final TimeSeriesDbSender sender) {
        try (JVMMetricsDbReporter jvmReporter = new JVMMetricsDbReporter(
                new MetricRegistry(),
                sender,
                "jvm-reporter",
                MetricFilter.ALL,
                TimeUnit.SECONDS,
                TimeUnit.MILLISECONDS,
                commonTags)
        ) {
            jvmReporter.start(config.getReportInMinutes(), TimeUnit.MINUTES);
        }
    }

    /**
     * This method is used to get the metric type based on the status code.
     *
     * @param statusCode the status code of the response.
     * @return the metric type as a string.
     */
    private static String getMetricTypeForStatusCode(int statusCode) {
        if (statusCode >= 200 && statusCode < 400) {
            return "success";
        } else if (statusCode == 401 || statusCode == 403) {
            return "auth_error";
        } else if (statusCode >= 400 && statusCode < 500) {
            return "request_error";
        } else if (statusCode >= 500) {
            return "server_error";
        }
        return null;
    }

    /**
     * This method is used to add common tags to the metrics that are collected by the metrics handler.
     *
     * @param commonTags the map that contains the common tags to be added to the metrics.
     */
    protected static void addCommonTags(final Map<String, String> commonTags) {
        commonTags.put(API_COMMON_TAG, ServerConfig.getInstance().getServiceId());
        commonTags.put(ENV_COMMON_TAG, ServerConfig.getInstance().getEnvironment());
        commonTags.put(ADDR_COMMON_TAG, Server.currentAddress);

        if (ServerConfig.getInstance().isEnableHttps()) {
            commonTags.put(PORT_COMMON_TAG, "" + Server.currentHttpsPort);
        } else {
            commonTags.put(PORT_COMMON_TAG, "" + Server.currentHttpPort);
        }
        InetAddress inetAddress = Util.getInetAddress();
        if (inetAddress != null) {
            commonTags.put(HOST_COMMON_TAG, inetAddress.getHostName());
        } else {
            commonTags.put(HOST_COMMON_TAG, UNKNOWN_TAG_VALUE);  // will be container id if in docker.
        }

        if (logger.isDebugEnabled()) {
            logger.debug(commonTags.toString());
        }
    }

    /**
     * This method is used to set a tag with the value from the auditInfo map if it exists.
     *
     * @param auditInfo the map that contains the audit information.
     * @param tags the map that contains the tags to be set.
     * @param auditField the field in the auditInfo map that contains the value to be set as a tag.
     * @param tagName the name of the tag to be set.
     */
    protected static void setOrUnknownTag(final Map<String, Object> auditInfo, final Map<String, String> tags, final String auditField, final String tagName) {
        if (auditInfo != null && auditInfo.get(auditField) instanceof String value) {
            tags.put(tagName, value);
            logger.trace("{} = {}", tagName, value);
        } else {
            tags.put(tagName, UNKNOWN_TAG_VALUE);
            logger.trace("{} is not available in the auditInfo, setting {} tag to {}.", auditField, tagName, UNKNOWN_TAG_VALUE);
        }
    }

    /**
     * This method is used to inject anonymous metrics for handlers that do not have auditInfo in the exchange.
     *
     * @param tags the map that contains the tags to be set for anonymous metrics.
     * @param endpoint the endpoint that is used to collect the metrics. It is optional and only provided by the external handlers.
     */
    private void injectAnonymousMetrics(final Map<String, String> tags, final String endpoint) {
        // for MRAS and Salesforce handlers that do not have auditInfo in the exchange as they may be called anonymously.
        tags.put(Constants.ENDPOINT_STRING, Objects.requireNonNullElse(endpoint, UNKNOWN_TAG_VALUE));

        tags.put(CLIENT_ID_TAG, UNKNOWN_TAG_VALUE);
        if (config.isSendScopeClientId()) {
            tags.put(SCOPE_CLIENT_ID_TAG, UNKNOWN_TAG_VALUE);
        }
        if (config.isSendCallerId()) {
            tags.put(CALLER_ID_TAG, UNKNOWN_TAG_VALUE);
        }
        if (config.isSendIssuer()) {
            tags.put(ISSUER_TAG, UNKNOWN_TAG_VALUE);
        }
    }

    /**
     * This method is used to inject audit information metrics into the tags map.
     *
     * @param tags the map that contains the tags to be set for audit information metrics.
     * @param auditInfo the map that contains the audit information to be used for metrics.
     * @param endpoint the endpoint that is used to collect the metrics. It is optional and only provided by the external handlers.
     */
    private void injectAuditInfoMetrics(final Map<String, String> tags, final Map<String, Object> auditInfo, final String endpoint) {
        // for external handlers, the endpoint must be unknown in the auditInfo. If that is the case, use the endpoint passed in.
        if (endpoint != null) {
            logger.trace("Using endpoint argument {}", endpoint);
            tags.put(ENDPOINT_TAG, endpoint);
        } else if (auditInfo.get(Constants.ENDPOINT_STRING) instanceof String endpointString) {
            logger.trace("Endpoint argument was null, but found endpoint in auditInfo: {}", endpointString);
            tags.put(ENDPOINT_TAG, endpointString);
        } else {
            logger.trace("endpoint is not available in the auditInfo, setting endpoint tag to {}.", UNKNOWN_TAG_VALUE);
            tags.put(ENDPOINT_TAG, UNKNOWN_TAG_VALUE);
        }

        if (config.isSendScopeClientId()) {
            AbstractMetricsHandler.setOrUnknownTag(auditInfo, tags, Constants.SCOPE_CLIENT_ID_STRING, SCOPE_CLIENT_ID_TAG);
        }

        AbstractMetricsHandler.setOrUnknownTag(auditInfo, tags, Constants.CLIENT_ID_STRING, CLIENT_ID_TAG);

        if (config.isSendCallerId()) {
            AbstractMetricsHandler.setOrUnknownTag(auditInfo, tags, Constants.CALLER_ID_STRING, CALLER_ID_TAG);
        }

        if (config.isSendIssuer()) {
            AbstractMetricsHandler.addIssuerRegex(tags, auditInfo);
        }
    }

    /**
     * Adds the issuer regex to the tags map if the issuer is present in the auditInfo.
     *
     * @param tags the map that contains the tags to be set for the issuer.
     * @param auditInfo the map that contains the audit information to be used for metrics.
     */
    protected static void addIssuerRegex(final Map<String, String> tags, final Map<String, Object> auditInfo) {
        if (auditInfo.get(Constants.ISSUER_CLAIMS) instanceof String issuer) {
            // we need to send issuer as a tag. Do we need to apply regex to extract only a part of the issuer?
            if (config.getIssuerRegex() != null) {
                Matcher matcher = pattern.matcher(issuer);
                if (matcher.find() && matcher.groupCount() > 0) {
                    String iss = matcher.group(1);
                    logger.trace("Extracted issuer {} from Original issuer {} is sent.", iss, issuer);
                    tags.put(ISSUER_TAG, Objects.requireNonNullElse(iss, UNKNOWN_TAG_VALUE));
                }
            } else {
                logger.trace("Original issuer {} is sent.", issuer);
                tags.put(ISSUER_TAG, issuer);
            }
        }
    }

    /**
     * This is the method that is used for all other handlers to inject its metrics info to the real metrics handler impl.
     *
     * @param httpServerExchange the HttpServerExchange that is used to get the auditInfo to collect the metrics tag.
     * @param startTime the start time passed in to calculate the response time.
     * @param metricsName the name of the metrics that is collected.
     * @param endpoint the endpoint that is used to collect the metrics. It is optional and only provided by the external handlers.
     */
    public void injectMetrics(HttpServerExchange httpServerExchange, long startTime, final String metricsName, final String endpoint) {

        final Map<String, Object> auditInfo = httpServerExchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        logger.trace("auditInfo = {}", auditInfo);

        final Map<String, String> tags = new HashMap<>();

        if (auditInfo != null) {
            this.injectAuditInfoMetrics(tags, auditInfo, endpoint);
        } else {
            this.injectAnonymousMetrics(tags, endpoint);
        }

        MetricName metricName = new MetricName(metricsName);
        metricName = metricName.tagged(commonTags);
        metricName = metricName.tagged(tags);
        long time = System.nanoTime() - startTime;
        registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.TIMERS).update(time, TimeUnit.NANOSECONDS);
    }

    /**
     * Access the metrics handler singleton instance from the handler chain.
     *
     * @return the metrics handler instance or null if not found.
     */
    public static AbstractMetricsHandler lookupMetricsHandler() {
        // get the metrics handler from the handler chain for metrics registration. If we cannot get the
        // metrics handler, then an error message will be logged.
        Map<String, HttpHandler> handlers = Handler.getHandlers();
        AbstractMetricsHandler metricsHandler = (AbstractMetricsHandler) handlers.get(MetricsConfig.CONFIG_NAME);
        if (metricsHandler == null) {
            logger.error("An instance of MetricsHandler is not configured in the handler.yml or needs to be moved up in order.");
        }
        return metricsHandler;
    }

    protected static class MetricsExchangeCompletionListener implements ExchangeCompletionListener {

        private final Map<String, String> commonTags;
        private final long startTime;

        public MetricsExchangeCompletionListener(final Map<String, String> commonTags, final long startTime) {
            this.commonTags = Objects.requireNonNull(commonTags, "commonTags cannot be null");
            this.startTime = startTime;
        }

        @Override
        public void exchangeEvent(HttpServerExchange exchange, NextListener nextListener) {
            try {
                Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
                if (auditInfo != null && !auditInfo.isEmpty()) {
                    Map<String, String> tags = new HashMap<>();
                    AbstractMetricsHandler.setOrUnknownTag(auditInfo, tags, Constants.ENDPOINT_STRING, ENDPOINT_TAG);
                    AbstractMetricsHandler.setOrUnknownTag(auditInfo, tags, Constants.CLIENT_ID_STRING, CLIENT_ID_TAG);

                    if (config.isSendScopeClientId()) {
                        AbstractMetricsHandler.setOrUnknownTag(auditInfo, tags, Constants.SCOPE_CLIENT_ID_STRING, SCOPE_CLIENT_ID_TAG);
                    }

                    // caller id is the calling serviceId that is passed from the caller. It is not always available but some organizations enforce it.
                    if (config.isSendCallerId()) {
                        AbstractMetricsHandler.setOrUnknownTag(auditInfo, tags, Constants.CALLER_ID_STRING, CALLER_ID_TAG);
                    }

                    if (config.isSendIssuer()) {
                        AbstractMetricsHandler.addIssuerRegex(tags, auditInfo);
                    }

                    MetricName metricName = new MetricName("response_time");
                    metricName = metricName.tagged(commonTags);
                    metricName = metricName.tagged(tags);
                    long time = Clock.defaultClock().getTick() - startTime;

                    registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.TIMERS).update(time, TimeUnit.NANOSECONDS);

                    if (logger.isTraceEnabled())
                        logger.trace("metricName = {}, commonTags = {} tags = {}", metricName, JsonMapper.toJson(commonTags), JsonMapper.toJson(tags));

                    incrementCounter("request", commonTags, tags);
                    String metricType = AbstractMetricsHandler.getMetricTypeForStatusCode(exchange.getStatusCode());
                    if (metricType != null) {
                        incrementCounter(metricType, commonTags, tags);
                    }

                } else {
                    // when we reach here, it will be in light-gateway so no specification is loaded on the server and also the security verification is failed.
                    // we need to come up with the endpoint at last to ensure we have some meaningful metrics info populated.
                    logger.error("auditInfo is null or empty. Please move the path prefix handler to the top of the handler chain after metrics.");
                }
            } catch (Exception e) {
                logger.error("ExchangeListener throwable", e);
            } finally {
                nextListener.proceed();
            }
        }

        private static void incrementCounter(final String metricName, final Map<String, String> commonTags, final Map<String, String> tags) {
            MetricName metric = new MetricName(metricName).tagged(commonTags).tagged(tags);
            registry.getOrAdd(metric, MetricRegistry.MetricBuilder.COUNTERS).inc();
        }
    }
}
