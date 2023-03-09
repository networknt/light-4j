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
        if(statusCode >= 200 && statusCode < 400) {
            metricName = new MetricName("success").tagged(commonTags).tagged(tags);
            registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.COUNTERS).inc();
        } else if(statusCode == 401 || statusCode == 403) {
            metricName = new MetricName("auth_error").tagged(commonTags).tagged(tags);
            registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.COUNTERS).inc();
        } else if(statusCode >= 400 && statusCode < 500) {
            metricName = new MetricName("request_error").tagged(commonTags).tagged(tags);
            registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.COUNTERS).inc();
        } else if(statusCode >= 500) {
            metricName = new MetricName("server_error").tagged(commonTags).tagged(tags);
            registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.COUNTERS).inc();
        }
    }

    public void injectMetrics(HttpServerExchange httpServerExchange, long startTime) {
        Map<String, Object> auditInfo = httpServerExchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        if (auditInfo != null) {
            Map<String, String> tags = new HashMap<>();
            tags.put("endpoint", (String)auditInfo.get(Constants.ENDPOINT_STRING));
            tags.put("clientId", auditInfo.get(Constants.CLIENT_ID_STRING) != null ? (String)auditInfo.get(Constants.CLIENT_ID_STRING) : "unknown");
            tags.put("scopeClientId", auditInfo.get(Constants.SCOPE_CLIENT_ID_STRING) != null ? (String)auditInfo.get(Constants.SCOPE_CLIENT_ID_STRING) : "unknown");
            tags.put("callerId", auditInfo.get(Constants.CALLER_ID_STRING) != null ? (String)auditInfo.get(Constants.CALLER_ID_STRING) : "unknown");
            MetricName metricName = new MetricName("api_response_time");
            metricName = metricName.tagged(commonTags);
            metricName = metricName.tagged(tags);
            long time = System.nanoTime() - startTime;
            registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.TIMERS).update(time, TimeUnit.NANOSECONDS);
            incCounterForStatusCode(httpServerExchange.getStatusCode(), commonTags, tags);
        }
    }

}
