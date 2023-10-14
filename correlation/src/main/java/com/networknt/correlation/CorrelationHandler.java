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

package com.networknt.correlation;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.Util;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;

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

    public static CorrelationConfig config;

    private volatile HttpHandler next;

    public CorrelationHandler() {
        config = CorrelationConfig.load();
        if(logger.isInfoEnabled()) logger.info("CorrelationHandler is loaded.");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("CorrelationHandler.handleRequest starts.");
        // check if the cid is in the request header
        String cId = exchange.getRequestHeaders().getFirst(HttpStringConstants.CORRELATION_ID);
        if(cId == null) {
        	// if not set, check the autogen flag and generate if set to true
        	if(config.isAutogenCorrelationID()) {
	            // generate a UUID and put it into the request header
	            cId = Util.getUUID();
	            exchange.getRequestHeaders().put(HttpStringConstants.CORRELATION_ID, cId);
	            String tId = exchange.getRequestHeaders().getFirst(HttpStringConstants.TRACEABILITY_ID);
	            if(tId != null && logger.isInfoEnabled()) {
	                logger.info("Associate traceability Id " + tId + " with correlation Id " + cId);
                }
        	} 
        }
        // Add the cId into MDC so that all log statement will have cId as part of it.
        MDC.put(CID, cId);

        if (cId != null) {
            this.addHandlerMDCContext(exchange, CID, cId);
        }

        // This is usually the first handler in the request/response chain, log all the request headers here for diagnostic purpose.
        if(logger.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            for (HeaderValues header : exchange.getRequestHeaders()) {
                for (String value : header) {
                    sb.append(header.getHeaderName()).append("=").append(value).append("\n");
                }
            }
            logger.trace("Request Headers: " + sb);
        }

        if(logger.isDebugEnabled()) logger.debug("CorrelationHandler.handleRequest ends.");
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
        ModuleRegistry.registerModule(CorrelationHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CorrelationConfig.CONFIG_NAME), null);
    }

    @Override
    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(CorrelationHandler.class.getName(), config.getMappedConfig(), null);
        if(logger.isInfoEnabled()) {
            logger.info("CorrelationHandler is enabled.");
        }
    }
}
