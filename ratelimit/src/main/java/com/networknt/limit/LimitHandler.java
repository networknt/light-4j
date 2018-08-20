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

package com.networknt.limit;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.RequestLimit;

/**
 * A handler which limits the maximum number of concurrent requests.  Requests beyond the limit will
 * be queued with limited size of queue. If the queue is full, then request will be dropped.
 *
 * @author Steve Hu
 */
public class LimitHandler implements MiddlewareHandler {
    private static final String CONFIG_NAME = "limit";

    public static LimitConfig config =
            (LimitConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, LimitConfig.class);

    private volatile HttpHandler next;
    private final RequestLimit requestLimit;

    public LimitHandler() {
        this.requestLimit = new RequestLimit(config.concurrentRequest, config.queueSize);
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        requestLimit.handleRequest(exchange, Handler.getNext(exchange, next));
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(final HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(LimitHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }
}
