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

package com.networknt.config.reload.model;

import com.networknt.config.Config;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.server.ModuleRegistry;

import java.util.Map;

/**
 * Config class for Config reload related handlers
 */
@ConfigSchema(configKey = "configReload", configName = "configReload", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD})
public class ConfigReloadConfig {
    private static final Logger logger = LoggerFactory.getLogger(ConfigReloadConfig.class);
    public static final String CONFIG_NAME = "configReload";

    private static final String ENABLED = "enabled";
    private final Map<String, Object> mappedConfig;
    private final Config config;
    private static ConfigReloadConfig instance;

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            defaultValue = "true",
            description = "config reload from config server.\n" +
            "Indicate if the config reload from config server  is enabled or not."
    )
    boolean enabled;

    private ConfigReloadConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        setConfigData();
    }

    private ConfigReloadConfig() {
        this(CONFIG_NAME);
    }

    public static ConfigReloadConfig load() {
        return load(CONFIG_NAME);
    }

    public static ConfigReloadConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> config = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == config) {
                return instance;
            }
            synchronized (ConfigReloadConfig.class) {
                config = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == config) {
                    return instance;
                }
                instance = new ConfigReloadConfig(configName);
                ModuleRegistry.registerModule(configName, ConfigReloadConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), null);
                return instance;
            }
        }
        return new ConfigReloadConfig(configName);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    Config getConfig() {
        return config;
    }

    private void setConfigData() {
        if(getMappedConfig() != null) {
            Object object = getMappedConfig().get(ENABLED);
            if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
        }
    }

}
