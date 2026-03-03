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

import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.StringField;
import com.networknt.utility.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Path Chain that maps to the paths in handler.yml
 *
 * @author Nicholas Azar
 */
public class PathChain {

    private String source;

    @StringField(
            configFieldName = "path",
            description = "The path to match",
            pattern = "^/.*"
    )
    private String path;

    @StringField(
            configFieldName = "method",
            description = "The HTTP method to match",
            pattern = "(?i)^(GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD|TRACE|CONNECT)$"
    )
    private String method;

    @ArrayField(
            configFieldName = "exec",
            description = "The list of handlers",
            items = String.class
    )
    private List<String> exec;

    /**
     * Constructor
     */
    public PathChain() {
    }

    /**
     * Get the source
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * Set the source
     * @param source the source
     */
    public void setSource(String source) {
        this.source = source;
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
     * @param path the path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Get the execution list
     * @return list of handlers
     */
    public List<String> getExec() {
        return exec;
    }

    /**
     * Set the execution list
     * @param exec list of handlers
     */
    public void setExec(List<String> exec) {
        this.exec = exec;
    }

    /**
     * Get the method
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Set the method
     * @param method the method
     */
    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String toString() {

        if (path != null)
            return path + "@" + method + " → " + exec;

        else return source + "() → " + exec;
    }

    /**
     * Validate the settings and raise Exception on error.
     * The origin is used to help locate problems.
     *
     * @param origin the origin
     */
    public void validate(String origin) {
        List<String> problems = new ArrayList<>();

        if (source == null) {

            if (path == null) {
                problems.add("You must specify either path or source");
                problems.add("It is possible that serviceId is missing from the values.yml and it is mandatory.");

            } else if (method == null)
                problems.add("You must specify method along with path: " + path);

        } else {

            if (path != null)
                problems.add("Conflicting source: " + source + " and path: " + path);

            if (method != null)
                problems.add("Conflicting source: " + source + " and method: " + method);
        }

        if (method != null && !Util.METHODS.contains(method.toUpperCase()))
            problems.add("Invalid HTTP method: " + method);

        if (!problems.isEmpty())
            throw new RuntimeException("Bad paths element in " + origin + " [ " + String.join(" | ", problems) + " ]");

    }
}
