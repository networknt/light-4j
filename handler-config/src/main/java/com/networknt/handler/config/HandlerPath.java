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
 * Handler Path that maps to the paths in handler.yml
 *
 * @author Nicholas Azar
 */
public class HandlerPath {
    private String path;
    private String httpVerb;
    private List<Object> middleware;
    private Object endPoint;
    private String namedMiddlewareChain;

    /**
     * Constructor
     */
    public HandlerPath() {
    }

    /**
     * Get the path
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Set the path
     * @param path request path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Get the http verb
     * @return the http verb
     */
    public String getHttpVerb() {
        return httpVerb;
    }

    /**
     * Set the http verb
     * @param httpVerb http verb
     */
    public void setHttpVerb(String httpVerb) {
        this.httpVerb = httpVerb;
    }

    /**
     * Get the middleware list
     * @return list of middleware
     */
    public List<Object> getMiddleware() {
        return middleware;
    }

    /**
     * Set the middleware list
     * @param middleware list of middleware
     */
    public void setMiddleware(List<Object> middleware) {
        this.middleware = middleware;
    }

    /**
     * Get the endpoint object
     * @return the end point
     */
    public Object getEndPoint() {
        return endPoint;
    }

    /**
     * Set the endpoint object
     * @param endPoint endpoint object
     */
    public void setEndPoint(Object endPoint) {
        this.endPoint = endPoint;
    }

    /**
     * Get the named middleware chain
     * @return the named middleware chain
     */
    public String getNamedMiddlewareChain() {
        return namedMiddlewareChain;
    }

    /**
     * Set the named middleware chain
     * @param namedMiddlewareChain named middleware chain
     */
    public void setNamedMiddlewareChain(String namedMiddlewareChain) {
        this.namedMiddlewareChain = namedMiddlewareChain;
    }
}
