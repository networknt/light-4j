/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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
import com.networknt.handler.MiddlewareHandler;
import com.networknt.metrics.MetricsConfig;
import com.networknt.server.Server;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.Util;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.SimpleTimer;
import io.prometheus.client.Summary;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MetricsHandler implements MiddlewareHandler {
    public static final String CONFIG_NAME = "metrics";
    public static MetricsConfig config;

    private CollectorRegistry registry;

    static final Logger logger = LoggerFactory.getLogger(MetricsHandler.class);

    private volatile HttpHandler next;
    Map<String, String> commonTags = new HashMap<>();
    private Counter requests, success, request_error, auth_error, server_error;

    static final Summary response_time = Summary.build().name("response_time_seconds").help("Response time in seconds.").labelNames("MetricsHandler").register();

    public MetricsHandler() {
        registry=  CollectorRegistry.defaultRegistry;

        commonTags.put("apiName", Server.config.getServiceId());
        InetAddress inetAddress = Util.getInetAddress();
        // On Docker for Mac, inetAddress will be null as there is a bug.
        commonTags.put("ipAddress", inetAddress == null ? "unknown" : inetAddress.getHostAddress());
        commonTags.put("hostname", inetAddress == null ? "unknown" : inetAddress.getHostName()); // will be container id if in docker.

        List<String> labels = new ArrayList<>(this.commonTags.keySet());
        List<String> labelValues = new ArrayList<>(this.commonTags.values());
        requests = Counter.build().name("requests_total").help("Total requests.").labelNames((String[])labels.toArray()).register();
        requests.labels((String[])labelValues.toArray());

        success = Counter.build().name("success_total").help("Total success requests.").labelNames((String[])labels.toArray()).register();
        success.labels((String[])labelValues.toArray());

        auth_error = Counter.build().name("auth_error_total").help("Total auth_error requests.").labelNames((String[])labels.toArray()).register();
        auth_error.labels((String[])labelValues.toArray());

        request_error = Counter.build().name("request_error_total").help("Total request error requests.").labelNames((String[])labels.toArray()).register();
        request_error.labels((String[])labelValues.toArray());

        server_error = Counter.build().name("server_error_total").help("Total server error requests.").labelNames((String[])labels.toArray()).register();
        server_error.labels((String[])labelValues.toArray());
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
            Map<String, Object> auditInfo = exchange1.getAttachment(AuditHandler.AUDIT_INFO);
            if(auditInfo != null) {


                Map<String, String> tags = new HashMap<>();
                tags.put("endpoint", (String)auditInfo.get(Constants.ENDPOINT_STRING));
                tags.put("clientId", auditInfo.get(Constants.CLIENT_ID_STRING) != null ? (String)auditInfo.get(Constants.CLIENT_ID_STRING) : "unknown");

                response_time.labels("respTimer").observe(respTimer.elapsedSeconds());

                incCounterForStatusCode(exchange1.getStatusCode(), commonTags, tags);
            }
            nextListener.proceed();
        });

        next.handleRequest(exchange);
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(MetricsHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }

    private void incCounterForStatusCode(int statusCode, Map<String, String> commonTags, Map<String, String> tags) {
        requests.inc();
        if(statusCode >= 200 && statusCode < 400) {
            success.inc();
        } else if(statusCode == 401 || statusCode == 403) {
            auth_error.inc();
        } else if(statusCode >= 400 && statusCode < 500) {
            request_error.inc();
        } else if(statusCode >= 500) {
            server_error.inc();
        }

    }

}
