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

package com.networknt.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.mask.Mask;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

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
    static final Logger logger = LoggerFactory.getLogger(AuditHandler.class);

    public static final String ENABLED = "enabled";
    static final String STATUS_CODE = "statusCode";
    static final String RESPONSE_TIME = "responseTime";
    static final String TIMESTAMP = "timestamp";
    static final String MASK_KEY = "audit";

    private AuditConfig auditConfig;

    private volatile HttpHandler next;

    public AuditHandler() {
        if(logger.isInfoEnabled()) logger.info("AuditHandler is loaded.");
        auditConfig = AuditConfig.load();
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        Map<String, Object> auditMap = new LinkedHashMap<>();
        final long start = System.currentTimeMillis();
        auditMap.put(TIMESTAMP, System.currentTimeMillis());

        // dump audit info fields according to config
        boolean needAuditData = auditInfo != null && auditConfig.hasAuditList();
        if(needAuditData) {
            auditFields(auditInfo, auditMap);
        }

        // dump headers field according to config
        if(auditConfig.hasHeaderList()) {
            auditHeader(exchange, auditMap);
        }

        if(auditConfig.isStatusCode() || auditConfig.isResponseTime()) {
            exchange.addExchangeCompleteListener((exchange1, nextListener) -> {
                if (auditConfig.isStatusCode()) {
                    auditMap.put(STATUS_CODE, exchange1.getStatusCode());
                }
                if (auditConfig.isResponseTime()) {
                    auditMap.put(RESPONSE_TIME, System.currentTimeMillis() - start);
                }

                // add additional fields accumulated during the microservice execution
                // according to the config
                //Map<String, Object> auditInfo1 = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
                if(auditInfo != null) {
                    if(auditConfig.getAuditList() != null && auditConfig.getAuditList().size() > 0) {
                        for(String name: auditConfig.getAuditList()) {
                            auditMap.putIfAbsent(name, auditInfo.get(name));
                        }
                    }
                }

                try {
                    // audit entries only is it is an error, if auditOnError flag is set
                    if(auditConfig.isAuditOnError()) {
                        if (exchange1.getStatusCode() >= 400)
                            auditConfig.getAuditFunc().accept(Config.getInstance().getMapper().writeValueAsString(auditMap));
                    } else {
                        auditConfig.getAuditFunc().accept(Config.getInstance().getMapper().writeValueAsString(auditMap));
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                nextListener.proceed();
            });
        } else {
            auditConfig.getAuditFunc().accept(auditConfig.getConfig().getMapper().writeValueAsString(auditMap));
        }
        next(exchange);
    }

    private void auditHeader(HttpServerExchange exchange, Map<String, Object> auditMap) {
        for(String name: auditConfig.getHeaderList()) {
            String value = exchange.getRequestHeaders().getFirst(name);
            auditMap.put(name, auditConfig.isMaskEnabled() ? Mask.maskRegex(value, "requestHeader", name) : value);
        }
    }

    protected void next(HttpServerExchange exchange) throws Exception {
        Handler.next(exchange, next);
    }

    private void auditFields(Map<String, Object> auditInfo, Map<String, Object> auditMap) {
        for(String name: auditConfig.getAuditList()) {
            Object value = auditInfo.get(name);
            boolean needApplyMask = auditConfig.isMaskEnabled() && value instanceof String;
            auditMap.put(name, needApplyMask ? Mask.maskRegex((String) value, MASK_KEY, name) : value);
        }
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
        Object object = auditConfig.getMappedConfig().get(ENABLED);
        return object != null && (Boolean) object;
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(AuditHandler.class.getName(), auditConfig.getMappedConfig(), null);
    }
}
