/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.networknt.registry;

import java.util.Map;

/**
 * Describe a service in registry.
 *
 * All parameter retrieve methods(getXXX method) must return an object. This is to prevent
 * value modification accidentally.
 *
 * If there is no defaultValue, return null for getXXX
 *
 * @author fishermen, stevehu
 */

public interface URL {

    /**
     * Creates a copy of the URL.
     *
     * @return URL copy
     */
    URL createCopy();


    /**
     * Gets the protocol of the URL.
     *
     * @return String protocol
     */
    String getProtocol();

    /**
     * Sets the protocol of the URL.
     *
     * @param protocol protocol
     */
    void setProtocol(String protocol);


    /**
     * Gets the host of the URL.
     *
     * @return String host
     */
    String getHost();

    /**
     * Sets the host of the URL.
     *
     * @param host host
     */
    void setHost(String host);


    /**
     * Gets the port of the URL.
     *
     * @return Integer port
     */
    Integer getPort();

    /**
     * Sets the port of the URL.
     *
     * @param port port
     */
    void setPort(int port);


    /**
     * Gets the path of the URL.
     *
     * @return String path
     */
    String getPath();

    /**
     * Sets the path of the URL.
     *
     * @param path path
     */
    void setPath(String path);


    /**
     * Gets the version parameter.
     *
     * @return String version
     */
    String getVersion();

    /**
     * Gets the group parameter.
     *
     * @return String group
     */
    String getGroup();


    /**
     * Gets the parameters map.
     *
     * @return Map of parameters
     */
    Map<String, String> getParameters();

    /**
     * Sets the parameters map.
     *
     * @param parameters parameters
     */
    void setParameters(Map<String, String> parameters);

    /**
     * Adds multiple parameters.
     *
     * @param params parameters to add
     */
    void addParameters(Map<String, String> params);

    /**
     * Gets a parameter by name.
     *
     * @param name parameter name
     * @return String parameter value
     */
    String getParameter(String name);

    /**
     * Gets a parameter by name with a default value.
     *
     * @param name         parameter name
     * @param defaultValue default value
     * @return String parameter value
     */
    String getParameter(String name, String defaultValue);


    /**
     * Gets a parameter by name, with method and parameter description context.
     *
     * @param methodName methodName
     * @param paramDesc paramDesc
     * @param name parameter name
     * @return String parameter value
     */
    String getMethodParameter(String methodName, String paramDesc, String name);

    /**
     * Gets a parameter by name with a default value, with method and parameter description context.
     *
     * @param methodName   methodName
     * @param paramDesc    paramDesc
     * @param name         parameter name
     * @param defaultValue default value
     * @return String parameter value
     */
    String getMethodParameter(String methodName, String paramDesc, String name, String defaultValue);


    /**
     * Gets a boolean parameter by name with a default value.
     *
     * @param name         parameter name
     * @param defaultValue default value
     * @return Boolean parameter value
     */
    Boolean getBooleanParameter(String name, boolean defaultValue);

    /**
     * Gets a boolean parameter by name with a default value, with method and parameter description context.
     *
     * @param methodName   methodName
     * @param paramDesc    paramDesc
     * @param name         parameter name
     * @param defaultValue default value
     * @return Boolean parameter value
     */
    Boolean getMethodParameter(String methodName, String paramDesc, String name, boolean defaultValue);


    /**
     * Gets an integer parameter by name with a default value.
     *
     * @param name         parameter name
     * @param defaultValue default value
     * @return Integer parameter value
     */
    Integer getIntParameter(String name, int defaultValue);

    /**
     * Gets an integer parameter by name with a default value, with method and parameter description context.
     *
     * @param methodName   methodName
     * @param paramDesc    paramDesc
     * @param name         parameter name
     * @param defaultValue default value
     * @return Integer parameter value
     */
    Integer getMethodParameter(String methodName, String paramDesc, String name, int defaultValue);


    /**
     * Gets a string representation of the URI.
     *
     * @return String URI
     */
    String getUri();

    /**
     * Gets the identity string of the URL.
     *
     * @return String identity
     */
    String getIdentity();

    /**
     * Returns a full string representation of the URL with all parameters.
     *
     * @return String full representation
     */
    String toFullStr();

    /**
     * Returns a simple string representation of the URL.
     *
     * @return String simple representation
     */
    String toSimpleString();

    /**
     * Checks if this URL can serve the given reference URL.
     *
     * @param refUrl reference URL
     * @return boolean true if it can serve
     */
    boolean canServe(URL refUrl);

    /**
     * Adds a parameter by name and value.
     *
     * @param name parameter name
     * @param value parameter value
     */
    void addParameter(String name, String value);

    /**
     * Adds a parameter by name and value if it's absent.
     *
     * @param name parameter name
     * @param value parameter value
     */
    void addParameterIfAbsent(String name, String value);

    /**
     * Checks if a parameter exists and is not empty.
     *
     * @param key parameter name
     * @return true if parameter exists
     */
    boolean hasParameter(String key);

    /**
     * Returns the server port string (host:port).
     *
     * @return String server port
     */
    String getServerPortStr();

    /**
     * Removes a parameter by name.
     *
     * @param name parameter name
     */
    void removeParameter(String name);

}
