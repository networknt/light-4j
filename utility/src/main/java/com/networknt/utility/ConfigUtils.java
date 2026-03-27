package com.networknt.utility;

import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Configuration utility class.
 */
public class ConfigUtils {
    /**
     * Default constructor for ConfigUtils.
     */
    public ConfigUtils() {
    }
	private static final Logger logger = LoggerFactory.getLogger(ConfigUtils.class);
    /** Delimiter for configuration keys */
    public static final String DELIMITOR = "@";
    /** Internal key format */
    protected static final String INTERNAL_KEY_FORMAT = "%s %s";

    /**
     * Finds a service entry in a mapping.
     * @param method HTTP method
     * @param searchKey search key
     * @param mapping mapping map
     * @return String service entry
     */
    public static String findServiceEntry(String method, String searchKey, Map<String, Object> mapping) {
        if(logger.isDebugEnabled()) logger.debug("findServiceEntry for " + searchKey + " and method: " + method);
        if(logger.isDebugEnabled()) logger.debug("mapping size: " + mapping.size());
        String result = null;
        for (Map.Entry<String, Object> entry : mapping.entrySet()) {
        	String[] tokens = StringUtils.trimToEmpty(entry.getKey()).split(DELIMITOR);
            String ConfigPrefix = tokens[0];
            String ConfigMethod = tokens[1];
            if(logger.isDebugEnabled()) logger.debug("prefix: " + ConfigPrefix);
            if(logger.isDebugEnabled()) logger.debug("method: " + ConfigMethod);
            if(searchKey.startsWith(ConfigPrefix)) {
                if((searchKey.length() == ConfigPrefix.length() || searchKey.charAt(ConfigPrefix.length()) == '/')
                		&& method.equals(ConfigMethod)) {
                    result = entry.getKey();
                    break;
                }
            }
        }
        if(result == null) {
            if(logger.isDebugEnabled()) logger.debug("serviceEntry not found!");
        } else {
            if(logger.isDebugEnabled()) logger.debug("prefix = " + result);
        }
        return result;
    }

    /**
     * Normalizes a request path by adding a leading slash if missing.
     * @param requestPath request path
     * @return String normalized path
     */
    public static String normalisePath(String requestPath) {
        if(!requestPath.startsWith("/")) {
            return "/" + requestPath;
        }
        return requestPath;
    }

    /**
     * Converts a key with a delimiter to an internal key.
     * @param key key string
     * @return String internal key
     */
    public static String toInternalKey(String key) {
        String[] tokens = StringUtils.trimToEmpty(key).split(DELIMITOR);

        if (tokens.length ==2) {
            return toInternalKey(tokens[1], tokens[0]);
        }

        logger.warn("Invalid key {}", key);
        return key;
    }

    /**
     * Converts a method and path to an internal key.
     * @param method HTTP method
     * @param path request path
     * @return String internal key
     */
    public static String toInternalKey(String method, String path) {
        return String.format(INTERNAL_KEY_FORMAT, method, ConfigUtils.normalisePath(path));
    }

    /**
     * Normalizes a map by sorting its keys and nested collections.
     * @param map map to normalize
     * @param keysToNotSort keys that should not be sorted
     * @return Map normalized map
     */
    public static Map<String, Object> normalizeMap(Map<String, Object> map, List<String> keysToNotSort) {
        Map<String, Object> normalizedData = new TreeMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            // Normalize arrays by sorting them
            if (value instanceof List) {
                List<?> listValue = (List<?>) value;
                // for a list of prefixes, skip the sorting.
                if (matchesAny(key, keysToNotSort)) {
                    normalizedData.put(key, listValue);
                } else {
                    normalizedData.put(key, normalizeList(listValue, keysToNotSort));
                }
            } else if (value instanceof Map) {
                // Recursively normalize nested maps
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                normalizedData.put(key, normalizeMap(nestedMap, keysToNotSort));
            } else {
                normalizedData.put(key, value);
            }
        }

        return normalizedData;
    }

    /**
     * Checks if a string matches any string in a list.
     * @param input input string
     * @param stringsToMatch list of strings to match
     * @return boolean true if matches
     */
    public static boolean matchesAny(String input, List<String> stringsToMatch) {
        return stringsToMatch.contains(input);
    }

    /**
     * Normalizes a list by sorting its elements if they are strings or by normalizing inner maps.
     * @param list list to normalize
     * @param keysToNotSort keys that should not be sorted
     * @return List normalized list
     */
    public static List<?> normalizeList(List<?> list, List<String> keysToNotSort) {
        if(list.isEmpty()) {
            return list;
        }
        if(list.get(0) instanceof String) {
            // Case 1: List of Strings
            List<String> stringList = (List<String>) list;
            Collections.sort(stringList);
            return stringList;
        } else if(list.get(0) instanceof Map) {
            // Case 2: List of Maps
            // Do not sort the map in the list. The order is important.
            List<Map<String, Object>> mapList = (List<Map<String, Object>>) list;
            for(int i = 0; i < mapList.size(); i++) {
                Map<String, Object> map = mapList.get(i);
                mapList.set(i, normalizeMap(map, keysToNotSort));
            }
            return mapList;
        } else {
            throw new IllegalArgumentException("Unsupported element type in the list");
        }
    }
}
