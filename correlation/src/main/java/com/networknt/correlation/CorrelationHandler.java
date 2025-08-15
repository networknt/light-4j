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
import com.networknt.server.ServerConfig;
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

    public static CorrelationConfig config;

    private volatile HttpHandler next;

    private final static String SERVICE_ID = "sId";

    public CorrelationHandler() {
        config = CorrelationConfig.load();
        logger.info("CorrelationHandler is loaded.");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        // Ensure the current thread is a worker thread, otherwise the MDC variables set after this point will be lost due to thread switching.
        if(exchange.isInIoThread()) {
            exchange.dispatch(this);
        }

        logger.debug("CorrelationHandler.handleRequest starts.");

        // Add serviceId into MDC so that all log statement will have serviceId as part of it.
        // Compensates for the previous approach of configuring sId in logback.xml with values.xml, which had the defect that sId was not available in the current thread when correlationId was created in the filter chain.
        ServerConfig serverConfig = ServerConfig.getInstance();
        if (serverConfig != null && serverConfig.getServiceId() != null) {
            String serviceId = serverConfig.getServiceId();
            String environment = serverConfig.getEnvironment();
            String serviceKey = environment == null ? serviceId : serviceId + "|" + environment;
            MDC.put(SERVICE_ID, serviceKey);
        } else {
            MDC.put(SERVICE_ID, "UNDEFINED");
        }

        // Remove MDC variables when the exchange is completed, otherwise the thread variables may be reused and carry over to other requests.
        exchange.addExchangeCompleteListener((exchange1, nextListener) -> {
            try {
                MDC.remove(SERVICE_ID);
                MDC.remove(config.getCorrelationMdcField());
                MDC.remove(config.getTraceabilityMdcField());
                logger.debug("CorrelationHandler.handleRequest cleaned up thread MDC variables [{}, {}, {}].", SERVICE_ID, config.getCorrelationMdcField(), config.getTraceabilityMdcField());
            } finally {
                nextListener.proceed();
            }
        });

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
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(CorrelationConfig.CONFIG_NAME, CorrelationHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CorrelationConfig.CONFIG_NAME), null);
    }

    @Override
    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(CorrelationConfig.CONFIG_NAME, CorrelationHandler.class.getName(), config.getMappedConfig(), null);
        logger.info("CorrelationHandler is enabled.");
    }
}
