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

package com.networknt.handler;

import com.networknt.config.Config;
import com.networknt.handler.config.HandlerConfig;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.status.Status;
import com.networknt.status.StatusWrapper;
import com.networknt.utility.Constants;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an extension of HttpHandler that provides a default method to handle
 * error status. All API handler should extend from this interface.
 *
 * @author Steve Hu
 */
public interface LightHttpHandler extends HttpHandler {
    Logger logger = LoggerFactory.getLogger(LightHttpHandler.class);
    String ERROR_NOT_DEFINED = "ERR10042";

    // Handler can save errors and stack traces for auditing. Default: false
    String CONFIG_NAME = "handler";
    String AUDIT_CONFIG_NAME = "audit";
    String AUDIT_ON_ERROR = "auditOnError";
    String AUDIT_STACK_TRACE = "auditStackTrace";

    HandlerConfig config = (HandlerConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, HandlerConfig.class);
    Map<String, Object> auditConfig = Config.getInstance().getDefaultJsonMapConfigNoCache(AUDIT_CONFIG_NAME);

    boolean auditOnError = auditConfig == null ? false : auditConfig.get(AUDIT_ON_ERROR) != null ? (boolean)auditConfig.get(AUDIT_ON_ERROR) : false;
    boolean auditStackTrace = auditConfig == null ? false : auditConfig.get(AUDIT_STACK_TRACE) != null ? (boolean)auditConfig.get(AUDIT_ON_ERROR) : false;

    /**
     * This method is used to construct a standard error status in JSON format from an error code.
     *
     * @param exchange HttpServerExchange
     * @param code     error code
     * @param args     arguments for error description
     */
    default void setExchangeStatus(HttpServerExchange exchange, String code, final Object... args) {
        Status status = new Status(code, args);
        if (status.getStatusCode() == 0) {
            // There is no entry in status.yml for this particular error code.
            status = new Status(ERROR_NOT_DEFINED, code);
        }
        setExchangeStatus(exchange, status);
    }

    /**
     * This method is used to construct a standard error status with metadata in JSON format from an error code.
     *
     * @param exchange HttpServerExchange
     * @param code     error code
     * @param metadata additional metadata info
     * @param args     arguments for error description
     */
    default void setExchangeStatus(HttpServerExchange exchange, String code, Map<String, Object> metadata, final Object... args) {
        Status status = new Status(code, args);
        if (status.getStatusCode() == 0) {
            // There is no entry in status.yml for this particular error code.
            status = new Status(ERROR_NOT_DEFINED, code);
        }
        status.setMetadata(metadata);
        setExchangeStatus(exchange, status);
    }

    /**
     * There are situations that the downstream service returns an error status response and we just
     * want to bubble up to the caller and eventually to the original caller.
     *
     * @param exchange HttpServerExchange
     * @param status   error status
     */
    default void setExchangeStatus(HttpServerExchange exchange, Status status) {
        // Wrap default status into custom status if the implementation of StatusWrapper was provided
        StatusWrapper statusWrapper;
        try {
            statusWrapper = SingletonServiceFactory.getBean(StatusWrapper.class);
        } catch (NoClassDefFoundError e) {
            statusWrapper = null;
        }
        status = statusWrapper == null ? status : statusWrapper.wrap(status, exchange);

        exchange.setStatusCode(status.getStatusCode());
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        status.setDescription(status.getDescription().replaceAll("\\\\", "\\\\\\\\"));
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();

        logger.error(status.toString());
        // in case to trace where the status is created, enable the trace level logging to diagnose.
        if (logger.isTraceEnabled()) {
            String stackTrace = Arrays.stream(elements)
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining("\n"));
            logger.trace(stackTrace);
        }
        // In normal case, the auditInfo shouldn't be null as it is created by OpenApiHandler with
        // endpoint and openapiOperation available. This handler will enrich the auditInfo.
        @SuppressWarnings("unchecked")
        Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        if (auditInfo == null) {
            auditInfo = new HashMap<>();
            exchange.putAttachment(AttachmentConstants.AUDIT_INFO, auditInfo);
        }

        // save info for auditing purposes in case of an error
        if (auditOnError)
            auditInfo.put(Constants.STATUS, status);
        if (auditStackTrace) {
            auditInfo.put(Constants.STACK_TRACE, Arrays.toString(elements));
        }
        exchange.getResponseSender().send(status.toString());
    }
}
