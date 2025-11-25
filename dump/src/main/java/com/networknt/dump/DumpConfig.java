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

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.schema.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * this class is to load dump.yml config file, and map settings to properties of this class.
 */
@ConfigSchema(configKey = "dump",
        configName = "dump",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD},
        configDescription = "Dump middleware configuration.")
public class DumpConfig {
    public static final String CONFIG_NAME = "dump";

    @BooleanField(
            configFieldName = "enabled",
            externalizedKeyName = "enabled",
            externalized = true,
            description = "Indicate if the dump middleware is enabled or not. It should only be enabled in test environment.",
            defaultValue = "false"
    )
    private boolean enabled = false;

    @BooleanField(
            configFieldName = "mask",
            externalizedKeyName = "mask",
            externalized = true,
            description = "Indicate if the dump middleware should mask sensitive data.",
            defaultValue = "false"
    )
    private boolean mask = false;

    @StringField(
            configFieldName = "logLevel",
            externalizedKeyName = "logLevel",
            defaultValue = "INFO",
            pattern = "^(TRACE|DEBUG|INFO|WARN|ERROR)$",
            externalized = true,
            description = "The log level for the dump middleware. ERROR | WARN | INFO | DEBUG | TRACE"
    )
    private String logLevel = "INFO";

    @IntegerField(
            configFieldName = "indentSize",
            externalizedKeyName = "indentSize",
            defaultValue = "4",
            externalized = true,
            description = "The indent size for the dump middleware."
    )
    private int indentSize;

    @BooleanField(
            configFieldName = "useJson",
            externalizedKeyName = "useJson",
            externalized = true,
            description = "Indicate if the dump middleware should use JSON format. If use json, indentSize option will be ignored.",
            defaultValue = "false"
    )
    private boolean useJson;

    @BooleanField(
            configFieldName = "requestEnabled",
            externalizedKeyName = "requestEnabled",
            externalized = true,
            description = "Indicate if the dump middleware should dump request.",
            defaultValue = "false"
    )
    private boolean requestEnabled;

    @ObjectField(
            configFieldName = "request",
            externalizedKeyName = "request",
            externalized = true,
            description = "The request settings for the dump middleware.\n" +
                    "request:\n" +
                    "  url: true\n" +
                    "  headers: true\n" +
                    "  #filter for headers\n" +
                    "  filteredHeaders:\n" +
                    "  - Postman-Token\n" +
                    "  - X-Correlation-Id\n" +
                    "  - cookie\n" +
                    "  cookies: true\n" +
                    "  #filter for cookies\n" +
                    "  filteredCookies:\n" +
                    "  - Cookie_Gmail\n" +
                    "  queryParameters: true\n" +
                    "  #filter for queryParameters\n" +
                    "  filteredQueryParameters:\n" +
                    "  - itemId\n" +
                    "  - a\n" +
                    "  body: true\n",
            ref = DumpRequestConfig.class
    )
    private DumpRequestConfig request;

    @BooleanField(
            configFieldName = "responseEnabled",
            externalizedKeyName = "responseEnabled",
            externalized = true,
            description = "Indicate if the dump middleware should dump response.",
            defaultValue = "false"
    )
    private boolean responseEnabled;

    @ObjectField(
            configFieldName = "response",
            externalizedKeyName = "response",
            externalized = true,
            description = "The response settings for the dump middleware.\n" +
                    "response:\n" +
                    "  headers: true\n" +
                    "  cookies: true\n" +
                    "  body: true\n" +
                    "  statusCode: true\n",
            ref = DumpResponseConfig.class
    )
    private DumpResponseConfig response;

    private static Boolean DEFAULT = false;

    //request settings:
//    private boolean requestUrlEnabled ;
//    private boolean requestHeaderEnabled ;
//    private List<String> requestFilteredHeaders;
//    private boolean requestCookieEnabled;
//    private List<String> requestFilteredCookies;
//    private boolean requestQueryParametersEnabled;
//    private List<String> requestFilteredQueryParameters;
//    private boolean requestBodyEnabled;
//
//    //response settings:
//    private boolean responseHeaderEnabled;
//    private List<String> responseFilteredHeaders;
//    private boolean responseCookieEnabled;
//    private List<String> responseFilteredCookies;
//    private boolean responseStatusCodeEnabled;
//    private boolean responseBodyEnabled;


    public void setResponse(Map<String, Object> response) {
        final var mapper = Config.getInstance().getMapper();
        this.response = mapper.convertValue(response, new TypeReference<>() {});
    }


    public void setRequest(Map<String, Object> request) {
        final var mapper = Config.getInstance().getMapper();
        this.request = mapper.convertValue(request, new TypeReference<>() {});
    }

//    private void loadRequestConfig(Map<String, Object> request) {
//        this.requestBodyEnabled = loadEnableConfig(request, DumpConstants.BODY);
//        this.requestCookieEnabled = loadEnableConfig(request, DumpConstants.COOKIES);
//        this.requestHeaderEnabled = loadEnableConfig(request, DumpConstants.HEADERS);
//        this.requestQueryParametersEnabled = loadEnableConfig(request, DumpConstants.QUERY_PARAMETERS);
//        this.requestUrlEnabled = loadEnableConfig(request, DumpConstants.URL);
//        this.requestFilteredCookies = loadFilterConfig(request, DumpConstants.FILTERED_COOKIES);
//        this.requestFilteredHeaders = loadFilterConfig(request, DumpConstants.FILTERED_HEADERS);
//        this.requestFilteredQueryParameters = loadFilterConfig(request, DumpConstants.FILTERED_QUERY_PARAMETERS);
//    }
//
//    private void loadResponseConfig(Map<String, Object> response) {
//        this.responseBodyEnabled = loadEnableConfig(response, DumpConstants.BODY);
//        this.responseCookieEnabled = loadEnableConfig(response, DumpConstants.COOKIES);
//        this.responseHeaderEnabled = loadEnableConfig(response, DumpConstants.HEADERS);
//        this.responseStatusCodeEnabled = loadEnableConfig(response, DumpConstants.STATUS_CODE);
//        this.responseFilteredCookies = loadFilterConfig(response, DumpConstants.FILTERED_COOKIES);
//        this.responseFilteredHeaders = loadFilterConfig(response, DumpConstants.HEADERS);
//    }

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
        return isEnabled() && !this.requestEnabled;
    }

    public boolean isResponseEnabled() {
        return isEnabled() && !this.responseEnabled;
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

    @Deprecated(since = "2.2.1")
    public Map<String, Object> getRequest() {
        final var mapper = Config.getInstance().getMapper();
        return mapper.convertValue(request, new TypeReference<>() {});
    }

    @Deprecated(since = "2.2.1")
    public Map<String, Object> getResponse() {
        final var mapper = Config.getInstance().getMapper();
        return mapper.convertValue(response, new TypeReference<>() {});
    }


    public boolean isRequestUrlEnabled() {
        return request.isUrl();
    }

    public boolean isRequestHeaderEnabled() {
        return request.isHeaders();
    }

    public List<String> getRequestFilteredHeaders() {
        return request.getFilteredHeaders();
    }

    public boolean isRequestCookieEnabled() {
        return request.isCookies();
    }

    public List<String> getRequestFilteredCookies() {
        return request.getFilteredCookies();
    }

    public boolean isRequestQueryParametersEnabled() {
        return request.isQueryParameters();
    }

    public List<String> getRequestFilteredQueryParameters() {
        return request.getFilteredQueryParameters();
    }

    public boolean isRequestBodyEnabled() {
        return request.isBody();
    }

    public boolean isResponseHeaderEnabled() {
        return response.isHeaders();
    }

    public List<String> getResponseFilteredHeaders() {
        return response.getFilteredHeaders();
    }

    public boolean isResponseCookieEnabled() {
        return response.isCookies();
    }

    public List<String> getResponseFilteredCookies() {
        return response.getFilteredCookies();
    }

    public boolean isResponseStatusCodeEnabled() {
        return response.isStatusCode();
    }

    public boolean isResponseBodyEnabled() {
        return response.isBody();
    }
}
