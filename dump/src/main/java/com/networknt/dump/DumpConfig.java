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
import com.networknt.server.ModuleRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * this class is to load dump.yml config file, and map settings to properties of this class.
 */
@ConfigSchema(
        configKey = "dump",
        configName = "dump",
        configDescription = "Dump middleware configuration.",
        outputFormats = {
                OutputFormat.JSON_SCHEMA,
                OutputFormat.YAML,
                OutputFormat.CLOUD
        }
)
public class DumpConfig {
    /** dump configuration name */
    public static final String CONFIG_NAME = "dump";

    @BooleanField(
            configFieldName = "enabled",
            externalizedKeyName = "enabled",
            description = "Indicate if the dump middleware is enabled or not. It should only be enabled in test environment.",
            defaultValue = "false"
    )
    private boolean enabled = false;

    @BooleanField(
            configFieldName = "mask",
            externalizedKeyName = "mask",
            description = "Indicate if the dump middleware should mask sensitive data.",
            defaultValue = "false"
    )
    private boolean mask = false;

    @StringField(
            configFieldName = "logLevel",
            externalizedKeyName = "logLevel",
            defaultValue = "INFO",
            pattern = "^(TRACE|DEBUG|INFO|WARN|ERROR)$",
            description = "The log level for the dump middleware. ERROR | WARN | INFO | DEBUG | TRACE"
    )
    private String logLevel = "INFO";

    @IntegerField(
            configFieldName = "indentSize",
            externalizedKeyName = "indentSize",
            defaultValue = "4",
            description = "The indent size for the dump middleware."
    )
    private int indentSize;

    @BooleanField(
            configFieldName = "useJson",
            externalizedKeyName = "useJson",
            description = "Indicate if the dump middleware should use JSON format. If use json, indentSize option will be ignored.",
            defaultValue = "false"
    )
    private boolean useJson;

    @BooleanField(
            configFieldName = "requestEnabled",
            externalizedKeyName = "requestEnabled",
            description = "Indicate if the dump middleware should dump request.",
            defaultValue = "false"
    )
    private boolean requestEnabled;

    @ObjectField(
            configFieldName = "request",
            externalizedKeyName = "request",
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
            description = "Indicate if the dump middleware should dump response.",
            defaultValue = "false"
    )
    private boolean responseEnabled;

    @ObjectField(
            configFieldName = "response",
            externalizedKeyName = "response",
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



    private Map<String, Object> mappedConfig;

    private static volatile DumpConfig instance;

    private DumpConfig(String configName) {
        mappedConfig = Config.getInstance().getJsonMapConfig(configName);
        setConfigData();
    }

    private DumpConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Loads the DumpConfig for the default config name.
     * @return DumpConfig instance
     */
    public static DumpConfig load() {
        return load(CONFIG_NAME);
    }

    /**
     * Loads the DumpConfig for a specific config name.
     * @param configName config name
     * @return DumpConfig instance
     */
    public static DumpConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (DumpConfig.class) {
                mappedConfig = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new DumpConfig(configName);
                // Register the module with the configuration.
                ModuleRegistry.registerModule(configName, DumpConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), null);
                return instance;
            }
        }
        return new DumpConfig(configName);
    }

    /**
     * Sets the response configuration.
     * @param response Map of response configuration
     */
    public void setResponse(Map<String, Object> response) {
        final var mapper = Config.getInstance().getMapper();
        this.response = mapper.convertValue(response, new TypeReference<>() {
        });
    }


    /**
     * Sets the request configuration.
     * @param request Map of request configuration
     */
    public void setRequest(Map<String, Object> request) {
        final var mapper = Config.getInstance().getMapper();
        this.request = mapper.convertValue(request, new TypeReference<>() {
        });
    }

    private boolean loadEnableConfig(Map<String, Object> config, String optionName) {
        return config.get(optionName) instanceof Boolean ? (Boolean) config.get(optionName) : DEFAULT;
    }

    private List<String> loadFilterConfig(Map<String, Object> config, String filterOptionName) {
        return config.get(filterOptionName) instanceof List ? (List<String>) config.get(filterOptionName) : new ArrayList();
    }

    /**
     * Checks if the dump middleware is enabled.
     * @return boolean true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Checks if request dumping is enabled.
     * @return boolean true if enabled
     */
    public boolean isRequestEnabled() {
        return isEnabled() && requestEnabled;
    }

    /**
     * Checks if response dumping is enabled.
     * @return boolean true if enabled
     */
    public boolean isResponseEnabled() {
        return isEnabled() && responseEnabled;
    }

    //auto-generated
    /**
     * Sets if the dump middleware is enabled.
     * @param enabled boolean true if enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Checks if masking is enabled.
     * @return boolean true if enabled
     */
    public boolean isMaskEnabled() {
        return mask;
    }

    /**
     * Sets if masking is enabled.
     * @param mask boolean true if enabled
     */
    public void setMask(boolean mask) {
        this.mask = mask;
    }

    /**
     * Gets the log level.
     * @return String log level
     */
    public String getLogLevel() {
        return logLevel;
    }

    /**
     * Sets the log level.
     * @param logLevel String log level
     */
    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * Gets the indent size.
     * @return int indent size
     */
    public int getIndentSize() {
        return indentSize;
    }

    /**
     * Sets the indent size.
     * @param indentSize int indent size
     */
    public void setIndentSize(int indentSize) {
        this.indentSize = indentSize;
    }

    /**
     * Checks if JSON format is used.
     * @return boolean true if enabled
     */
    public boolean isUseJson() {
        return useJson;
    }

    /**
     * Sets if JSON format should be used.
     * @param useJson boolean true if enabled
     */
    public void setUseJson(boolean useJson) {
        this.useJson = useJson;
    }

    /**
     * @deprecated since 2.2.1
     * Gets the request configuration Map.
     * @return Map of request configuration
     */
    @Deprecated(since = "2.2.1")
    public Map<String, Object> getRequest() {
        final var mapper = Config.getInstance().getMapper();
        return mapper.convertValue(request, new TypeReference<>() {
        });
    }

    /**
     * @deprecated since 2.2.1
     * Gets the response configuration Map.
     * @return Map of response configuration
     */
    @Deprecated(since = "2.2.1")
    public Map<String, Object> getResponse() {
        final var mapper = Config.getInstance().getMapper();
        return mapper.convertValue(response, new TypeReference<>() {
        });
    }


    /**
     * Checks if request URL dumping is enabled.
     * @return boolean true if enabled
     */
    public boolean isRequestUrlEnabled() {
        return request.isUrl();
    }

    /**
     * Checks if request header dumping is enabled.
     * @return boolean true if enabled
     */
    public boolean isRequestHeaderEnabled() {
        return request.isHeaders();
    }

    /**
     * Gets filtered request headers.
     * @return List filtered request headers
     */
    public List<String> getRequestFilteredHeaders() {
        return request.getFilteredHeaders();
    }

    /**
     * Checks if request cookie dumping is enabled.
     * @return boolean true if enabled
     */
    public boolean isRequestCookieEnabled() {
        return request.isCookies();
    }

    /**
     * Gets filtered request cookies.
     * @return List filtered request cookies
     */
    public List<String> getRequestFilteredCookies() {
        return request.getFilteredCookies();
    }

    /**
     * Checks if request query parameter dumping is enabled.
     * @return boolean true if enabled
     */
    public boolean isRequestQueryParametersEnabled() {
        return request.isQueryParameters();
    }

    /**
     * Gets filtered request query parameters.
     * @return List filtered request query parameters
     */
    public List<String> getRequestFilteredQueryParameters() {
        return request.getFilteredQueryParameters();
    }

    /**
     * Checks if request body dumping is enabled.
     * @return boolean true if enabled
     */
    public boolean isRequestBodyEnabled() {
        return request.isBody();
    }

    /**
     * Checks if response header dumping is enabled.
     * @return boolean true if enabled
     */
    public boolean isResponseHeaderEnabled() {
        return response.isHeaders();
    }

    /**
     * Gets filtered response headers.
     * @return List filtered response headers
     */
    public List<String> getResponseFilteredHeaders() {
        return response.getFilteredHeaders();
    }

    /**
     * Checks if response cookie dumping is enabled.
     * @return boolean true if enabled
     */
    public boolean isResponseCookieEnabled() {
        return response.isCookies();
    }

    /**
     * Gets filtered response cookies.
     * @return List filtered response cookies
     */
    public List<String> getResponseFilteredCookies() {
        return response.getFilteredCookies();
    }

    /**
     * Checks if response status code dumping is enabled.
     * @return boolean true if enabled
     */
    public boolean isResponseStatusCodeEnabled() {
        return response.isStatusCode();
    }

    /**
     * Checks if response body dumping is enabled.
     * @return boolean true if enabled
     */
    public boolean isResponseBodyEnabled() {
        return response.isBody();
    }

    /**
     * Gets the mapped configuration.
     * @return Map mapped configuration
     */
    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    /**
     * Sets the configuration data from the mapped config.
     */
    public void setConfigData() {
        if (mappedConfig != null) {
            Object object = mappedConfig.get("enabled");
            if (object != null) enabled = Config.loadBooleanValue("enabled", object);
            object = mappedConfig.get("mask");
            if (object != null) mask = Config.loadBooleanValue("mask", object);
            object = mappedConfig.get("logLevel");
            if (object != null) logLevel = (String) object;
            object = mappedConfig.get("indentSize");
            if (object != null) indentSize = Config.loadIntegerValue("indentSize", object);
            object = mappedConfig.get("useJson");
            if (object != null) useJson = Config.loadBooleanValue("useJson", object);
            object = mappedConfig.get("requestEnabled");
            if (object != null) requestEnabled = Config.loadBooleanValue("requestEnabled", object);
            object = mappedConfig.get("responseEnabled");
            if (object != null) responseEnabled = Config.loadBooleanValue("responseEnabled", object);

            object = mappedConfig.get("request");
            if (object != null) {
                request = Config.getInstance().getMapper().convertValue(object, DumpRequestConfig.class);
            }
            object = mappedConfig.get("response");
            if (object != null) {
                response = Config.getInstance().getMapper().convertValue(object, DumpResponseConfig.class);
            }
        }
    }
}
