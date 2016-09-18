package com.networknt.validator;

import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;

import static java.util.Objects.requireNonNull;

/**
 * A container representing a single API operation.
 * <p>
 * This includes the path, method and operation components from the OAI spec object. Used as a
 * convenience to hold related information in one place.
 */
public class SwaggerOperation {
    private final NormalisedPath pathString;
    private final Path pathObject;
    private final HttpMethod method;
    private final Operation operation;

    public SwaggerOperation(final NormalisedPath pathString, final Path pathObject,
                            final HttpMethod method, final Operation operation) {

        this.pathString = requireNonNull(pathString, "A path string is required");
        this.pathObject = requireNonNull(pathObject, "A path object is required");
        this.method = requireNonNull(method, "A request method is required");
        this.operation = requireNonNull(operation, "A operation object is required");
    }

    /**
     * @return The path the operation is on
     */
    public NormalisedPath getPathString() {
        return pathString;
    }

    /**
     * @return The path object from the OAI specification
     */
    public Path getPathObject() {
        return pathObject;
    }

    /**
     * @return The method the operation is on
     */
    public HttpMethod getMethod() {
        return method;
    }

    /**
     * @return The operation object from the OAI specification
     */
    public Operation getOperation() {
        return operation;
    }
}
