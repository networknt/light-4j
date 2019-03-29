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
 * @author Nicholas Azar
 */
public class HandlerPath {
    private String path;
    private String httpVerb;
    private List<Object> middleware;
    private Object endPoint;
    private String namedMiddlewareChain;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHttpVerb() {
        return httpVerb;
    }

    public void setHttpVerb(String httpVerb) {
        this.httpVerb = httpVerb;
    }

    public List<Object> getMiddleware() {
        return middleware;
    }

    public void setMiddleware(List<Object> middleware) {
        this.middleware = middleware;
    }

    public Object getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(Object endPoint) {
        this.endPoint = endPoint;
    }

    public String getNamedMiddlewareChain() {
        return namedMiddlewareChain;
    }

    public void setNamedMiddlewareChain(String namedMiddlewareChain) {
        this.namedMiddlewareChain = namedMiddlewareChain;
    }
}
