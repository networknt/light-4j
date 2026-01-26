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

package com.networknt.body;

import com.networknt.config.Config;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import com.networknt.server.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by steve on 29/09/16.
 */
@ConfigSchema(
        configKey = "body",
        configName = "body",
        configDescription = "The config for the body handler.",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
public class BodyConfig {
    private static final Logger logger = LoggerFactory.getLogger(BodyConfig.class);

    public static final String CONFIG_NAME = "body";
    private static final String ENABLED = "enabled";
    private static final String CACHE_REQUEST_BODY = "cacheRequestBody";
    private static final String CACHE_RESPONSE_BODY = "cacheResponseBody";
    private static final String LOG_FULL_REQUEST_BODY = "logFullRequestBody";
    private static final String LOG_FULL_RESPONSE_BODY = "logFullResponseBody";

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            description = "Enable body parse flag",
            defaultValue = "true"
    )
    boolean enabled;

    @BooleanField(
            configFieldName = CACHE_REQUEST_BODY,
            externalizedKeyName = CACHE_REQUEST_BODY,
            defaultValue = "false",
            description = "cache request body as a string along with JSON object. The string formatted request body will be used for audit log.\n" +
                          "you should only enable this if you have configured audit.yml to log the request body as it uses extra memory."
    )
    boolean cacheRequestBody;

    @BooleanField(
            configFieldName = LOG_FULL_REQUEST_BODY,
            externalizedKeyName = LOG_FULL_REQUEST_BODY,
            defaultValue = "false",
            description = "log the full request body when RequestBodyInterceptor is enabled. This is useful for troubleshooting but not recommended\n" +
                          "for production. The default value is false and only 16K of the request body will be logged."
    )
    boolean logFullRequestBody;

    @BooleanField(
            configFieldName = CACHE_RESPONSE_BODY,
            externalizedKeyName = CACHE_RESPONSE_BODY,
            defaultValue = "false",
            description = "cache response body as a string along with JSON object. The string formatted response body will be used for audit log.\n" +
                          "you should only enable this if you have configured audit.yml to log the response body as it uses extra memory."
    )
    boolean cacheResponseBody;

    @BooleanField(
            configFieldName = LOG_FULL_RESPONSE_BODY,
            externalizedKeyName = LOG_FULL_RESPONSE_BODY,
            defaultValue = "false",
            description = "log the full response body when ResponseBodyInterceptor is enabled. This is useful for troubleshooting but not recommended\n" +
                          "for production. The default value is false and only 16K of the response body will be logged."
    )
    boolean logFullResponseBody;

    private Map<String, Object> mappedConfig;
    private static volatile BodyConfig instance;

    private BodyConfig(String configName) {
        mappedConfig = Config.getInstance().getJsonMapConfig(configName);
        setConfigData();
    }

    private BodyConfig() {
        this(CONFIG_NAME);
    }

    public static BodyConfig load() {
        return load(CONFIG_NAME);
    }

    public static BodyConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (BodyConfig.class) {
                mappedConfig = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new BodyConfig(configName);
                // Register the module with the configuration.
                ModuleRegistry.registerModule(configName, BodyConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), null);
                return instance;
            }
        }
        return new BodyConfig(configName);
    }


    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isCacheRequestBody() {
        return cacheRequestBody;
    }
    public boolean isCacheResponseBody() {
        return cacheResponseBody;
    }

    public boolean isLogFullRequestBody() { return logFullRequestBody; }

    public boolean isLogFullResponseBody() { return logFullResponseBody; }

    private void setConfigData() {
        if(mappedConfig != null) {
            Object object = mappedConfig.get(ENABLED);
            if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
            object = mappedConfig.get(CACHE_REQUEST_BODY);
            if(object != null) cacheRequestBody = Config.loadBooleanValue(CACHE_REQUEST_BODY, object);
            object = mappedConfig.get(CACHE_RESPONSE_BODY);
            if(object != null) cacheResponseBody = Config.loadBooleanValue(CACHE_RESPONSE_BODY, object);
            object = mappedConfig.get(LOG_FULL_REQUEST_BODY);
            if(object != null) logFullRequestBody = Config.loadBooleanValue(LOG_FULL_REQUEST_BODY, object);
            object = mappedConfig.get(LOG_FULL_RESPONSE_BODY);
            if(object != null) logFullResponseBody = Config.loadBooleanValue(LOG_FULL_RESPONSE_BODY, object);
        }
    }
}
