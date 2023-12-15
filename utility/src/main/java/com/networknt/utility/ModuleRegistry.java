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

package com.networknt.utility;

import com.networknt.server.ServerConfig;

import java.util.*;

/**
 * Module registry for server info component. Every enabled module should register
 * itself so that server info can output the status of the module as well as its
 * configuration at runtime.
 *
 * @author Steve Hu
 */
public class ModuleRegistry {

    private static final Map<String, Object> moduleRegistry = new HashMap<>();
    private static final Map<String, Object> pluginRegistry = new HashMap<>();
    private static final List<Map<String, Object>> plugins = new ArrayList<>();

    public static void registerModule(String configName, String moduleClass, Map<String, Object> config, List<String> masks) {
        // use module name as key for the config map will make api-certification parses this object easily.
        if(config != null) {
            if(ServerConfig.getInstance().isMaskConfigProperties() && masks != null && !masks.isEmpty()) {
                for (String mask : masks) {
                    maskNode(config, mask);
                }
            }
            moduleRegistry.put(configName + ":" + moduleClass, config);
        } else {
            // we don't have any module without config, but we cannot guarantee user created modules
            moduleRegistry.put(configName + ":" + moduleClass, new HashMap<String, Object>());
        }
    }
    public static Map<String, Object> getModuleRegistry() {
        return moduleRegistry;
    }

    /**
     * Register a plugin with config and masks
     * @param configName The config name which is the file name of the configuration file.
     * @param pluginClass The Java class name of the plugin.
     * @param pluginName The name of the plugin in the pom.xml file. It can be different from the configName.
     * @param pluginVersion The version of the plugin in the pom.xml file.
     * @param config The map of the configuration of the plugin.
     * @param masks The list of the properties that need to be masked.
     */
    public static void registerPlugin(String pluginName, String pluginVersion, String configName, String pluginClass, Map<String, Object> config, List<String> masks) {
        // use plugin name as key for the config map will make api-certification parses this object easily.
        if(config != null) {
            if(ServerConfig.getInstance().isMaskConfigProperties() && masks != null && !masks.isEmpty()) {
                for (String mask : masks) {
                    maskNode(config, mask);
                }
            }
            pluginRegistry.put(configName + ":" + pluginClass, config);
        }
        Map<String, Object> plugin = new HashMap<>();
        plugin.put("pluginName", pluginName);
        plugin.put("pluginClass", pluginClass);
        plugin.put("pluginVersion", pluginVersion);
        plugins.add(plugin);
    }

    public static Map<String, Object> getPluginRegistry() { return pluginRegistry; }
    public static List<Map<String, Object>> getPlugins() { return plugins; }

    @SuppressWarnings("unchecked")
    private static void maskNode(Map<String, Object> map, String mask) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String && key.equals(mask))
                map.put(key, "*");
            else if (value instanceof Map)
                maskNode((Map) value, mask);
            else if (value instanceof List) {
                maskList((List) value, mask);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void maskList(List list, String mask) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof String && list.get(i).equals(mask)) {
                list.set(i, "*");
            } else if(list.get(i) instanceof Map) {
                maskNode((Map<String, Object>) list.get(i), mask);
            } else if(list.get(i) instanceof List) {
                maskList((List) list.get(i), mask);
            }
        }
    }
}
