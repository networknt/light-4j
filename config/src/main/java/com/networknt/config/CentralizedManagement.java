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

package com.networknt.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class has two public methods called mergeObject and mergeMap which is
 * used to merge config file with the values generate by ConfigInjection.class.
 * <p>
 * The first method "mergeMap" is used to merge map config with values and
 * return another merged map while the second one "mergeObject" is used to merge
 * map config with values and return a mapping object. the merge logic is based on
 * depth first search.
 * <p>
 * Created by jiachen on 2019-01-08.
 */
public class CentralizedManagement {
    // Merge map config with values generated by ConfigInjection.class and return map
    public static void mergeMap(boolean decrypt, Map<String, Object> config) {
        merge(decrypt, config);
    }
    // Merge map config with values generated by ConfigInjection.class and return mapping object
    public static Object mergeObject(boolean decrypt, Object config, Class clazz) {
        merge(decrypt, config);
        return convertMapToObj((Map<String, Object>) config, clazz);
    }
    // Search the config map recursively, expand List and Map level by level util no further expand
    private static void merge(boolean decrypt, Object m1) {
        if (m1 instanceof Map) {
            Iterator<Object> fieldNames = ((Map<Object, Object>) m1).keySet().iterator();
            String fieldName = null;
            Map<String, Object> mapWithInjectedKey = new HashMap<>();
            while (fieldNames.hasNext()) {
                fieldName = String.valueOf(fieldNames.next());
                Object field1 = ((Map<String, Object>) m1).get(fieldName);
                if (field1 != null) {
                    if (field1 instanceof Map || field1 instanceof List) {
                        merge(decrypt, field1);
                    // Overwrite previous value when the field1 can not be expanded further
                    } else if (field1 instanceof String) {
                        // Retrieve values from ConfigInjection.class
                        Object injectValue = ConfigInjection.getInjectValue((String) field1, decrypt);
                        ((Map<String, Object>) m1).put(fieldName, injectValue);
                    }
                }
                // post order, in case the key of configuration can also be injected.
                Object injectedFieldName = ConfigInjection.getInjectValue(fieldName, decrypt);
                // only inject when key has been changed
                if (!fieldName.equals(injectedFieldName)) {
                    validateInjectedFieldName(fieldName, injectedFieldName);
                    // the map is unmodifiable during iterator, so put in another map and put it back after iteration.
                    mapWithInjectedKey.put((String)ConfigInjection.getInjectValue(fieldName, decrypt), field1);
                    fieldNames.remove();
                }
            }
            // put back those updated keys
            ((Map<String, Object>) m1).putAll(mapWithInjectedKey);
        } else if (m1 instanceof List) {
            for (int i = 0; i < ((List<Object>) m1).size(); i++) {
                Object field1 = ((List<Object>) m1).get(i);
                if (field1 instanceof Map || field1 instanceof List) {
                    merge(decrypt, field1);
                // Overwrite previous value when the field1 can not be expanded further
                } else if (field1 instanceof String) {
                    // Retrieve values from ConfigInjection.class
                    Object injectValue = ConfigInjection.getInjectValue((String) field1, decrypt);
                    ((List<Object>) m1).set(i, injectValue);
                }
            }
        }
    }

    private static void validateInjectedFieldName(String fieldName, Object injectedFieldName) {
        if (injectedFieldName == null) {
            throw new RuntimeException("the overwritten value cannot be null for key:" + fieldName);
        }
        if (!(injectedFieldName instanceof String)) {
            throw new RuntimeException("the overwritten value for key has to be a String" + fieldName);
        }
        if(((String) injectedFieldName).isBlank()) {
            throw new RuntimeException("the overwritten value cannot be empty for key:" + fieldName);
        }
    }

    // Method used to convert map to object based on the reference class provided
    private static Object convertMapToObj(Map<String, Object> map, Class clazz) {
        ObjectMapper mapper = new ObjectMapper();
        Object obj = mapper.convertValue(map, clazz);
        return obj;
    }
}
