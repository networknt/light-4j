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

package com.networknt.resource;

import com.networknt.server.ModuleRegistry;

import java.util.List;
import java.util.Map;

public class VirtualHostConfig {
    public static final String CONFIG_NAME = "virtual-host";

    List<VirtualHost> hosts;

    private static volatile VirtualHostConfig instance;
    private final com.networknt.config.Config config;
    private java.util.Map<String, Object> mappedConfig;

    private VirtualHostConfig(String configName) {
        config = com.networknt.config.Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        if (mappedConfig != null) {
            setConfigData();
        }
    }

    private VirtualHostConfig() {
        this(CONFIG_NAME);
    }

    public static VirtualHostConfig load() {
        return load(CONFIG_NAME);
    }

    public static VirtualHostConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            if (instance != null && instance.getMappedConfig() == com.networknt.config.Config.getInstance().getJsonMapConfig(configName)) {
                return instance;
            }
            synchronized (VirtualHostConfig.class) {
                if (instance != null && instance.getMappedConfig() == com.networknt.config.Config.getInstance().getJsonMapConfig(configName)) {
                    return instance;
                }
                instance = new VirtualHostConfig(configName);
                ModuleRegistry.registerModule(CONFIG_NAME, VirtualHostConfig.class.getName(), com.networknt.config.Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
                return instance;
            }
        }
        return new VirtualHostConfig(configName);
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
        // manual mapping to hosts list, need to implementation based on actual structure of virtual-host.yml
        // assuming it is a list of maps, conversion logic might be complex depending on yaml structure.
        // For simplicity and assuming Jackson binding is preferred for complex types, we can use the default binding if Config.getJsonObjectConfig was used.
        // However, we are moving away from getJsonObjectConfig static call.
        // If we want to keep manual parsing:
        if (mappedConfig.containsKey("hosts")) {
             try {
                 hosts = com.networknt.config.Config.getInstance().getMapper().convertValue(mappedConfig.get("hosts"), new com.fasterxml.jackson.core.type.TypeReference<List<VirtualHost>>(){});
             } catch (Exception e) {
                 e.printStackTrace();
             }
        }
    }

    public List<VirtualHost> getHosts() {
        return hosts;
    }

    public void setHosts(List<VirtualHost> hosts) {
        this.hosts = hosts;
    }
}
