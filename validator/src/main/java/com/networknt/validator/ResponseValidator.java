package com.networknt.validator;

import com.networknt.validator.report.MessageResolver;
import com.networknt.validator.report.MutableValidationReport;
import com.networknt.validator.report.ValidationReport;
import io.undertow.server.HttpServerExchange;

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
     * @param response The response to validate
     * @param apiOperation The API operation to validate the response against
     *
     * @return A validation report containing validation errors
     */
    /*
    public ValidationReport validateResponse(HttpServerExchange exchange, final ApiOperation apiOperation) {

        final NormalisedPath requestPath = new BaseNormalisedPath(exchange.getRequestURI());

        final Optional<NormalisedPath> maybeSwaggerPath = findMatchingSwaggerPath(requestPath);
        if (!maybeSwaggerPath.isPresent()) {
            result.add("Invalid request path " + exchange.getRequestURI());
            return result;
        }

        final NormalisedPath swaggerPathString = maybeSwaggerPath.get();
        final Path swaggerPath = SwaggerHelper.swagger.getPath(swaggerPathString.original());

        final HttpMethod httpMethod = HttpMethod.valueOf(exchange.getRequestMethod().toString());
        final Operation operation = swaggerPath.getOperationMap().get(httpMethod);
        if (operation == null) {
            result.add("Invalid method " + exchange.getRequestMethod() + " for request path " + exchange.getRequestURI());
            return result;
        }


        io.swagger.models.Response apiResponse = apiOperation.getOperation().getResponses().get(Integer.toString(response.getStatus()));
        if (apiResponse == null) {
            apiResponse = apiOperation.getOperation().getResponses().get("default"); // try the default response
        }

        final MutableValidationReport validationReport = new MutableValidationReport();
        if (apiResponse == null) {
            return validationReport.add(
                    messages.get("validation.response.status.unknown",
                            response.getStatus(), apiOperation.getPathString().original())
            );
        }

        if (apiResponse.getSchema() == null) {
            return validationReport;
        }

        if (!response.getBody().isPresent() || response.getBody().get().isEmpty()) {
            return validationReport.add(
                    messages.get("validation.response.body.missing",
                            apiOperation.getMethod(), apiOperation.getPathString().original())
            );
        }

        return validationReport.merge(schemaValidator.validate(response.getBody().get(), apiResponse.getSchema()));
    }
    */
}
