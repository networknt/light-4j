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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This is a simple audit handler that dump most important info per request basis. The following
 * elements will be logged if it's available in auditInfo object attached to exchange. This object
 * wil be populated by other upstream handlers like swagger-meta and swagger-security for
 * light-rest-4j framework.
 *
 * This handler can be used on production but be aware that it will impact the overall performance.
 * Turning off statusCode and responseTime can make it faster as these have to be captured on the
 * response chain instead of request chain.
 *
 * For most business and majority of microservices, you don't need to enable this handler due to
 * performance reason. The default audit log will be audit.log config in the default logback.xml;
 * however, it can be changed to syslog or Kafka with customized appender.
 *
 * Majority of the fields in audit log are collected in request and response; however, to allow
 * user to customize it, we have put an attachment into the exchange to allow other handlers to
 * write important info into it. The audit.yml can control which fields should be included in the
 * final log.
 *
 * By default, the following fields are included:
 *
 * timestamp
 * serviceId (from server.yml)
 * correlationId
 * traceabilityId (if available)
 * clientId
 * userId (if available)
 * scopeClientId (available if called by another API)
 * endpoint (uriPattern@method)
 * statusCode
 * responseTime
 *
 * Created by steve on 17/09/16.
 */
public class AuditHandler implements MiddlewareHandler {
    public static final String CONFIG_NAME = "audit";

    public static final String ENABLED = "enabled";
    static final String HEADERS = "headers";
    static final String AUDIT = "audit";
    static final String STATUS_CODE = "statusCode";
    static final String RESPONSE_TIME = "responseTime";
    static final String TIMESTAMP = "timestamp";
    static final String AUDIT_ON_ERROR = "auditOnError";
    static final String IS_LOG_LEVEL_ERROR = "logLevelIsError";

    public static final Map<String, Object> config;
    private static final List<String> headerList;
    private static final List<String> auditList;

    private static boolean statusCode = false;
    private static boolean responseTime = false;
    private static boolean auditOnError = false;

    // A customized logger appender defined in default logback.xml
    static Consumer<String> auditFunc = LoggerFactory.getLogger(Constants.AUDIT_LOGGER)::info;

    // The key to the audit info attachment in exchange. Allow other handlers to set values.
    public static final AttachmentKey<Map> AUDIT_INFO = AttachmentKey.create(Map.class);

    private volatile HttpHandler next;

    static {
        config = Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME);
        headerList = (List<String>)config.get(HEADERS);
        auditList = (List<String>)config.get(AUDIT);
        Object object = config.get(STATUS_CODE);
        if(object != null && (Boolean) object) {
            statusCode = true;
        }
        object = config.get(RESPONSE_TIME);
        if(object != null && (Boolean) object) {
            responseTime = true;
        }

        // audit on error response flag
        object = config.get(AUDIT_ON_ERROR);
        if(object != null && (Boolean) object) {
            auditOnError = true;
        }

        // set the log level
        object = config.get(IS_LOG_LEVEL_ERROR);
        auditFunc = (object != null && (Boolean) object) ?
            LoggerFactory.getLogger(Constants.AUDIT_LOGGER)::error : LoggerFactory.getLogger(Constants.AUDIT_LOGGER)::info;

    }

    public AuditHandler() {

    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        Map<String, Object> auditInfo = exchange.getAttachment(AuditHandler.AUDIT_INFO);
        Map<String, Object> auditMap = new LinkedHashMap<>();
        final long start = System.currentTimeMillis();
        auditMap.put(TIMESTAMP, System.currentTimeMillis());
        // dump audit info fields according to config
        if(auditInfo != null) {
            if(auditList != null && auditList.size() > 0) {
                for(String name: auditList) {
                    auditMap.put(name, auditInfo.get(name));
                }
            }
        }
        // dump headers field according to config
        if(headerList != null && headerList.size() > 0) {
            for(String name: headerList) {
                auditMap.put(name, exchange.getRequestHeaders().getFirst(name));
            }
        }
        if(statusCode || responseTime) {
            exchange.addExchangeCompleteListener((exchange1, nextListener) -> {
                if (AuditHandler.statusCode) {
                    auditMap.put(STATUS_CODE, exchange1.getStatusCode());
                }
                if (responseTime) {
                    auditMap.put(RESPONSE_TIME, System.currentTimeMillis() - start);
                }

                // add additional fields accumulated during the microservice execution
                // according to the config
                //Map<String, Object> auditInfo1 = exchange.getAttachment(AuditHandler.AUDIT_INFO);
                if(auditInfo != null) {
                    if(auditList != null && auditList.size() > 0) {
                        for(String name: auditList) {
                            auditMap.putIfAbsent(name, auditInfo.get(name));
                        }
                    }
                }

                try {
                    // audit entries only is it is an error, if auditOnError flag is set
                    if(auditOnError) {
                        if (exchange1.getStatusCode() >= 400)
                            auditFunc.accept(Config.getInstance().getMapper().writeValueAsString(auditMap));
                    } else {
                        auditFunc.accept(Config.getInstance().getMapper().writeValueAsString(auditMap));
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                nextListener.proceed();
            });
        } else {
            auditFunc.accept(Config.getInstance().getMapper().writeValueAsString(auditMap));
        }
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
        Object object = config.get(ENABLED);
        return object != null && (Boolean) object;
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(AuditHandler.class.getName(), config, null);
    }
}
