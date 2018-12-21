package com.networknt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
// TODO: convert recursive to bfs
// TODO: solve "map inside list" occasion
public class CentralizedManagement {
    private static final String CENTRALIZED_MANAGEMENT = "values";
    private static final Map<String, Object> valueConfig = Config.getInstance().getJsonMapConfig(CENTRALIZED_MANAGEMENT);

    static final Logger logger = LoggerFactory.getLogger(CentralizedManagement.class);

    public static Map<String, Object> merge(String configName, Map<String, Object> config) {
        merge(config, valueConfig.get(configName));
        return config;
    }

    public static Object merge(String configName, Object config, Class clazz) {
        Map<String, Object> map = covertObjectToMap(config);
        merge(map, valueConfig.get(configName));
        return convertMapToObj(map, clazz);
    }

    private static void merge(Object m1, Object m2) {
        Iterator<String> fieldNames = ((Map<String, Object>) m1).keySet().iterator();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            Object field1 = ((Map<String, Object>) m1).get(fieldName);
            Object field2 = ((Map<String, Object>) m2).get(fieldName);
            if (field1 != null && field2 != null
                    && field1 instanceof Map && field2 instanceof Map) {
                merge(field1, field2);
            } else if (field2 != null) {
                ((Map<String, Object>) m1).put(fieldName, field2);
            }
        }
    }

    private static Map<String, Object> covertObjectToMap(Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = mapper.convertValue(obj, Map.class);
        return map;
    }

    private static Object convertMapToObj(Map<String, Object> map, Class clazz) {
        ObjectMapper mapper = new ObjectMapper();
        Object obj = mapper.convertValue(map, clazz);
        return obj;
    }
}
