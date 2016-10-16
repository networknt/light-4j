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
import com.networknt.utility.ModuleRegistry;
import io.dropwizard.metrics.Clock;
import io.dropwizard.metrics.MetricFilter;
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

import java.util.concurrent.TimeUnit;

import static io.dropwizard.metrics.MetricRegistry.name;

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
                    new InfluxDbHttpSender(config.influxdbHost, config.influxdbPort,
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

    public MetricsHandler() {

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

        exchange.addExchangeCompleteListener(new ExchangeCompletionListener() {
            @Override
            public void exchangeEvent(HttpServerExchange exchange, ExchangeCompletionListener.NextListener nextListener) {
                long time = Clock.defaultClock().getTick() - startTime;
                registry.getOrAdd(name("response_time"), MetricRegistry.MetricBuilder.TIMERS).update(time, TimeUnit.NANOSECONDS);
                nextListener.proceed();
            }
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

}
