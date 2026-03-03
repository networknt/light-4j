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

package com.networknt.handler.config;

import java.util.List;

/**
 * Named Middleware Chain that maps to the named middleware chains in handler.yml
 *
 * @author Nicholas Azar
 */
public class NamedMiddlewareChain {
    private String name;
    private List<Object> middleware;

    /**
     * Constructor
     */
    public NamedMiddlewareChain() {
    }

    /**
     * Get the name of the chain
     * @return name of the chain
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the chain
     * @param name name of the chain
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the list of middleware handlers
     * @return list of middleware handlers
     */
    public List<Object> getMiddleware() {
        return middleware;
    }

    /**
     * Set the list of middleware handlers
     * @param middleware list of middleware handlers
     */
    public void setMiddleware(List<Object> middleware) {
        this.middleware = middleware;
    }
}
