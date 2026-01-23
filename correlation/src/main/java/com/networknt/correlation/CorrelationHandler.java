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
import com.networknt.utility.UuidUtil;
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
 * <p>
 * The correlation-id is set by the first API/service and it will be passed to all services. Every logging
 * statement in the server should have correlationId logged so that this id can link all the logs across
 * services in ELK or other logging aggregation application.
 * <p>
 * Dependencies: SimpleAuditHandler, Client
 * <p>
 * Created by steve on 05/11/16.
 */
public class CorrelationHandler implements MiddlewareHandler {
    private static final Logger logger = LoggerFactory.getLogger(CorrelationHandler.class);



    private volatile HttpHandler next;

    public CorrelationHandler() {
        logger.info("CorrelationHandler is loaded.");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        CorrelationConfig config = CorrelationConfig.load();
        logger.debug("CorrelationHandler.handleRequest starts.");

        var tid = exchange.getRequestHeaders().getFirst(HttpStringConstants.TRACEABILITY_ID);

        if (tid != null) {
            exchange.getResponseHeaders().put(HttpStringConstants.TRACEABILITY_ID, tid);
            this.addHandlerMDCContext(exchange, config.getTraceabilityMdcField(), tid);
            MDC.put(config.getTraceabilityMdcField(), tid);

        } else if (MDC.get(config.traceabilityMdcField) != null) {
            MDC.remove(config.getTraceabilityMdcField());
        }

        // check if the cid is in the request header
        var cId = exchange.getRequestHeaders().getFirst(HttpStringConstants.CORRELATION_ID);

        if (cId == null && (config.isAutogenCorrelationID())) {

            // generate a UUID and put it into the request header
            cId = UuidUtil.uuidToBase64(UuidUtil.getUUID());
            exchange.getRequestHeaders().put(HttpStringConstants.CORRELATION_ID, cId);
            String tId = exchange.getRequestHeaders().getFirst(HttpStringConstants.TRACEABILITY_ID);

            if (tId != null)
                logger.info("Associate traceability Id {} with correlation Id {}", tId, cId);

        }

        // Add the cId into MDC so that all log statement will have cId as part of it.
        MDC.put(config.getCorrelationMdcField(), cId);

        if (cId != null)
            this.addHandlerMDCContext(exchange, config.getCorrelationMdcField(), cId);


        // This is usually the first handler in the request/response chain, log all the request headers here for diagnostic purpose.
        if (logger.isTraceEnabled()) {
            var sb = new StringBuilder();

            for (HeaderValues header : exchange.getRequestHeaders())
                for (String value : header)
                    sb.append(header.getHeaderName()).append("=").append(value).append("\n");

            logger.trace("Request Headers: '{}'", sb);
        }

        logger.debug("CorrelationHandler.handleRequest ends.");

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
        return CorrelationConfig.load().isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(CorrelationConfig.CONFIG_NAME, CorrelationHandler.class.getName(), Config.getInstance().getJsonMapConfig(CorrelationConfig.CONFIG_NAME), null);
    }
}
