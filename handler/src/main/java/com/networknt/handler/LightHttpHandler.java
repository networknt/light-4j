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

import com.networknt.audit.AuditConfig;
import com.networknt.common.ContentType;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.status.Status;
import com.networknt.status.StatusWrapper;
import com.networknt.utility.Constants;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This is an extension of HttpHandler that provides a default method to handle
 * error status. All API handler should extend from this interface.
 *
 * @author Steve Hu
 */
public interface LightHttpHandler extends HttpHandler {
    /** Logger instance */
    Logger logger = LoggerFactory.getLogger(LightHttpHandler.class);
    /** Error code for not defined error */
    String ERROR_NOT_DEFINED = "ERR10042";

    /** Audit configuration instance */
    AuditConfig auditConfig = AuditConfig.load();
    /** Indicator of audit on error */
    boolean AUDIT_ON_ERROR = auditConfig.isAuditOnError();
    /** Indicator of audit stack trace */
    boolean AUDIT_STACK_TRACE = auditConfig.isAuditStackTrace();


    /**
     * This method is used to construct a standard error status in JSON format from an error code.
     *
     * @param exchange The HttpServerExchange to set the status on.
     * @param code     The error code from status.yml.
     * @param args     Arguments for formatting the error description.
     */
    default void setExchangeStatus(HttpServerExchange exchange, String code, final Object... args) {
        var status = new Status(code, args);

        // There is no entry in status.yml for this particular error code.
        if (status.getStatusCode() == 0)
            status = new Status(ERROR_NOT_DEFINED, code);

        setExchangeStatus(exchange, status);
    }

    /**
     * This method is used to construct a standard error status with metadata in JSON format from an error code.
     *
     * @param exchange The HttpServerExchange to set the status on.
     * @param code     The error code from status.yml.
     * @param metadata Additional metadata to include in the response.
     * @param args     Arguments for formatting the error description.
     */
    default void setExchangeStatus(HttpServerExchange exchange, String code, Map<String, Object> metadata, final Object... args) {
        var status = new Status(code, args);

        // There is no entry in status.yml for this particular error code.
        if (status.getStatusCode() == 0)
            status = new Status(ERROR_NOT_DEFINED, code);

        status.setMetadata(metadata);
        setExchangeStatus(exchange, status);
    }

    /**
     * There are situations that the downstream service returns an error status response and we just
     * want to bubble up to the caller and eventually to the original caller.
     *
     * @param ex     The HttpServerExchange to set the status on.
     * @param status The Status object containing error details.
     */
    default void setExchangeStatus(HttpServerExchange ex, Status status) {
        // Wrap default status into custom status if the implementation of StatusWrapper was provided
        StatusWrapper statusWrapper;

        try {
            statusWrapper = SingletonServiceFactory.getBean(StatusWrapper.class);

        } catch (NoClassDefFoundError e) {
            statusWrapper = null;
        }

        status = statusWrapper == null ? status : statusWrapper.wrap(status, ex);
        ex.setStatusCode(status.getStatusCode());
        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, ContentType.APPLICATION_JSON.value());
        status.setDescription(status.getDescription().replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\""));

        var elements = Thread.currentThread().getStackTrace();

        if (logger.isErrorEnabled())
            logger.error(status.toString());

        // in case to trace where the status is created, enable the trace level logging to diagnose.
        if (logger.isTraceEnabled())
            logger.trace(Arrays.stream(elements).map(StackTraceElement::toString).collect(Collectors.joining("\n")));

        // In normal case, the auditInfo shouldn't be null as it is created by OpenApiHandler with
        // endpoint and openapiOperation available. This handler will enrich the auditInfo.
        @SuppressWarnings("unchecked")
        Map<String, Object> auditInfo = ex.getAttachment(AttachmentConstants.AUDIT_INFO);
        if (auditInfo == null) {
            auditInfo = new HashMap<>();
            ex.putAttachment(AttachmentConstants.AUDIT_INFO, auditInfo);
        }

        // save info for auditing purposes in case of an error
        if (AUDIT_ON_ERROR)
            auditInfo.put(Constants.STATUS, status);

        if (AUDIT_STACK_TRACE)
            auditInfo.put(Constants.STACK_TRACE, Arrays.toString(elements));

        ex.getResponseSender().send(status.toStringConditionally());
    }
}
