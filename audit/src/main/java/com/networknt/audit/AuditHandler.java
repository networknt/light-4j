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
import com.networknt.utility.StringUtils;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.HeaderMap;
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
 * <p>
 * This handler can be used on production but be aware that it will impact the overall performance.
 * Turning off statusCode and responseTime can make it faster as these have to be captured on the
 * response chain instead of request chain.
 * <p>
 * For most business and the majority of microservices, you don't need to enable this handler due to
 * performance reason. The default audit log will be the audit.log configured in the default logback.xml;
 * however, it can be changed to syslog or Kafka with customized appender.
 * <p>
 * Majority of the fields in audit log are collected in request and response; however, to allow
 * user to customize it, we have put an attachment into the exchange to allow other handlers to
 * write important info into it. The audit.yml can control which fields should be included in the
 * final log.
 * <p>
 * By default, the following fields are included:
 * <p>
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
 * <p>
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
    static final String REQUEST_HEADER_KEY = "requestHeader";
    static final String QUERY_PARAMETERS_KEY = "queryParameters";
    static final String PATH_PARAMETERS_KEY = "pathParameters";
    static final String REQUEST_COOKIES_KEY = "requestCookies";
    static final String SERVICE_ID_KEY = "serviceId";
    static final String INVALID_CONFIG_VALUE_CODE = "ERR10060";


    private volatile HttpHandler next;
    private volatile AuditConfig config;
    private volatile String serviceId;
    private volatile DateTimeFormatter dateTimeFormatter;

    public AuditHandler() {
        logger.info("AuditHandler is loaded.");
        ServerConfig serverConfig = ServerConfig.load();
        if (serverConfig != null) {
            serviceId = serverConfig.getServiceId();
        }
        config = AuditConfig.load();
        String timestampFormat = config.getTimestampFormat();
        if (!StringUtils.isBlank(timestampFormat)) {
            try {
                dateTimeFormatter = DateTimeFormatter.ofPattern(timestampFormat)
                        .withZone(ZoneId.systemDefault());
            } catch (IllegalArgumentException e) {
                logger.error(new Status(INVALID_CONFIG_VALUE_CODE, timestampFormat, "timestampFormat", "audit.yml").toString());
                dateTimeFormatter = null;
            }
        } else {
            dateTimeFormatter = null;
        }
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (logger.isDebugEnabled()) logger.debug("AuditHandler.handleRequest starts.");
        AuditConfig newConfig = AuditConfig.load();
        if (newConfig != config) {
            synchronized (this) {
                if (newConfig != config) {
                    config = newConfig;
                    ServerConfig serverConfig = ServerConfig.getInstance();
                    if (serverConfig != null) {
                        serviceId = serverConfig.getServiceId();
                    }
                    String timestampFormat = config.getTimestampFormat();
                    if (!StringUtils.isBlank(timestampFormat)) {
                        try {
                            dateTimeFormatter = DateTimeFormatter.ofPattern(timestampFormat)
                                    .withZone(ZoneId.systemDefault());
                        } catch (IllegalArgumentException e) {
                            logger.error(new Status(INVALID_CONFIG_VALUE_CODE, timestampFormat, "timestampFormat", "audit.yml").toString());
                            dateTimeFormatter = null;
                        }
                    } else {
                        dateTimeFormatter = null;
                    }
                }
            }
        }
        final Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        final Map<String, Object> auditMap = new LinkedHashMap<>();
        final long start = System.currentTimeMillis();

        // add audit timestamp
        auditMap.put(TIMESTAMP, dateTimeFormatter == null ? System.currentTimeMillis() : dateTimeFormatter.format(Instant.now()));

        // dump audit info fields according to config
        boolean needAuditData = auditInfo != null && config.hasAuditList();
        if (needAuditData) {
            auditFields(auditInfo, auditMap, config);
        }

        // dump request header, request body, path parameters, query parameters and request cookies according to config
        auditRequest(exchange, auditMap, config);

        // dump serviceId from server.yml
        if (config.hasAuditList() && config.getAuditList().contains(SERVICE_ID_KEY)) {
            auditServiceId(auditMap);
        }

        if (config.isStatusCode() || config.isResponseTime()) {
            exchange.addExchangeCompleteListener((completedExchange, nextListener) -> {
                // response status code and response time.
                try {
                    if (config.isStatusCode()) {
                        auditMap.put(STATUS_CODE, completedExchange.getStatusCode());
                    }
                    if (config.isResponseTime()) {
                        auditMap.put(RESPONSE_TIME, System.currentTimeMillis() - start);
                    }
                    // add additional fields accumulated during the microservice execution
                    // according to the config
                    Map<String, Object> auditInfo1 = completedExchange.getAttachment(AttachmentConstants.AUDIT_INFO);
                    if (auditInfo1 != null && config.getAuditList() != null) {
                        for (String name : config.getAuditList()) {
                            Object object = auditInfo1.get(name);
                            if (object != null) {
                                auditMap.put(name, object);
                            }
                        }
                    }
                    // audit the response body.
                    if (config.getAuditList() != null && config.getAuditList().contains(RESPONSE_BODY_KEY)) {
                        AuditHandler.auditResponseBody(completedExchange, auditMap, config);
                    }
                    AuditHandler.logAudit(completedExchange, auditMap, config);
                } catch (Exception e) {
                    logger.error("ExchangeListener Throwable", e);
                } finally {
                    nextListener.proceed();
                }
            });
        } else {
            config.getAuditFunc().accept(Config.getInstance().getMapper().writeValueAsString(auditMap));
        }
        logger.debug("AuditHandler.handleRequest ends.");
        next(exchange);
    }

    /**
     * Log the audit information to the configured audit function.
     *
     * @param exchange the HttpServerExchange containing the request and response
     * @param auditMap the map containing the audit information
     */
    private static void logAudit(final HttpServerExchange exchange, final Map<String, Object> auditMap, AuditConfig config) {
        try {
            // audit entries only is it is an error, if auditOnError flag is set
            if (config.isAuditOnError()) {
                if (exchange.getStatusCode() >= 400)
                    config.getAuditFunc().accept(Config.getInstance().getMapper().writeValueAsString(auditMap));
            } else {
                config.getAuditFunc().accept(Config.getInstance().getMapper().writeValueAsString(auditMap));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Audit the request headers and put them into the audit map.
     *
     * @param exchange the HttpServerExchange containing the request headers
     * @param auditMap the map to hold the audited headers
     */
    private static void auditHeader(final HttpServerExchange exchange, final Map<String, Object> auditMap, AuditConfig config) {
        for (String name : config.getHeaderList()) {
            String value = exchange.getRequestHeaders().getFirst(name);
            logger.trace("header name = {} header value = {}", name, value);
            if (config.isMask())
                auditMap.put(name, Mask.maskRegex(value, REQUEST_HEADER_KEY, name));
            else
                auditMap.put(name, value);
        }
    }

    /**
     * Audit the fields in the auditInfo map and put them into the audit map.
     *
     * @param auditInfo the map containing the audit information
     * @param auditMap the map to hold the audited fields
     */
    private void auditFields(final Map<String, Object> auditInfo, final Map<String, Object> auditMap, AuditConfig config) {
        for (String name : config.getAuditList()) {
            final Object value = auditInfo.get(name);
            if (config.isMask() && value instanceof String stringValue)
                auditMap.put(name, Mask.maskRegex(stringValue, MASK_KEY, name));
            else
                auditMap.put(name, value);
        }
    }

    /**
     * Audit the request and put it into the audit map.
     *
     * @param exchange the HttpServerExchange containing the request
     * @param auditMap the map to hold the audited request data
     */
    private void auditRequest(final HttpServerExchange exchange, final Map<String, Object> auditMap, AuditConfig config) {
        if (config.hasHeaderList()) {
            AuditHandler.auditHeader(exchange, auditMap, config);
        }
        if (!config.hasAuditList()) {
            return;
        }
        for (String key : config.getAuditList()) {
            switch (key) {
                case REQUEST_BODY_KEY:
                    AuditHandler.auditRequestBody(exchange, auditMap, config);
                    break;
                case REQUEST_COOKIES_KEY:
                    AuditHandler.auditRequestCookies(exchange, auditMap, config);
                    break;
                case QUERY_PARAMETERS_KEY:
                    AuditHandler.auditQueryParameters(exchange, auditMap, config);
                    break;
                case PATH_PARAMETERS_KEY:
                    AuditHandler.auditPathParameters(exchange, auditMap, config);
                    break;
                default:
                    logger.error("Unknown audit key {} in audit.yml", key);
                    break;
            }
        }
    }

    /**
     * Audit the body of the request and put it into the audit map.
     *
     * @param exchange the HttpServerExchange containing the request body
     * @param auditMap the map to hold the audited request body
     */
    private static void auditRequestBody(final HttpServerExchange exchange, final Map<String, Object> auditMap, AuditConfig config) {
        AuditHandler.auditBody(
                exchange.getRequestHeaders(),
                exchange.getAttachment(AttachmentConstants.REQUEST_BODY_STRING),
                exchange.getAttachment(AttachmentConstants.REQUEST_BODY),
                auditMap,
                REQUEST_BODY_KEY,
                config
        );
    }

    /**
     * Audit the body of the response and put it into the audit map.
     *
     * @param exchange the HttpServerExchange containing the response body
     * @param auditMap the map to hold the audited response body
     */
    private static void auditResponseBody(final HttpServerExchange exchange, final Map<String, Object> auditMap, AuditConfig config) {
        AuditHandler.auditBody(
                exchange.getResponseHeaders(),
                exchange.getAttachment(AttachmentConstants.RESPONSE_BODY_STRING),
                exchange.getAttachment(AttachmentConstants.RESPONSE_BODY),
                auditMap,
                RESPONSE_BODY_KEY,
                config
        );
    }

    /**
     * Audit the body of the request or response and put it into the audit map.
     *
     * @param headers the headers of the request or response
     * @param bodyString the body as a string, can be null
     * @param bodyRaw the raw body object, can be null
     * @param auditMap the map to hold the audited body
     * @param auditKey the key to be used in the audit map
     */
    private static void auditBody(final HeaderMap headers, final String bodyString, final Object bodyRaw, Map<String, Object> auditMap, final String auditKey, AuditConfig config) {
        if (bodyString == null && bodyRaw == null) {
            logger.debug("No body present to audit for {}", auditKey);
            return;
        }

        String parsedBodyString = bodyString;
        if (parsedBodyString == null) {
            try {
                parsedBodyString = Config.getInstance().getMapper().writeValueAsString(bodyRaw);
            } catch (JsonProcessingException e) {
                parsedBodyString = bodyRaw.toString();
            }
        }

        if (parsedBodyString != null && !parsedBodyString.isEmpty()) {
            final String contentType = headers.getFirst(Headers.CONTENT_TYPE);

            if (config.isMask() && contentType != null) {

                if (contentType.startsWith("application/json")) {
                    parsedBodyString = Mask.maskJson(parsedBodyString, auditKey);

                } else if (contentType.startsWith("text") || contentType.startsWith("application/xml")) {
                    parsedBodyString = Mask.maskString(parsedBodyString, auditKey);

                } else {
                    logger.error("Incorrect content-type {} for {}", contentType, auditKey);
                }
            }

            // if the parsedBodyString is larger than the max size, we will truncate it.
            if (parsedBodyString != null && parsedBodyString.length() > config.getResponseBodyMaxSize()) {
                parsedBodyString = parsedBodyString.substring(0, config.getResponseBodyMaxSize());
            }

            auditMap.put(auditKey, parsedBodyString);
        }
    }


    /**
     * Audit the query parameters and put them into the audit map.
     *
     * @param exchange the HttpServerExchange containing the query parameters
     * @param auditMap the map to hold the audited query parameters
     */
    private static void auditQueryParameters(final HttpServerExchange exchange, final Map<String, Object> auditMap, AuditConfig config) {
        auditParameters(exchange.getQueryParameters(), auditMap, QUERY_PARAMETERS_KEY, config);
    }

    /**
     * Audit the path parameters and put them into the audit map.
     *
     * @param exchange the HttpServerExchange containing the path parameters
     * @param auditMap the map to hold the audited path parameters
     */
    private static void auditPathParameters(final HttpServerExchange exchange, final Map<String, Object> auditMap, AuditConfig config) {
        auditParameters(exchange.getPathParameters(), auditMap, PATH_PARAMETERS_KEY, config);
    }

    /**
     * Audit the parameters from either query or path parameters.
     *
     * @param parameters the parameters to be audited
     * @param auditMap the map to hold the audited parameters
     * @param auditKey the key to be used in the audit map
     */
    private static void auditParameters(Map<String, Deque<String>> parameters, Map<String, Object> auditMap, String auditKey, AuditConfig config) {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }
        Map<String, String> res = HashMap.newHashMap(parameters.size());
        for (Map.Entry<String, Deque<String>> entry : parameters.entrySet()) {
            String name = entry.getKey();
            String value = parameters.get(name).toString();
            res.put(name, config.isMask() ? Mask.maskRegex(value, auditKey, name) : value);
        }
        auditMap.put(auditKey, res.toString());
    }

    /**
     * Audit the request cookies and put them into the audit map.
     *
     * @param exchange the HttpServerExchange containing the request cookies
     * @param auditMap the map to hold the audited cookies
     */
    private static void auditRequestCookies(final HttpServerExchange exchange, final Map<String, Object> auditMap, AuditConfig config) {
        Iterable<Cookie> iterable = exchange.requestCookies();
        if (iterable == null || !iterable.iterator().hasNext()) {
            return;
        }
        Map<String, String> res = new HashMap<>();
        for (Cookie cookie : iterable) {
            String name = cookie.getName();
            String value = cookie.getValue();
            String mask = config.isMask() ? Mask.maskRegex(value, REQUEST_COOKIES_KEY, name) : value;
            res.put(name, mask);
        }
        auditMap.put(REQUEST_COOKIES_KEY, res.toString());
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
        return AuditConfig.load().isEnabled();
    }

    protected void next(HttpServerExchange exchange) throws Exception {
        Handler.next(exchange, next);
    }

}
