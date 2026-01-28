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

package com.networknt.deref;

import com.networknt.config.Config;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import com.networknt.server.ModuleRegistry;

import java.util.Map;

/**
 * The config class that maps to deref.yml
 *
 * @author Steve Hu
 */
@ConfigSchema(configKey = "deref", configName = "deref", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD})
public class DerefConfig {
    public static final String CONFIG_NAME = "deref";

    @BooleanField(
        configFieldName = "enabled",
        externalizedKeyName = "enabled",
        description = "indicate if the deref handler is enabled or not.",
        defaultValue = "false"
    )
    boolean enabled;

    private final Map<String, Object> mappedConfig;

    private static volatile DerefConfig instance;

    private DerefConfig(String configName) {
        mappedConfig = Config.getInstance().getJsonMapConfig(configName);
        setConfigData();
    }

    private DerefConfig() {
        this(CONFIG_NAME);
    }

    public static DerefConfig load() {
        return load(CONFIG_NAME);
    }

    public static DerefConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (DerefConfig.class) {
                mappedConfig = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new DerefConfig(configName);
                // Register the module with the configuration.
                ModuleRegistry.registerModule(configName, DerefConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), null);
                return instance;
            }
        }
        return new DerefConfig(configName);
    }

    public void setConfigData() {
        if(mappedConfig != null) {
            Object object = mappedConfig.get("enabled");
            if(object != null) enabled = Config.loadBooleanValue("enabled", object);
        }
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
