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

package com.networknt.metrics;

import com.networknt.config.Config;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.swagger.SwaggerHandler;
import com.networknt.swagger.SwaggerHelper;
import com.networknt.swagger.SwaggerOperation;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.Util;
import io.dropwizard.metrics.Clock;
import io.dropwizard.metrics.MetricFilter;
import io.dropwizard.metrics.MetricName;
import io.dropwizard.metrics.MetricRegistry;
import io.dropwizard.metrics.influxdb.InfluxDbHttpSender;
import io.dropwizard.metrics.influxdb.InfluxDbReporter;
import io.dropwizard.metrics.influxdb.InfluxDbSender;
import io.undertow.Handlers;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by steve on 03/10/16.
 */
public class MetricsHandler implements MiddlewareHandler {
    public static final String CONFIG_NAME = "metrics";
    public static MetricsConfig config;

    static final MetricRegistry registry = new MetricRegistry();

    static final Logger logger = LoggerFactory.getLogger(MetricsHandler.class);

    static {
        config = (MetricsConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, MetricsConfig.class);
        // initialize reporter and start the report scheduler.
        try {
            InfluxDbSender influxDb =
                    new InfluxDbHttpSender(config.influxdbProtocol, config.influxdbHost, config.influxdbPort,
                            config.influxdbName, config.influxdbUser, config.influxdbPass);
            InfluxDbReporter reporter = InfluxDbReporter
                    .forRegistry(registry)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .filter(MetricFilter.ALL)
                    .build(influxDb);
            reporter.start(config.getReportInMinutes(), TimeUnit.MINUTES);
        } catch (Exception e) {
            // if there are any exception, chances are influxdb is not available. disable this handler.
            logger.warn("metrics is disabled as it cannot connect to the influxdb");
            config.setEnabled(false);
        }
    }

    private volatile HttpHandler next;
    Map<String, String> commonTags = new HashMap<>();

    public MetricsHandler() {
        commonTags.put("apiName", SwaggerHelper.swagger.getInfo().getTitle().replaceAll(" ", "_").toLowerCase());
        InetAddress inetAddress = Util.getInetAddress();
        commonTags.put("ipAddress", inetAddress.getHostAddress());
        commonTags.put("hostname", inetAddress.getHostName()); // will be container id if in docker.
        commonTags.put("version", Util.getJarVersion());

        //commonTags.put("frameworkVersion", Util.getFrameworkVersion());
        // TODO need to find a way to get env to put into the metrics.
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
        logger.debug("in default metrics handler");

        long startTime = Clock.defaultClock().getTick();

        exchange.addExchangeCompleteListener((exchange1, nextListener) -> {
            SwaggerOperation swaggerOperation = exchange1.getAttachment(SwaggerHandler.SWAGGER_OPERATION);
            if(swaggerOperation != null) {
                Map<String, String> tags = new HashMap<String, String>();
                tags.put("endpoint", swaggerOperation.getEndpoint());
                tags.put("clientId", swaggerOperation.getClientId());

                long time = Clock.defaultClock().getTick() - startTime;
                MetricName metricName = new MetricName("response_time");
                metricName = metricName.tagged(commonTags);
                metricName = metricName.tagged(tags);
                registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.TIMERS).update(time, TimeUnit.NANOSECONDS);
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

}
