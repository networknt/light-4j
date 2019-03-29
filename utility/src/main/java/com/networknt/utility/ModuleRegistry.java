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

import java.util.*;

/**
 * Module registry for server info component. Every enabled module should register
 * itself so that server info can output the status of the module as well as its
 * configuration at runtime.
 *
 * @author Steve Hu
 */
public class ModuleRegistry {

    private static Map<String, Object> registry = new HashMap<>();

    public static void registerModule(String moduleName, Map<String, Object> config, List<String> masks) {
        // use module name as key for the config map will make api-certification parses this object easily.
        if(config != null) {
            if(masks != null && masks.size() > 0) {
                for (String mask : masks) {
                    maskNode(config, mask);
                }
            }
            registry.put(moduleName, config);
        } else {
            // we don't have any module without config but we cannot guarantee user created modules
            registry.put(moduleName, new HashMap<String, Object>());
        }
    }

    public static Map<String, Object> getRegistry() {
        return registry;
    }

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
