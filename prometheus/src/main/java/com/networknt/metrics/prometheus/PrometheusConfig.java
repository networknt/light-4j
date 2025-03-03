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

/**
 * Prometheus metrics middleware handler configuration that is mapped to all
 * properties in metrics.yml config file.
 *
 * @author Gavin Chen
 */
@ConfigSchema(configName = "prometheus", configKey = "prometheus", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML})
public class PrometheusConfig {
    public static final String CONFIG_NAME = "prometheus";

    @BooleanField(
        configFieldName = "enabled",
        externalizedKeyName = "enabled",
        externalized = true,
        description = "If metrics handler is enabled or not"
    )
    boolean enabled;

    @BooleanField(
            configFieldName = "enableHotspot",
            externalizedKeyName = "enableHotspot",
            externalized = true,
            description = "If the Prometheus hotspot is enabled or not.\n" +
                    "hotspot include thread, memory, classloader,..."
    )
    boolean enableHotspot;

    @JsonIgnore
    String description;

    public PrometheusConfig() {
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
