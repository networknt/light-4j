package com.networknt.reqtrans;

import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class ConfigUtils {
	private static final Logger logger = LoggerFactory.getLogger(ConfigUtils.class);
    public static final String DELIMITOR = "@";
    protected static final String INTERNAL_KEY_FORMAT = "%s %s";

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

    public static String normalisePath(String requestPath) {
        if(!requestPath.startsWith("/")) {
            return "/" + requestPath;
        }
        return requestPath;
    }

    public static String toInternalKey(String key) {
        String[] tokens = StringUtils.trimToEmpty(key).split(DELIMITOR);

        if (tokens.length ==2) {
            return toInternalKey(tokens[1], tokens[0]);
        }

        logger.warn("Invalid key {}", key);
        return key;
    }

    public static String toInternalKey(String method, String path) {
        return String.format(INTERNAL_KEY_FORMAT, method, ConfigUtils.normalisePath(path));
    }

}