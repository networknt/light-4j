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

import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.utility.Constants;
import io.dropwizard.metrics.MetricFilter;
import io.dropwizard.metrics.MetricName;
import io.dropwizard.metrics.MetricRegistry;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metrics middleware handler can be plugged into the request/response chain to capture metrics information
 * for all services. It is based on the dropwizard metric but customized to capture statistic info for a
 * period of time and then reset all the data in order to capture the next period. The capture period can
 * be configured in metrics.yml and normally should be 5 minutes, 10 minutes or 20 minutes depending on the
 * load of the service.
 *
 * This is the generic implementation and all others will be extended from this handler. This handler will
 * be used by others to inject metrics info if enabled.
 *
 * @author Steve Hu
 */
public abstract class AbstractMetricsHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(AbstractMetricsHandler.class);
    // The metrics.yml configuration that supports reload.
    public static MetricsConfig config;
    static Pattern pattern;
    // The structure that collect all the metrics entries. Even others will be using this structure to inject.
    public static final MetricRegistry registry = new MetricRegistry();
    public Map<String, String> commonTags = new HashMap<>();

    public AbstractMetricsHandler() {
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    public void createJVMMetricsReporter(final TimeSeriesDbSender sender) {
        JVMMetricsDbReporter jvmReporter = new JVMMetricsDbReporter(new MetricRegistry(), sender, "jvm-reporter",
                MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.MILLISECONDS, commonTags);
        jvmReporter.start(config.getReportInMinutes(), TimeUnit.MINUTES);
    }

    public void incCounterForStatusCode(int statusCode, Map<String, String> commonTags, Map<String, String> tags) {
        MetricName metricName = new MetricName("request").tagged(commonTags).tagged(tags);
        registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.COUNTERS).inc();
        if (statusCode >= 200 && statusCode < 400) {
            metricName = new MetricName("success").tagged(commonTags).tagged(tags);
            registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.COUNTERS).inc();
        } else if (statusCode == 401 || statusCode == 403) {
            metricName = new MetricName("auth_error").tagged(commonTags).tagged(tags);
            registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.COUNTERS).inc();
        } else if (statusCode >= 400 && statusCode < 500) {
            metricName = new MetricName("request_error").tagged(commonTags).tagged(tags);
            registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.COUNTERS).inc();
        } else if (statusCode >= 500) {
            metricName = new MetricName("server_error").tagged(commonTags).tagged(tags);
            registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.COUNTERS).inc();
        }
    }

    /**
     * This is the method that is used for all other handlers to inject its metrics info to the real metrics handler impl.
     *
     * @param httpServerExchange the HttpServerExchange that is used to get the auditInfo to collect the metrics tag.
     * @param startTime          the start time passed in to calculate the response time.
     * @param metricsName        the name of the metrics that is collected.
     */
    public void injectMetrics(HttpServerExchange httpServerExchange, long startTime, String metricsName) {
        Map<String, Object> auditInfo = httpServerExchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        if (auditInfo != null) {
            Map<String, String> tags = new HashMap<>();
            tags.put("endpoint", (String) auditInfo.get(Constants.ENDPOINT_STRING));
            tags.put("clientId", auditInfo.get(Constants.CLIENT_ID_STRING) != null ? (String) auditInfo.get(Constants.CLIENT_ID_STRING) : "unknown");
            // scope client id will only be available if two token is used. For example, authorization code flow.
            if (config.isSendScopeClientId()) {
                tags.put("scopeClientId", auditInfo.get(Constants.SCOPE_CLIENT_ID_STRING) != null ? (String) auditInfo.get(Constants.SCOPE_CLIENT_ID_STRING) : "unknown");
            }
            // caller id is the calling serviceId that is passed from the caller. It is not always available but some organizations enforce it.
            if (config.isSendCallerId()) {
                tags.put("callerId", auditInfo.get(Constants.CALLER_ID_STRING) != null ? (String) auditInfo.get(Constants.CALLER_ID_STRING) : "unknown");
            }
            if (config.isSendIssuer()) {
                String issuer = (String) auditInfo.get(Constants.ISSUER_CLAIMS);
                if (issuer != null) {
                    // we need to send issuer as a tag. Do we need to apply regex to extract only a part of the issuer?
                    if(config.getIssuerRegex() != null) {
                        Matcher matcher = pattern.matcher(issuer);
                        if (matcher.find()) {
                            String iss = matcher.group(1);
                            if(logger.isTraceEnabled()) logger.trace("Extracted issuer {} from Original issuer {] is sent.", iss, issuer);
                            tags.put("issuer", iss != null ? iss : "unknown");
                        }
                    } else {
                        if(logger.isTraceEnabled()) logger.trace("Original issuer {} is sent.", issuer);
                        tags.put("issuer", issuer);
                    }
                }
            }
            MetricName metricName = new MetricName(metricsName);
            metricName = metricName.tagged(commonTags);
            metricName = metricName.tagged(tags);
            long time = System.nanoTime() - startTime;
            registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.TIMERS).update(time, TimeUnit.NANOSECONDS);
            incCounterForStatusCode(httpServerExchange.getStatusCode(), commonTags, tags);
        }
    }
}
