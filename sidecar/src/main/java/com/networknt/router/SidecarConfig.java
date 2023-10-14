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

package com.networknt.router;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;

import java.util.Map;

/**
 * Config class for gateway.
 *
 */
public class SidecarConfig {
    public static final String CONFIG_NAME = "sidecar";
    private static final String EGRESS_INGRESS_INDICATOR = "egressIngressIndicator";
    private Map<String, Object> mappedConfig;
    private final Config config;
    String egressIngressIndicator;
    private SidecarConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }
    private SidecarConfig() {
        this(CONFIG_NAME);
    }

    public static SidecarConfig load(String configName) {
        return new SidecarConfig(configName);
    }

    public static SidecarConfig load() {
        return new SidecarConfig();
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
    }

    public void reload(String configName) {
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    public String getEgressIngressIndicator() {
        return egressIngressIndicator;
    }

    public void setEgressIngressIndicator(String egressIngressIndicator) {
        this.egressIngressIndicator = egressIngressIndicator;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    Config getConfig() {
        return config;
    }

    private void setConfigData() {
        if(getMappedConfig() != null) {
            Object object = getMappedConfig().get(EGRESS_INGRESS_INDICATOR);
            if(object != null) egressIngressIndicator = (String)object;
        }
    }

}
