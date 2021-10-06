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

    public static String normalisePath(String requestPath) {
        if(!requestPath.startsWith("/")) {
            return "/" + requestPath;
        }
        return requestPath;
    }
}
