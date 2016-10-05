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

package com.networknt.audit;

import com.networknt.config.Config;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a simple audit handler that dump most important info per request basis. The following
 * elements will be logged if it's available. This handle can be used on production for certain
 * applications that need audit on request. Turn off statusCode and responseTime can make it faster
 *
 * timestamp
 * correlationId
 * traceabilityId (if available)
 * clientId
 * userId (if available)
 * scopeClientId (available if called by an API)
 * endpoint (uriPattern@method)
 * statusCode
 * responseTime
 *
 * Created by steve on 17/09/16.
 */
public class SimpleAuditHandler implements MiddlewareHandler {
    public static final String CONFIG_NAME = "audit";
    public static final String ENABLE_SIMPLE_AUDIT = "enableSimpleAudit";

    static final String SIMPLE = "simple";
    static final String HEADERS = "headers";
    static final String STATUS_CODE = "statusCode";
    static final String RESPONSE_TIME = "responseTime";
    static final String TIMESTAMPT = "timestamp";

    public static Map<String, Object> config;
    private static List<String> headerList;
    private static boolean statusCode = false;
    private static boolean responseTime = false;

    static final Logger audit = LoggerFactory.getLogger(Constants.AUDIT_LOGGER);

    private volatile HttpHandler next;

    static {
        config = Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME);
        Map<String, Object> simpleMap = (Map<String, Object>)config.get(SIMPLE);
        headerList = (List<String>)simpleMap.get(HEADERS);
        Object object = simpleMap.get(STATUS_CODE);
        if(object != null && (Boolean)object == true) {
            statusCode = true;
        }
        object = simpleMap.get(RESPONSE_TIME);
        if(object != null && (Boolean)object == true) {
            responseTime = true;
        }
    }

    public SimpleAuditHandler() {

    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        final long start = System.currentTimeMillis();
        Map<String, Object> auditMap = new LinkedHashMap<>();
        auditMap.put(TIMESTAMPT, System.currentTimeMillis());
        // dump headers according to config
        if(headerList != null && headerList.size() > 0) {
            for(String name: headerList) {
                auditMap.put(name, exchange.getRequestHeaders().getFirst(name));
            }
        }
        if(statusCode || responseTime) {
            exchange.addExchangeCompleteListener(new ExchangeCompletionListener() {
                @Override
                public void exchangeEvent(final HttpServerExchange exchange, final NextListener nextListener) {
                    if(statusCode) {
                        auditMap.put(STATUS_CODE, exchange.getStatusCode());
                    }
                    if(responseTime) {
                        auditMap.put(RESPONSE_TIME, new Long(System.currentTimeMillis() - start));
                    }
                    nextListener.proceed();
                }
            });
        }
        audit.info(Config.getInstance().getMapper().writeValueAsString(auditMap));
        next.handleRequest(exchange);
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
    public boolean enabled() {
        Object object = config.get(ENABLE_SIMPLE_AUDIT);
        return object != null && (Boolean)object == true;
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(SimpleAuditHandler.class.getName(), config, null);
    }
}
