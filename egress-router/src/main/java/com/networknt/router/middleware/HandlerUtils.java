package com.networknt.router.middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class HandlerUtils {
	private static final Logger logger = LoggerFactory.getLogger(HandlerUtils.class);
	
    /**
     * Looks up the appropriate serviceId for a given requestPath taken directly from exchange.
     * Returns null if the path does not map to a configured service.
     * @param searchKey search key
     * @param mapping a map of prefix and service id
     * @return serviceId that is found
     */
    public static String findServiceId(String searchKey, Map<String, String> mapping) {
        if(logger.isDebugEnabled()) logger.debug("findServiceId for " + searchKey);
        String serviceId = null;

        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String prefix = entry.getKey();
            if(searchKey.startsWith(prefix)) {
                if((searchKey.length() == prefix.length() || searchKey.charAt(prefix.length()) == '/')) {
                    serviceId = entry.getValue();
                    break;
                }
            }
        }
        if(logger.isDebugEnabled()) logger.debug("serviceId = " + serviceId);
        return serviceId;
    }

    /**
     * Looks up the appropriate serviceId for a given requestPath taken directly from exchange.
     * Returns null if the path does not map to a configured service, otherwise, an array will
     * be returned with the first element the path prefix and the second element the serviceId.
     *
     * @param searchKey search key
     * @param mapping a map of prefix and service id
     * @return pathPrefix and serviceId in an array that is found
     */
    public static String[] findServiceEntry(String searchKey, Map<String, String> mapping) {
        if(logger.isDebugEnabled()) logger.debug("findServiceEntry for " + searchKey);
        String[] result = null;

        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String prefix = entry.getKey();
            if(searchKey.startsWith(prefix)) {
                if((searchKey.length() == prefix.length() || searchKey.charAt(prefix.length()) == '/')) {
                    result = new String[2];
                    result[0] = entry.getKey();
                    result[1] = entry.getValue();
                    break;
                }
            }
        }
        if(result == null) {
            if(logger.isDebugEnabled()) logger.debug("serviceEntry not found!");
        } else {
            if(logger.isDebugEnabled()) logger.debug("prefix = " + result[0] + " serviceId = " + result[1]);
        }
        return result;
    }

    public static String normalisePath(String requestPath) {
        if(!requestPath.startsWith("/")) {
            return "/" + requestPath;
        }
        return requestPath;
    }
}
