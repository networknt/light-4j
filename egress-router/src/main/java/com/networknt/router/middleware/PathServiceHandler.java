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

package com.networknt.router.middleware;

import com.networknt.httpstring.AttachmentConstants;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * When using router, each request must have serviceId in the header in order to allow router
 * to do the service discovery before invoke downstream service. The reason we have to do that
 * is due to the unpredictable path between services. If you are sure that all the downstream
 * services can be identified by the path, then you can use this Path to ServiceId mapper handler
 * to uniquely identify the serviceId and put it into the header. In this case, the client can
 * invoke the service just the same way it is invoking the service directly.
 *
 * Please note that you cannot invoke /health or /server/info endpoints as these are the common
 * endpoints injected by the framework and all services will have them on the same path. The
 * router cannot figure out which service you want to invoke so an error message will be returned
 *
 * This handler depends on OpenAPIHandler or SwaggerHandler in light-rest-4j framework. That means
 * this handler only works with RESTful APIs. In rest swagger-meta or openapi-meta, the endpoint
 * of each request is saved into auditInfo object which is attached to the exchange for auditing.
 *
 * This service mapper handler is very similar to the ServiceDictHandler but this one is using
 * the light-rest-4j endpoint in the auditInfo object to do the mapping and the other one is
 * doing the path and method pattern to do the mapping.
 *
 * @author Steve Hu
 *
 */
public class PathServiceHandler implements MiddlewareHandler {
    static Logger logger = LoggerFactory.getLogger(PathServiceHandler.class);
    private volatile HttpHandler next;
    private static PathServiceConfig config;
    public PathServiceHandler() {
        logger.info("PathServiceHandler is constructed");
        config = PathServiceConfig.load();
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("PathServiceConfig.handleRequest starts.");
        // if service URL is in the header, we don't need to do the service discovery with serviceId.
        HeaderValues serviceIdHeader = exchange.getRequestHeaders().get(HttpStringConstants.SERVICE_ID);
        String serviceId = serviceIdHeader != null ? serviceIdHeader.peekFirst() : null;
        if(serviceId == null) {
            Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
            if (auditInfo != null) {
                String endpoint = (String) auditInfo.get(Constants.ENDPOINT_STRING);
                if (logger.isDebugEnabled()) logger.debug("endpoint = " + endpoint);
                // now find the mapped serviceId from the mapping.
                if (endpoint != null) {
                    serviceId = config.getMapping() == null ? null : config.getMapping().get(endpoint);
                    if(serviceId != null) {
                        if(logger.isTraceEnabled()) logger.trace("Put into the service_id header for serviceId = " + serviceId);
                        exchange.getRequestHeaders().put(HttpStringConstants.SERVICE_ID, serviceId);
                    } else {
                        if(logger.isDebugEnabled()) logger.debug("The endpoint is not in the mapping config");
                    }
                } else {
                    logger.error("could not get endpoint from the auditInfo.");
                }
            } else {
                // couldn't find auditInfo object in exchange attachment.
                logger.error("could not find auditInfo object in exchange attachment.");
            }
        }
        if(logger.isDebugEnabled()) logger.debug("PathServiceConfig.handleRequest ends.");
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
        ModuleRegistry.registerModule(PathServiceHandler.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(PathServiceHandler.class.getName(), config.getMappedConfig(), null);
    }
}
