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

    URL createCopy();

    String getProtocol();
    void setProtocol(String protocol);

    String getHost();
    void setHost(String host);

    Integer getPort();
    void setPort(int port);

    String getPath();
    void setPath(String path);

    String getVersion();
    String getGroup();

    Map<String, String> getParameters();
    void setParameters(Map<String, String> parameters);
    void addParameters(Map<String, String> params);
    String getParameter(String name);
    String getParameter(String name, String defaultValue);

    String getMethodParameter(String methodName, String paramDesc, String name);
    String getMethodParameter(String methodName, String paramDesc, String name, String defaultValue);

    Boolean getBooleanParameter(String name, boolean defaultValue);
    Boolean getMethodParameter(String methodName, String paramDesc, String name, boolean defaultValue);

    Integer getIntParameter(String name, int defaultValue);
    Integer getMethodParameter(String methodName, String paramDesc, String name, int defaultValue);

    String getUri();
    String getIdentity();
    String toFullStr();
    String toSimpleString();
    boolean canServe(URL refUrl);

    void addParameter(String name, String value);
    void addParameterIfAbsent(String name, String value);
    void removeParameter(String name);
    boolean hasParameter(String key);

    String getServerPortStr();

}
