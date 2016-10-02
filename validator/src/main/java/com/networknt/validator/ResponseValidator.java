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

import com.networknt.status.Status;
import com.networknt.swagger.SwaggerOperation;
import io.undertow.server.HttpServerExchange;

import static java.util.Objects.requireNonNull;

/**
 * Validate a response against an API operation
 */
public class ResponseValidator {
    private final SchemaValidator schemaValidator;

    /**
     * Construct a new response validator with the given schema validator.
     *
     * @param schemaValidator The schema validator to use when validating response bodies
     */
    public ResponseValidator(final SchemaValidator schemaValidator) {
        this.schemaValidator = requireNonNull(schemaValidator, "A schema validator is required");
    }

    /**
     * Validate the given response against the API operation.
     *
     * @param exchange The exchange to validate
     * @param swaggerOperation The API operation to validate the response against
     *
     * @return A status containing validation error
     */
    public Status validateResponse(final HttpServerExchange exchange, final SwaggerOperation swaggerOperation) {
        requireNonNull(exchange, "An exchange is required");
        requireNonNull(swaggerOperation, "A swagger operation is required");

        io.swagger.models.Response swaggerResponse = swaggerOperation.getOperation().getResponses().get(Integer.toString(exchange.getStatusCode()));
        if (swaggerResponse == null) {
            swaggerResponse = swaggerOperation.getOperation().getResponses().get("default"); // try the default response
        }

        if (swaggerResponse == null) {
            return new Status("ERR11015", exchange.getStatusCode(), swaggerOperation.getPathString().original());
        }

        if (swaggerResponse.getSchema() == null) {
            return null;
        }
        String body = exchange.getOutputStream().toString();

        if (body == null || body.length() == 0) {
            return new Status("ERR11016", swaggerOperation.getMethod(), swaggerOperation.getPathString().original());
        }
        return schemaValidator.validate(body, swaggerResponse.getSchema());
    }

}
