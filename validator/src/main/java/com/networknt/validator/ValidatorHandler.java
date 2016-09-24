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

package com.networknt.validator;

import com.networknt.config.Config;
import com.networknt.status.Status;
import com.networknt.utility.path.ApiNormalisedPath;
import com.networknt.utility.path.SwaggerHelper;
import com.networknt.utility.path.NormalisedPath;
import com.networknt.validator.report.MessageResolver;
import com.networknt.validator.report.MutableValidationReport;
import com.networknt.validator.report.ValidationReport;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * This is a swagger validator handler that validate request and response based on the spec. In
 * production only request validator should be turned on and response validator should only be
 * used during development.
 *
 * Created by steve on 17/09/16.
 */
public class ValidatorHandler implements HttpHandler {
    public static final String CONFIG_NAME = "validator";
    public static final String ENABLE_VALIDATOR = "enableValidator";
    static final String ENABLE_RESPONSE_VALIDATOR = "enableResponseValidator";

    static final String STATUS_INVALID_REQUEST_PATH = "ERR10007";
    static final String STATUS_METHOD_NOT_ALLOWED = "ERR10008";

    static final AttachmentKey<String> REQUEST_BODY = AttachmentKey.create(String.class);
    static final Logger logger = LoggerFactory.getLogger(ValidatorHandler.class);

    private final HttpHandler next;

    static final MessageResolver messages = new MessageResolver();

    RequestValidator requestValidator;
    ResponseValidator responseValidator;

    public ValidatorHandler(final HttpHandler next) {

        MessageResolver messages = new MessageResolver();
        final SchemaValidator schemaValidator = new SchemaValidator(SwaggerHelper.swagger, messages);
        this.requestValidator = new RequestValidator(schemaValidator, messages);
        this.responseValidator = new ResponseValidator(schemaValidator, messages);
        this.next = next;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        ValidatorConfig config = (ValidatorConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ValidatorConfig.class);

        final MutableValidationReport validationReport = new MutableValidationReport();
        final NormalisedPath requestPath = new ApiNormalisedPath(exchange.getRequestURI());

        final Optional<NormalisedPath> maybeApiPath = SwaggerHelper.findMatchingApiPath(requestPath);
        if (!maybeApiPath.isPresent()) {
            Status status = new Status(STATUS_INVALID_REQUEST_PATH);
            exchange.setStatusCode(status.getStatusCode());
            exchange.getResponseSender().send(status.toString());
            return;
        }

        final NormalisedPath swaggerPathString = maybeApiPath.get();
        final Path swaggerPath = SwaggerHelper.swagger.getPath(swaggerPathString.original());

        final HttpMethod httpMethod = HttpMethod.valueOf(exchange.getRequestMethod().toString());
        final Operation operation = swaggerPath.getOperationMap().get(httpMethod);

        if (operation == null) {
            Status status = new Status(STATUS_METHOD_NOT_ALLOWED);
            exchange.setStatusCode(status.getStatusCode());
            exchange.getResponseSender().send(status.toString());
            return;
        }

        final SwaggerOperation swaggerOperation = new SwaggerOperation(swaggerPathString, swaggerPath, httpMethod, operation);

        if(exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
        exchange.startBlocking();

        ValidationReport report = requestValidator.validateRequest(requestPath, exchange, swaggerOperation);
        validationReport.merge(report);
        if(validationReport.hasErrors()) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(validationReport));
            return;
        }

        if(config.enableResponseValidator) {
            exchange.addExchangeCompleteListener(new ExchangeCompletionListener() {
                @Override
                public void exchangeEvent(final HttpServerExchange exchange, final NextListener nextListener) {
                    validationReport.merge(responseValidator.validateResponse(exchange, swaggerOperation));
                    if(validationReport.hasErrors()) {
                        logger.error("Response error" + validationReport);
                    }
                    nextListener.proceed();
                }
            });
        }

        next.handleRequest(exchange);
    }
}
