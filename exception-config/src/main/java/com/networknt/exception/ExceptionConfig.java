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

package com.networknt.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.networknt.config.Config;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import com.networknt.server.ModuleRegistry;

import java.util.Map;

/**
 * Config class for Exception module to control the behavior
 *
 * @author  Steve Hu
 */
@ConfigSchema(
        configName = "exception",
        configKey = "exception",
        outputFormats = {
                OutputFormat.JSON_SCHEMA,
                OutputFormat.YAML,
                OutputFormat.CLOUD
        },
        configDescription = "Exception handler for runtime exception and ApiException if it is not handled by other handlers in the chain."
        )
public class ExceptionConfig {
    public static final String CONFIG_NAME = "exception";

    @BooleanField(
            configFieldName = "enabled",
            externalizedKeyName = "enabled",
            defaultValue = "true",
            description = "Enable or disable the exception module."
    )
    boolean enabled;

    @JsonIgnore
    String description;

    private Map<String, Object> mappedConfig;


    private static volatile ExceptionConfig instance;

    private ExceptionConfig(String configName) {
        mappedConfig = Config.getInstance().getJsonMapConfig(configName);
        setConfigData();
    }
    private ExceptionConfig() {
        this(CONFIG_NAME);
    }

    public static ExceptionConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (ExceptionConfig.class) {
                mappedConfig = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new ExceptionConfig(configName);
                // Register the module with the configuration.
                ModuleRegistry.registerModule(configName, ExceptionConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), null);
                return instance;
            }
        }
        return new ExceptionConfig(configName);
    }

    public static ExceptionConfig load() {
        return load(CONFIG_NAME);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }



    private void setConfigData() {
        if(getMappedConfig() != null) {
            Object object = getMappedConfig().get("enabled");
            if(object != null) enabled = Config.loadBooleanValue("enabled", object);
        }
    }
}
