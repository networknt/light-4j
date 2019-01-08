package com.networknt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CentralizedManagement {
    static final Logger logger = LoggerFactory.getLogger(CentralizedManagement.class);

    public static Map<String, Object> mergeMap(Map<String, Object> config) {
        merge(config);
        return config;
    }

    public static Object mergeObject(Object config, Class clazz) {
        merge(config);
        return convertMapToObj((Map<String, Object>) config, clazz);
    }

    private static void merge(Object m1) {
        if (m1 instanceof Map) {
            Iterator<String> fieldNames = ((Map<String, Object>) m1).keySet().iterator();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                Object field1 = ((Map<String, Object>) m1).get(fieldName);
                if (field1 != null) {
                    if (field1 instanceof Map || field1 instanceof List) {
                        merge(field1);
                    } else if (field1 instanceof String) {
                        Object injectValue = ConfigInjection.getInjectValue((String) field1);
                        ((Map<String, Object>) m1).put(fieldName, injectValue);
                    }
                }
            }
        } else if (m1 instanceof List) {
            for (int i = 0; i < ((List<Object>) m1).size(); i++) {
                Object field1 = ((List<Object>) m1).get(i);
                if (field1 instanceof Map || field1 instanceof List) {
                    merge(field1);
                } else if (field1 instanceof String) {
                    Object injectValue = ConfigInjection.getInjectValue((String) field1);
                    ((List<Object>) m1).set(i, injectValue);
                }
            }
        }
    }

    private static Object convertMapToObj(Map<String, Object> map, Class clazz) {
        ObjectMapper mapper = new ObjectMapper();
        Object obj = mapper.convertValue(map, clazz);
        return obj;
    }
}
