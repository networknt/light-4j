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

package com.networknt.metrics.prometheus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import com.networknt.server.ModuleRegistry;

import java.util.Map;

/**
 * Prometheus metrics middleware handler configuration that is mapped to all
 * properties in metrics.yml config file.
 *
 * @author Gavin Chen
 */
@ConfigSchema(
        configName = "prometheus",
        configKey = "prometheus",
        configDescription = "Prometheus Metrics handler configuration.",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
public class PrometheusConfig {
    public static final String CONFIG_NAME = "prometheus";

    @BooleanField(
        configFieldName = "enabled",
        externalizedKeyName = "enabled",
        defaultValue = "false",
        description = "If metrics handler is enabled or not"
    )
    boolean enabled;

    @BooleanField(
            configFieldName = "enableHotspot",
            externalizedKeyName = "enableHotspot",
            defaultValue = "false",
            description = "If the Prometheus hotspot is enabled or not.\n" +
                    "hotspot include thread, memory, classloader,..."
    )
    boolean enableHotspot;

    @JsonIgnore
    String description;

    private static volatile PrometheusConfig instance;
    private final com.networknt.config.Config config;
    private java.util.Map<String, Object> mappedConfig;

    private PrometheusConfig(String configName) {
        config = com.networknt.config.Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        if (mappedConfig != null) {
            setConfigData();
        }
    }

    private PrometheusConfig() {
        this(CONFIG_NAME);
    }

    public static PrometheusConfig load() {
        return load(CONFIG_NAME);
    }

    public static PrometheusConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = com.networknt.config.Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (PrometheusConfig.class) {
                mappedConfig = com.networknt.config.Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new PrometheusConfig(configName);
                ModuleRegistry.registerModule(CONFIG_NAME, PrometheusConfig.class.getName(), com.networknt.config.Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
                return instance;
            }
        }
        return new PrometheusConfig(configName);
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }



    private void setConfigData() {
        if (mappedConfig.containsKey("enabled")) {
            enabled = com.networknt.config.Config.loadBooleanValue("enabled", mappedConfig.get("enabled"));
        }
        if (mappedConfig.containsKey("enableHotspot")) {
            enableHotspot = com.networknt.config.Config.loadBooleanValue("enableHotspot", mappedConfig.get("enableHotspot"));
        }
        if (mappedConfig.containsKey("description")) {
            description = (String) mappedConfig.get("description");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnableHotspot() {
        return enableHotspot;
    }

    public void setEnableHotspot(boolean enableHotspot) {
        this.enableHotspot = enableHotspot;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
