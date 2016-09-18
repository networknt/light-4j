package com.networknt.validator;

import com.networknt.validator.report.MessageResolver;
import com.networknt.validator.report.MutableValidationReport;
import com.networknt.validator.report.ValidationReport;
import io.undertow.server.HttpServerExchange;

import java.util.Optional;
import java.util.Scanner;

import static java.util.Objects.requireNonNull;

/**
 * Validate a response against an API operation
 */
public class ResponseValidator {
    private final SchemaValidator schemaValidator;
    private final MessageResolver messages;

    /**
     * Construct a new response validator with the given schema validator.
     *
     * @param schemaValidator The schema validator to use when validating response bodies
     * @param messages The message resolver to use
     */
    public ResponseValidator(final SchemaValidator schemaValidator, final MessageResolver messages) {
        this.schemaValidator = requireNonNull(schemaValidator, "A schema validator is required");
        this.messages = requireNonNull(messages, "A message resolver is required");
    }

    /**
     * Validate the given response against the API operation.
     *
     * @param exchange The exchange to validate
     * @param swaggerOperation The API operation to validate the response against
     *
     * @return A validation report containing validation errors
     */
    public ValidationReport validateResponse(final HttpServerExchange exchange, final SwaggerOperation swaggerOperation) {
        requireNonNull(exchange, "An exchange is required");
        requireNonNull(swaggerOperation, "A swagger operation is required");

        io.swagger.models.Response swaggerResponse = swaggerOperation.getOperation().getResponses().get(Integer.toString(exchange.getStatusCode()));
        if (swaggerResponse == null) {
            swaggerResponse = swaggerOperation.getOperation().getResponses().get("default"); // try the default response
        }

        final MutableValidationReport validationReport = new MutableValidationReport();
        if (swaggerResponse == null) {
            return validationReport.add(
                    messages.get("validation.response.status.unknown",
                            exchange.getStatusCode(), swaggerOperation.getPathString().original())
            );
        }

        if (swaggerResponse.getSchema() == null) {
            return validationReport;
        }
        String body = exchange.getOutputStream().toString();

        if (body == null || body.length() == 0) {
            return validationReport.add(
                    messages.get("validation.response.body.missing",
                            swaggerOperation.getMethod(), swaggerOperation.getPathString().original())
            );
        }

        return validationReport.merge(schemaValidator.validate(body, swaggerResponse.getSchema()));
    }

}
