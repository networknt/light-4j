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

import com.networknt.body.BodyHandler;
import com.networknt.status.Status;
import com.networknt.swagger.NormalisedPath;
import com.networknt.swagger.SwaggerOperation;
import com.networknt.validator.parameter.ParameterValidators;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Validate a request against a given API operation.
 */
public class RequestValidator {

    static final Logger logger = LoggerFactory.getLogger(RequestValidator.class);
    static final String VALIDATOR_REQUEST_BODY_UNEXPECTED = "ERR11013";
    static final String VALIDATOR_REQUEST_BODY_MISSING = "ERR11014";
    static final String VALIDATOR_REQUEST_PARAMETER_HEADER_MISSING = "ERR11017";
    static final String VALIDATOR_REQUEST_PARAMETER_QUERY_MISSING = "ERR11000";

    private final SchemaValidator schemaValidator;
    private final ParameterValidators parameterValidators;

    /**
     * Construct a new request validator with the given schema validator.
     *
     * @param schemaValidator The schema validator to use when validating request bodies
     */
    public RequestValidator(final SchemaValidator schemaValidator) {
        this.schemaValidator = requireNonNull(schemaValidator, "A schema validator is required");
        this.parameterValidators = new ParameterValidators(schemaValidator);
    }

    /**
     * Validate the request against the given API operation
     *
     * @param exchange The HttpServerExchange to validate
     * @return A validation report containing validation errors
     */
    public Status validateRequest(final NormalisedPath requestPath, HttpServerExchange exchange, SwaggerOperation swaggerOperation) {
        requireNonNull(requestPath, "A request path is required");
        requireNonNull(exchange, "An exchange is required");
        requireNonNull(swaggerOperation, "An swagger operation is required");

        Status status = validatePathParameters(requestPath, swaggerOperation);
        if(status != null) return status;

        status = validateQueryParameters(exchange, swaggerOperation);
        if(status != null) return status;

        status = validateHeader(exchange, swaggerOperation);
        if(status != null) return status;

        Object body = exchange.getAttachment(BodyHandler.REQUEST_BODY);
        status = validateRequestBody(body, swaggerOperation);

        return status;
    }

    private Status validateRequestBody(Object requestBody,
                                                 final SwaggerOperation swaggerOperation) {
        final Optional<Parameter> bodyParameter = swaggerOperation.getOperation().getParameters()
                .stream().filter(p -> p.getIn().equalsIgnoreCase("body")).findFirst();

        if (requestBody != null && !bodyParameter.isPresent()) {
            return new Status(VALIDATOR_REQUEST_BODY_UNEXPECTED, swaggerOperation.getMethod(), swaggerOperation.getPathString().original());
        }

        if (!bodyParameter.isPresent()) {
            return null;
        }

        if (requestBody == null) {
            if (bodyParameter.isPresent() && bodyParameter.get().getRequired()) {
                return new Status(VALIDATOR_REQUEST_BODY_MISSING, swaggerOperation.getMethod(), swaggerOperation.getPathString().original());
            }
            return null;
        }
        return schemaValidator.validate(requestBody, ((BodyParameter)bodyParameter.get()).getSchema());
    }

    private Status validatePathParameters(final NormalisedPath requestPath,
                                          final SwaggerOperation swaggerOperation) {
        Status status = null;
        for (int i = 0; i < swaggerOperation.getPathString().parts().size(); i++) {
            if (!swaggerOperation.getPathString().isParam(i)) {
                continue;
            }

            final String paramName = swaggerOperation.getPathString().paramName(i);
            final String paramValue = requestPath.part(i);

            final Optional<Parameter> parameter = swaggerOperation.getOperation().getParameters()
                    .stream()
                    .filter(p -> p.getIn().equalsIgnoreCase("PATH"))
                    .filter(p -> p.getName().equalsIgnoreCase(paramName))
                    .findFirst();

            if (parameter.isPresent()) {
                status = parameterValidators.validate(paramValue, parameter.get());
            }
        }
        return status;
    }

    private Status validateQueryParameters(final HttpServerExchange exchange,
                                           final SwaggerOperation swaggerOperation) {
        Optional<Status> optional = swaggerOperation
                .getOperation()
                .getParameters()
                .stream()
                .filter(p -> p.getIn().equalsIgnoreCase("QUERY"))
                .map(p -> validateQueryParameter(exchange, swaggerOperation, p))
                .filter(s -> s != null)
                .findFirst();
        if(optional.isPresent()) {
            return optional.get();
        } else {
            return null;
        }
    }


    private Status validateQueryParameter(final HttpServerExchange exchange,
                                          final SwaggerOperation swaggerOperation,
                                          final Parameter queryParameter) {

        final Collection<String> queryParameterValues = exchange.getQueryParameters().get(queryParameter.getName());

        if ((queryParameterValues == null || queryParameterValues.isEmpty())) {
            if(queryParameter.getRequired()) {
                return new Status(VALIDATOR_REQUEST_PARAMETER_QUERY_MISSING, queryParameter.getName(), swaggerOperation.getPathString().original());
            }
        } else {

            Optional<Status> optional = queryParameterValues
                    .stream()
                    .map((v) -> parameterValidators.validate(v, queryParameter))
                    .filter(s -> s != null)
                    .findFirst();
            if(optional.isPresent()) {
                return optional.get();
            }
        }
        return null;
    }

    private Status validateHeader(final HttpServerExchange exchange,
                                  final SwaggerOperation swaggerOperation) {
        Optional<Status> optional = swaggerOperation
                .getOperation()
                .getParameters()
                .stream()
                .filter(p -> p.getIn().equalsIgnoreCase("header"))
                .map(p -> validateHeader(exchange, swaggerOperation, p))
                .filter(s -> s != null)
                .findFirst();
        if(optional.isPresent()) {
            return optional.get();
        } else {
            return null;
        }
    }

    private Status validateHeader(final HttpServerExchange exchange,
                                  final SwaggerOperation swaggerOperation,
                                  final Parameter headerParameter) {

        final HeaderValues headerValues = exchange.getRequestHeaders().get(headerParameter.getName());
        if ((headerValues == null || headerValues.isEmpty())) {
            if(headerParameter.getRequired()) {
                return new Status(VALIDATOR_REQUEST_PARAMETER_HEADER_MISSING, headerParameter.getName(), swaggerOperation.getPathString().original());
            }
        } else {

            Optional<Status> optional = headerValues
                    .stream()
                    .map((v) -> parameterValidators.validate(v, headerParameter))
                    .filter(s -> s != null)
                    .findFirst();
            if(optional.isPresent()) {
                return optional.get();
            }
        }
        return null;
    }
}
