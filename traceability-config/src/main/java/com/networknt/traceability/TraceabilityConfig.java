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

package com.networknt.traceability;

import com.networknt.config.Config;

import java.util.Map;

/**
 * Configuration class for traceability
 *
 * @author Steve Hu
 *
 * @deprecated (Merged traceability handler into correlation handler)
 */
@Deprecated
public class TraceabilityConfig {
    public static final String CONFIG_NAME = "traceability";
    private static final String ENABLED = "enabled";
    private Map<String, Object> mappedConfig;
    private final Config config;
    boolean enabled;

    private TraceabilityConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        setConfigData();
    }
    private TraceabilityConfig() {
        this(CONFIG_NAME);
    }

    public static TraceabilityConfig load(String configName) {
        return new TraceabilityConfig(configName);
    }

    public static TraceabilityConfig load() {
        return new TraceabilityConfig();
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
