package com.networknt.validator;

import com.networknt.schema.ValidationMessage;
import com.networknt.security.SwaggerHelper;
import com.networknt.validator.parameter.ParameterValidators;
import com.networknt.validator.report.MessageResolver;
import com.networknt.validator.report.ValidationReport;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.undertow.server.HttpServerExchange;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/*

    public Set<String> validate(HttpServerExchange exchange) {
        Set<String> result = new HashSet<String>();
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


    }

 */

/**
 * Validate a request against a given API operation.
 */
public class RequestValidator {

    private final SchemaValidator schemaValidator;
    private final ParameterValidators parameterValidators;
    private final MessageResolver messages;

    /**
     * Construct a new request validator with the given schema validator.
     *
     * @param schemaValidator The schema validator to use when validating request bodies
     * @param messages The message resolver to use
     */
    public RequestValidator(final SchemaValidator schemaValidator, final MessageResolver messages) {
        this.schemaValidator = requireNonNull(schemaValidator, "A schema validator is required");
        this.parameterValidators = new ParameterValidators(schemaValidator, messages);
        this.messages = requireNonNull(messages, "A message resolver is required");
    }

    /**
     * Validate the request against the given API operation
     *
     * @param exchange The HttpServerExchange to validate
     * @return A validation report containing validation errors
     */
    public ValidationReport validateRequest(final NormalisedPath requestPath, HttpServerExchange exchange, SwaggerOperation swaggerOperation) {
        requireNonNull(requestPath, "A request path is required");
        requireNonNull(exchange, "An exchange is required");
        requireNonNull(swaggerOperation, "An swagger operation is required");

        return validatePathParameters(requestPath, swaggerOperation)
                .merge(validateRequestBody(Optional.ofNullable(new Scanner(exchange.getInputStream(),"UTF-8").useDelimiter("\\A").next()), swaggerOperation))
                .merge(validateQueryParameters(exchange, swaggerOperation));
    }

    private ValidationReport validateRequestBody(final Optional<String> requestBody,
                                                 final SwaggerOperation swaggerOperation) {
        final Optional<Parameter> bodyParameter = swaggerOperation.getOperation().getParameters()
                .stream().filter(p -> p.getIn().equalsIgnoreCase("body")).findFirst();

        if (requestBody.isPresent() && !requestBody.get().isEmpty() && !bodyParameter.isPresent()) {
            return ValidationReport.singleton(
                    messages.get("validation.request.body.unexpected",
                            swaggerOperation.getMethod(), swaggerOperation.getPathString().original())
            );
        }

        if (!bodyParameter.isPresent()) {
            return ValidationReport.empty();
        }

        if (!requestBody.isPresent() || requestBody.get().isEmpty()) {
            if (bodyParameter.get().getRequired()) {
                return ValidationReport.singleton(
                        messages.get("validation.request.body.missing",
                                swaggerOperation.getMethod(), swaggerOperation.getPathString().original())
                );
            }
            return ValidationReport.empty();
        }

        return schemaValidator.validate(requestBody.get(), ((BodyParameter)bodyParameter.get()).getSchema());
    }

    private ValidationReport validatePathParameters(final NormalisedPath requestPath,
                                                    final SwaggerOperation swaggerOperation) {

        ValidationReport validationReport = ValidationReport.empty();
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
                validationReport = validationReport.merge(parameterValidators.validate(paramValue, parameter.get()));
            }
        }
        return validationReport;
    }

    private ValidationReport validateQueryParameters(final HttpServerExchange exchange,
                                                     final SwaggerOperation swaggerOperation) {
        return swaggerOperation
                .getOperation()
                .getParameters()
                .stream()
                .filter(p -> p.getIn().equalsIgnoreCase("QUERY"))
                .map(p -> validateQueryParameter(exchange, swaggerOperation, p))
                .reduce(ValidationReport.empty(), ValidationReport::merge);
    }

    private ValidationReport validateQueryParameter(final HttpServerExchange exchange,
                                                    final SwaggerOperation swaggerOperation,
                                                    final Parameter queryParameter) {

        final Collection<String> queryParameterValues = exchange.getQueryParameters().get(queryParameter.getName());

        if (queryParameterValues.isEmpty() && queryParameter.getRequired()) {
            return ValidationReport.singleton(
                    messages.get("validation.request.parameter.query.missing",
                            queryParameter.getName(), swaggerOperation.getPathString().original())
            );
        }

        return queryParameterValues
                .stream()
                .map((v) -> parameterValidators.validate(v, queryParameter))
                .reduce(ValidationReport.empty(), ValidationReport::merge);
    }

}
