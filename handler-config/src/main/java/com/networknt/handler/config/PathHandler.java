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
 * Path Handler that maps to the paths in handler.yml
 *
 * @author Nicholas Azar
 */
public class PathHandler {
    private String handlerName;
    private List<HandlerPath> paths;

    /**
     * Constructor
     */
    public PathHandler() {
    }

    /**
     * Get the list of handler paths
     * @return list of handler paths
     */
    public List<HandlerPath> getPaths() {
        return paths;
    }

    /**
     * Set the list of handler paths
     * @param paths list of handler paths
     */
    public void setPaths(List<HandlerPath> paths) {
        this.paths = paths;
    }

    /**
     * Get the handler name
     * @return handler name
     */
    public String getHandlerName() {
        return handlerName;
    }

    /**
     * Set the handler name
     * @param handlerName handler name
     */
    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }
}
