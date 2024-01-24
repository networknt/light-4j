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
import com.networknt.server.ServerConfig;
import com.networknt.status.Status;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.StringUtils;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * This is a simple audit handler that dump most important info per-request basis. The following
 * elements will be logged if it's available in auditInfo object attached to exchange. This object
 * wil be populated by other upstream handlers like swagger-meta and swagger-security for
 * light-rest-4j framework.
 *
 * This handler can be used on production but be aware that it will impact the overall performance.
 * Turning off statusCode and responseTime can make it faster as these have to be captured on the
 * response chain instead of request chain.
 *
 * For most business and the majority of microservices, you don't need to enable this handler due to
 * performance reason. The default audit log will be the audit.log configured in the default logback.xml;
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
    static final String STATUS_CODE = "statusCode";
    static final String RESPONSE_TIME = "responseTime";
    static final String TIMESTAMP = "timestamp";
    static final String MASK_KEY = "audit";
    static final String REQUEST_BODY_KEY = "requestBody";
    static final String RESPONSE_BODY_KEY = "responseBody";
    static final String QUERY_PARAMETERS_KEY = "queryParameters";
    static final String PATH_PARAMETERS_KEY = "pathParameters";
    static final String REQUEST_COOKIES_KEY = "requestCookies";
    static final String SERVICE_ID_KEY = "serviceId";
    static final String INVALID_CONFIG_VALUE_CODE = "ERR10060";

    private static AuditConfig config;

    private volatile HttpHandler next;

    private String serviceId;

    private DateTimeFormatter DATE_TIME_FORMATTER;

    public AuditHandler() {
        if (logger.isInfoEnabled()) logger.info("AuditHandler is loaded.");
        config = AuditConfig.load();
        ServerConfig serverConfig = ServerConfig.getInstance();
        if (serverConfig != null) {
            serviceId = serverConfig.getServiceId();
        }
        String timestampFormat = config.getTimestampFormat();
        if (!StringUtils.isBlank(timestampFormat)) {
            try {
                DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(timestampFormat)
                        .withZone(ZoneId.systemDefault());
            } catch (IllegalArgumentException e) {
                logger.error(new Status(INVALID_CONFIG_VALUE_CODE, timestampFormat, "timestampFormat", "audit.yml").toString());

            }
        }
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("AuditHandler.handleRequest starts.");
        Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        Map<String, Object> auditMap = new LinkedHashMap<>();
        final long start = System.currentTimeMillis();

        // add audit timestamp
        auditMap.put(TIMESTAMP, DATE_TIME_FORMATTER == null ? System.currentTimeMillis() : DATE_TIME_FORMATTER.format(Instant.now()));

        // dump audit info fields according to config
        boolean needAuditData = auditInfo != null && config.hasAuditList();
        if (needAuditData) {
            auditFields(auditInfo, auditMap);
        }

        // dump request header, request body, path parameters, query parameters and request cookies according to config
        auditRequest(exchange, auditMap, config);

        // dump serviceId from server.yml
        if (config.hasAuditList() && config.getAuditList().contains(SERVICE_ID_KEY)) {
            auditServiceId(auditMap);
        }

        if (config.isStatusCode() || config.isResponseTime()) {
            exchange.addExchangeCompleteListener((exchange1, nextListener) -> {
                // response status code and response time.
                try {
                    if (config.isStatusCode()) {
                        auditMap.put(STATUS_CODE, exchange1.getStatusCode());
                    }
                    if (config.isResponseTime()) {
                        auditMap.put(RESPONSE_TIME, System.currentTimeMillis() - start);
                    }
                    // add additional fields accumulated during the microservice execution
                    // according to the config
                    Map<String, Object> auditInfo1 = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
                    if (auditInfo1 != null && config.getAuditList() != null) {
                        for (String name : config.getAuditList()) {
                            Object object = auditInfo1.get(name);
                            if(object != null) {
                                auditMap.put(name, object);
                            }
                        }
                    }
                    // audit the response body.
                    if(config.getAuditList() != null && config.getAuditList().contains(RESPONSE_BODY_KEY)) {
                        auditResponseBody(exchange, auditMap);
                    }

                    try {
                        // audit entries only is it is an error, if auditOnError flag is set
                        if (config.isAuditOnError()) {
                            if (exchange1.getStatusCode() >= 400)
                                config.getAuditFunc().accept(Config.getInstance().getMapper().writeValueAsString(auditMap));
                        } else {
                            config.getAuditFunc().accept(Config.getInstance().getMapper().writeValueAsString(auditMap));
                        }
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                } catch (Throwable e) {
                    logger.error("ExchangeListener Throwable", e);
                } finally {
                    nextListener.proceed();
                }
            });
        } else {
            config.getAuditFunc().accept(config.getConfig().getMapper().writeValueAsString(auditMap));
        }
        if(logger.isDebugEnabled()) logger.debug("AuditHandler.handleRequest ends.");
        next(exchange);
    }

    private void auditHeader(HttpServerExchange exchange, Map<String, Object> auditMap) {
        for (String name : config.getHeaderList()) {
            String value = exchange.getRequestHeaders().getFirst(name);
            if(logger.isTraceEnabled()) logger.trace("header name = " + name + " header value = " + value);
            auditMap.put(name, config.isMask() ? Mask.maskRegex(value, "requestHeader", name) : value);
        }
    }

    protected void next(HttpServerExchange exchange) throws Exception {
        Handler.next(exchange, next);
    }

    private void auditFields(Map<String, Object> auditInfo, Map<String, Object> auditMap) {
        for (String name : config.getAuditList()) {
            Object value = auditInfo.get(name);
            boolean needApplyMask = config.isMask() && value instanceof String;
            auditMap.put(name, needApplyMask ? Mask.maskRegex((String) value, MASK_KEY, name) : value);
        }
    }

    private void auditRequest(HttpServerExchange exchange, Map<String, Object> auditMap, AuditConfig config) {
        if (config.hasHeaderList()) {
            auditHeader(exchange, auditMap);
        }
        if (!config.hasAuditList()) {
            return;
        }
        for (String key : config.getAuditList()) {
            switch (key) {
                case REQUEST_BODY_KEY:
                    auditRequestBody(exchange, auditMap);
                    break;
                case REQUEST_COOKIES_KEY:
                    auditRequestCookies(exchange, auditMap);
                    break;
                case QUERY_PARAMETERS_KEY:
                    auditQueryParameters(exchange, auditMap);
                    break;
                case PATH_PARAMETERS_KEY:
                    auditPathParameters(exchange, auditMap);
                    break;
            }
        }
    }

    // Audit request body automatically if body handler enabled
    private void auditRequestBody(HttpServerExchange exchange, Map<String, Object> auditMap) {
        // Try to get BodyHandler cached request body string first to prevent unnecessary decoding
        String requestBodyString = exchange.getAttachment(AttachmentConstants.REQUEST_BODY_STRING);
        if (requestBodyString == null && exchange.getAttachment(AttachmentConstants.REQUEST_BODY) != null) {
            // try to convert the request body to JSON if possible. Fallback to to String().
            try {
                requestBodyString = Config.getInstance().getMapper().writeValueAsString(exchange.getAttachment(AttachmentConstants.REQUEST_BODY));
            } catch (JsonProcessingException e) {
                requestBodyString = exchange.getAttachment(AttachmentConstants.REQUEST_BODY).toString();
            }
        }
        // Mask requestBody json string if mask enabled
        if (requestBodyString != null && requestBodyString.length() > 0) {
            String contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
            if(contentType != null) {
                if(contentType.startsWith("application/json")) {
                    if(config.isMask()) requestBodyString = Mask.maskJson(requestBodyString, REQUEST_BODY_KEY);
                } else if(contentType.startsWith("text") || contentType.startsWith("application/xml")) {
                    if(config.isMask()) requestBodyString = Mask.maskString(requestBodyString, REQUEST_BODY_KEY);
                } else {
                    logger.error("Incorrect request content type " + contentType);
                }
            }
            if(requestBodyString.length() > config.getRequestBodyMaxSize()) {
                requestBodyString = requestBodyString.substring(0, config.getRequestBodyMaxSize());
            }
            auditMap.put(REQUEST_BODY_KEY, requestBodyString);
        }
    }

    // Audit response body
    private void auditResponseBody(HttpServerExchange exchange, Map<String, Object> auditMap) {
        String responseBodyString = exchange.getAttachment(AttachmentConstants.RESPONSE_BODY_STRING);
        if(responseBodyString == null && exchange.getAttachment(AttachmentConstants.RESPONSE_BODY) != null) {
            // try to convert the response body to JSON if possible. Fallback to String().
            try {
                responseBodyString = Config.getInstance().getMapper().writeValueAsString(exchange.getAttachment(AttachmentConstants.RESPONSE_BODY));
            } catch (JsonProcessingException e) {
                responseBodyString = exchange.getAttachment(AttachmentConstants.RESPONSE_BODY).toString();
            }
        }
        // mask the response body json string if mask is enabled.
        if(responseBodyString != null && responseBodyString.length() > 0) {
            String contentType = exchange.getResponseHeaders().getFirst(Headers.CONTENT_TYPE);
            if(contentType != null) {
                if(contentType.startsWith("application/json")) {
                    if(config.isMask()) responseBodyString =Mask.maskJson(responseBodyString, RESPONSE_BODY_KEY);
                } else if(contentType.startsWith("text") || contentType.startsWith("application/xml")) {
                    if(config.isMask()) responseBodyString = Mask.maskString(responseBodyString, RESPONSE_BODY_KEY);
                } else {
                    logger.error("Incorrect response content type " + contentType);
                }
            }
            if(responseBodyString.length() > config.getResponseBodyMaxSize()) {
                responseBodyString = responseBodyString.substring(0, config.getResponseBodyMaxSize());
            }
            auditMap.put(RESPONSE_BODY_KEY, responseBodyString);
        }
    }

    // Audit query parameters
    private void auditQueryParameters(HttpServerExchange exchange, Map<String, Object> auditMap) {
        Map<String, String> res = new HashMap<>();
        Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();
        if (queryParameters != null && queryParameters.size() > 0) {
            for (String query : queryParameters.keySet()) {
                String value = queryParameters.get(query).toString();
                String mask = config.isMask() ? Mask.maskRegex(value, QUERY_PARAMETERS_KEY, query) : value;
                res.put(query, mask);
            }
            auditMap.put(QUERY_PARAMETERS_KEY, res.toString());
        }
    }

    private void auditPathParameters(HttpServerExchange exchange, Map<String, Object> auditMap) {
        Map<String, String> res = new HashMap<>();
        Map<String, Deque<String>> pathParameters = exchange.getPathParameters();
        if (pathParameters != null && pathParameters.size() > 0) {
            for (String name : pathParameters.keySet()) {
                String value = pathParameters.get(name).toString();
                String mask = config.isMask() ? Mask.maskRegex(value, PATH_PARAMETERS_KEY, name) : value;
                res.put(name, mask);
            }
            auditMap.put(PATH_PARAMETERS_KEY, res.toString());
        }
    }

    private void auditRequestCookies(HttpServerExchange exchange, Map<String, Object> auditMap) {
        Map<String, String> res = new HashMap<>();
        Iterable<Cookie> iterable = exchange.requestCookies();
        if(iterable != null) {
            Iterator<Cookie> iterator = iterable.iterator();
            while(iterator.hasNext()) {
                Cookie cookie = iterator.next();
                String name = cookie.getName();
                String value = cookie.getValue();
                String mask = config.isMask() ? Mask.maskRegex(value, REQUEST_COOKIES_KEY, name) : value;
                res.put(name, mask);
            }
            auditMap.put(REQUEST_COOKIES_KEY, res.toString());
        }
    }

    private void auditServiceId(Map<String, Object> auditMap) {
        if (!StringUtils.isBlank(serviceId)) {
            auditMap.put(SERVICE_ID_KEY, serviceId);
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
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(AuditConfig.CONFIG_NAME, AuditHandler.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(AuditConfig.CONFIG_NAME, AuditHandler.class.getName(), config.getMappedConfig(), null);
        if(logger.isInfoEnabled()) logger.info("AuditHandler is reloaded.");
    }
}
