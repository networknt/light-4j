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

package com.networknt.correlation;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.Util;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * This is a handler that checks if X-Correlation-Id exists in request header and put it into
 * request header if it doesn't exist.
 *
 * The correlation-id is set by the first API/service and it will be passed to all services. Every logging
 * statement in the server should have correlationId logged so that this id can link all the logs across
 * services in ELK or other logging aggregation application.
 *
 * Dependencies: SimpleAuditHandler, Client
 *
 * Created by steve on 05/11/16.
 */
public class CorrelationHandler implements MiddlewareHandler {
    private static final Logger logger = LoggerFactory.getLogger(CorrelationHandler.class);
    private static final String CID = "cId";
    private static final String CONFIG_NAME = "correlation";

    public static CorrelationConfig config =
            (CorrelationConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, CorrelationConfig.class);

    private volatile HttpHandler next;

    public CorrelationHandler() {
        if(logger.isInfoEnabled()) logger.info("CorrelationHandler is loaded.");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        // check if the cid is in the request header
        String cId = exchange.getRequestHeaders().getFirst(Constants.CORRELATION_ID);
        if(cId == null) {
            // if not, generate a UUID and put it into the request header
            cId = Util.getUUID();
            exchange.getRequestHeaders().put(Constants.CORRELATION_ID, cId);
        }
        // Add the cId into MDC so that all log statement will have cId as part of it.
        MDC.put(CID, cId);
        //logger.debug("Init cId:" + cId);
        Handler.next(exchange, next);
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
        ModuleRegistry.registerModule(CorrelationHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }

}
