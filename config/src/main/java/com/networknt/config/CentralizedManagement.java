package com.networknt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CentralizedManagement {
    private static final String CENTRALIZED_MANAGEMENT = "values";
    private static final Map<String, Object> valueConfig = Config.getInstance().getJsonMapConfig(CENTRALIZED_MANAGEMENT);

    static final Logger logger = LoggerFactory.getLogger(CentralizedManagement.class);

    public static Map<String, Object> merge(String configName, Map<String, Object> config) {
        if (valueConfig == null) {
            logger.error("centralized management file \"values.yaml\" cannot be found");
            return config;
        }
        merge(config, valueConfig.get(configName));
        return config;
    }

    public static Object merge(String configName, Map<String, Object> config, Class clazz) {
        if (valueConfig == null) {
            logger.error("centralized management file \"values.yaml\" cannot be found");
            return convertMapToObj(config, clazz);
        }
        merge(config, valueConfig.get(configName));
        return convertMapToObj(config, clazz);
    }

    private static void merge(Object m1, Object m2) {
        if (m1 instanceof Map && m2 instanceof Map) {
            Iterator<String> fieldNames = ((Map<String, Object>) m1).keySet().iterator();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                Object field1 = ((Map<String, Object>) m1).get(fieldName);
                Object field2 = ((Map<String, Object>) m2).get(fieldName);
                if (field1 != null && field2 == null
                        && (field1 instanceof Map || field1 instanceof List)) {
                    merge(field1, m2);
                } else if (field2 != null) {
                    ((Map<String, Object>) m1).put(fieldName, field2);
                }
            }
        } else if (m1 instanceof List) {
            for (Object field1 : ((List<Object>) m1)) {
                merge(field1, m2);
            }
        }
    }

    private static Object convertMapToObj(Map<String, Object> map, Class clazz) {
        ObjectMapper mapper = new ObjectMapper();
        Object obj = mapper.convertValue(map, clazz);
        return obj;
    }
}
