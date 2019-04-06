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

package com.networknt.dump;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * this class is to load dump.yml config file, and map settings to properties of this class.
 */
public class DumpConfig {
    private boolean enabled = false;
    private boolean mask = false;
    private String logLevel = "INFO";
    private int indentSize = 4;
    private boolean useJson = false;
    private Map<String, Object> request;
    private Map<String, Object> response;
    private static Boolean DEFAULT = false;

    //request settings:
    private boolean requestUrlEnabled ;
    private boolean requestHeaderEnabled ;
    private List<String> requestFilteredHeaders;
    private boolean requestCookieEnabled;
    private List<String> requestFilteredCookies;
    private boolean requestQueryParametersEnabled;
    private List<String> requestFilteredQueryParameters;
    private boolean requestBodyEnabled;

    //response settings:
    private boolean responseHeaderEnabled;
    private List<String> responseFilteredHeaders;
    private boolean responseCookieEnabled;
    private List<String> responseFilteredCookies;
    private boolean responseStatusCodeEnabled;
    private boolean responseBodyEnabled;


    public void setResponse(Map<String, Object> response) {
        this.response = response == null ? new HashMap<>() : response;
        loadResponseConfig(this.response);
    }

    public void setRequest(Map<String, Object> request) {
        this.request = request == null ? new HashMap<>() : request;
        loadRequestConfig(this.request);
    }

    private void loadRequestConfig(Map<String, Object> request) {
        this.requestBodyEnabled = loadEnableConfig(request, DumpConstants.BODY);
        this.requestCookieEnabled = loadEnableConfig(request, DumpConstants.COOKIES);
        this.requestHeaderEnabled = loadEnableConfig(request, DumpConstants.HEADERS);
        this.requestQueryParametersEnabled = loadEnableConfig(request, DumpConstants.QUERY_PARAMETERS);
        this.requestUrlEnabled = loadEnableConfig(request, DumpConstants.URL);
        this.requestFilteredCookies = loadFilterConfig(request, DumpConstants.FILTERED_COOKIES);
        this.requestFilteredHeaders = loadFilterConfig(request, DumpConstants.FILTERED_HEADERS);
        this.requestFilteredQueryParameters = loadFilterConfig(request, DumpConstants.FILTERED_QUERY_PARAMETERS);
    }

    private void loadResponseConfig(Map<String, Object> response) {
        this.responseBodyEnabled = loadEnableConfig(response, DumpConstants.BODY);
        this.responseCookieEnabled = loadEnableConfig(response, DumpConstants.COOKIES);
        this.responseHeaderEnabled = loadEnableConfig(response, DumpConstants.HEADERS);
        this.responseStatusCodeEnabled = loadEnableConfig(response, DumpConstants.STATUS_CODE);
        this.responseFilteredCookies = loadFilterConfig(response, DumpConstants.FILTERED_COOKIES);
        this.responseFilteredHeaders = loadFilterConfig(response, DumpConstants.HEADERS);
    }

    private boolean loadEnableConfig(Map<String, Object> config, String optionName) {
        return config.get(optionName) instanceof Boolean ? (Boolean)config.get(optionName) : DEFAULT;
    }

    private List<String> loadFilterConfig(Map<String, Object> config, String filterOptionName) {
        return config.get(filterOptionName) instanceof List ? (List<String>)config.get(filterOptionName) : new ArrayList();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRequestEnabled() {
        return isEnabled() && !request.isEmpty();
    }

    public boolean isResponseEnabled() {
        return isEnabled() && !response.isEmpty();
    }

    //auto-generated
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isMaskEnabled() {
        return mask;
    }

    public void setMask(boolean mask) {
        this.mask = mask;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public int getIndentSize() { return indentSize; }

    public void setIndentSize(int indentSize) {
        this.indentSize = indentSize;
    }

    public boolean isUseJson() {
        return useJson;
    }

    public void setUseJson(boolean useJson) {
        this.useJson = useJson;
    }

    public Map<String, Object> getRequest() {
        return request;
    }

    public Map<String, Object> getResponse() {
        return response;
    }

    public boolean isRequestUrlEnabled() {
        return requestUrlEnabled;
    }

    public boolean isRequestHeaderEnabled() {
        return requestHeaderEnabled;
    }

    public List<String> getRequestFilteredHeaders() {
        return requestFilteredHeaders;
    }

    public boolean isRequestCookieEnabled() {
        return requestCookieEnabled;
    }

    public List<String> getRequestFilteredCookies() {
        return requestFilteredCookies;
    }

    public boolean isRequestQueryParametersEnabled() {
        return requestQueryParametersEnabled;
    }

    public List<String> getRequestFilteredQueryParameters() {
        return requestFilteredQueryParameters;
    }

    public boolean isRequestBodyEnabled() {
        return requestBodyEnabled;
    }

    public boolean isResponseHeaderEnabled() {
        return responseHeaderEnabled;
    }

    public List<String> getResponseFilteredHeaders() {
        return responseFilteredHeaders;
    }

    public boolean isResponseCookieEnabled() {
        return responseCookieEnabled;
    }

    public List<String> getResponseFilteredCookies() {
        return responseFilteredCookies;
    }

    public boolean isResponseStatusCodeEnabled() {
        return responseStatusCodeEnabled;
    }

    public boolean isResponseBodyEnabled() {
        return responseBodyEnabled;
    }
}
