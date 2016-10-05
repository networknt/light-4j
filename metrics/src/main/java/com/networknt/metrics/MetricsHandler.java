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
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by steve on 03/10/16.
 */
public class MetricsHandler implements MiddlewareHandler {
    public static final String CONFIG_NAME = "metrics";

    public static MetricsConfig config;

    static final Logger logger = LoggerFactory.getLogger(MetricsHandler.class);

    static {
        config = (MetricsConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, MetricsConfig.class);
    }

    private volatile HttpHandler next;

    public MetricsHandler() {}

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
        next.handleRequest(exchange);
    }

    @Override
    public boolean enabled() {
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(MetricsHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }

}
