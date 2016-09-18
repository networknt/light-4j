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

}
