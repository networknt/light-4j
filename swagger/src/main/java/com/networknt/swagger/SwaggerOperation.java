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

package com.networknt.swagger;

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
    private String endpoint;
    private String clientId;

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

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
