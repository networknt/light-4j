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

package com.networknt.metrics.prometheus;

import com.networknt.audit.AuditHandler;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.SimpleTimer;
import io.prometheus.client.Summary;
import io.prometheus.client.hotspot.DefaultExports;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Metrics middleware handler can be plugged into the request/response chain to
 * capture metrics information for all services. This is systems monitoring middleware handler
 * to integrated with Prometheus
 *
 */
public class PrometheusHandler implements MiddlewareHandler {
    public static final String CONFIG_NAME = "prometheus";
    public static PrometheusConfig config =(PrometheusConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, PrometheusConfig.class);

    private CollectorRegistry registry;

    static final Logger logger = LoggerFactory.getLogger(PrometheusHandler.class);

    private volatile HttpHandler next;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Summary> response_times = new ConcurrentHashMap<>();

    public static final String REQUEST_TOTAL = "requests_total";
    public static final String SUCCESS_TOTAL = "success_total";
    public static final String AUTO_ERROR_TOTAL = "auth_error_total";
    public static final String REQUEST_ERROR_TOTAL = "request_error_total";
    public static final String SERVER_ERROR_TOTAL = "server_error_total";
    public static final String RESPONSE_TIME_SECOND = "response_time_seconds";



    public PrometheusHandler() {
        registry=  CollectorRegistry.defaultRegistry;
    }

    @Override
    public HttpHandler getNext() {
        return this.next;
    }

    @Override
    public MiddlewareHandler setNext(final HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;

    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        SimpleTimer respTimer = new SimpleTimer();

        exchange.addExchangeCompleteListener((exchange1, nextListener) -> {
            Map<String, Object> auditInfo = exchange1.getAttachment(AttachmentConstants.AUDIT_INFO);
            if(auditInfo != null) {
                Map<String, String> tags = new HashMap<>();
                tags.put("endpoint", (String)auditInfo.get(Constants.ENDPOINT_STRING));
                tags.put("clientId", auditInfo.get(Constants.CLIENT_ID_STRING) != null ? (String)auditInfo.get(Constants.CLIENT_ID_STRING) : "unknown");

                List<String> labels = new ArrayList<>(tags.keySet());
                List<String> labelValues = new ArrayList<>(tags.values());

                summary(RESPONSE_TIME_SECOND, labels).labels(labelValues.stream().toArray(String[]::new)).observe(respTimer.elapsedSeconds());

                incCounterForStatusCode(exchange1.getStatusCode(), labels, labelValues);
                if (config.enableHotspot) {
                    logger.info("Prometheus hotspot monitor enabled.");
                    DefaultExports.initialize();
                }
            }
            nextListener.proceed();
        });

        Handler.next(exchange, next);
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(PrometheusHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }

    private void incCounterForStatusCode(int statusCode, List<String> labels,  List<String> labelValues) {

        counter(REQUEST_TOTAL, labels).labels(labelValues.stream().toArray(String[]::new)).inc();
        if(statusCode >= 200 && statusCode < 400) {
            counter(SUCCESS_TOTAL , labels).labels(labelValues.stream().toArray(String[]::new)).inc();
        } else if(statusCode == 401 || statusCode == 403) {
            counter(AUTO_ERROR_TOTAL , labels).labels(labelValues.stream().toArray(String[]::new)).inc();
        } else if(statusCode >= 400 && statusCode < 500) {
            counter(REQUEST_ERROR_TOTAL , labels).labels(labelValues.stream().toArray(String[]::new)).inc();
        } else if(statusCode >= 500) {
            counter(SERVER_ERROR_TOTAL , labels).labels(labelValues.stream().toArray(String[]::new)).inc();
        }

    }

    private Counter counter(String name, List<String> labels) {
        String key = sanitizeName(name);
        return counters.computeIfAbsent(key, k-> Counter.build().name(k).help(k).labelNames(labels.stream().toArray(String[]::new)).register(registry));
    }

    private Summary summary(String name, List<String> labels) {
        String key = sanitizeName(name);
        return response_times.computeIfAbsent(key, k-> Summary.build().name(k).help(k).labelNames(labels.stream().toArray(String[]::new)).register(registry));
    }

    private String sanitizeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_:]", "_");
    }
}
