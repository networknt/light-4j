package com.networknt.utility;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by steve on 02/09/16.
 */
public class ModuleRegistry {

    private static List<Map<String, Object>> registry = new LinkedList<Map<String, Object>>();

    public static void registerModule(String moduleName, Map<String, Object> config, List<String> masks) {
        Map<String, Object> moduleMap = new LinkedHashMap<String, Object>();
        moduleMap.put("moduleName", moduleName);
        if(config != null) {
            if(masks != null && masks.size() > 0) {
                for(int i = 0; i < masks.size(); i++) {
                    maskNode(config, masks.get(i));
                }
            }
            moduleMap.put("config", config);
        }
        registry.add(moduleMap);
    }

    public static List<Map<String, Object>> getRegistry() {
        return registry;
    }

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
