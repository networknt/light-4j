package com.networknt.handler;

import com.networknt.httpstring.AttachmentConstants;
import com.networknt.utility.Constants;
import com.networknt.utility.StringUtils;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class HandlerUtils {
	private static final Logger logger = LoggerFactory.getLogger(HandlerUtils.class);
    public static final String DELIMITOR = "@";
    protected static final String INTERNAL_KEY_FORMAT = "%s %s";

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
        if(mapping == null) {
            if(logger.isDebugEnabled()) logger.debug("mapping is empty in the configuration.");
            return null;
        }
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

    public static String toInternalKey(String key) {
        String[] tokens = StringUtils.trimToEmpty(key).split(DELIMITOR);

        if (tokens.length ==2) {
            return toInternalKey(tokens[1], tokens[0]);
        }

        logger.warn("Invalid key {}", key);
        return key;
    }

    public static String toInternalKey(String method, String path) {
        return String.format(INTERNAL_KEY_FORMAT, method, HandlerUtils.normalisePath(path));
    }

    public static void populateAuditAttachmentField(final HttpServerExchange exchange, String fieldName, String fieldValue) {
        Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);

        if(auditInfo == null) {
            logger.trace("AuditInfo is null, creating a new one and inserting the key-value pair '{}:{}'", fieldName, fieldValue);
            auditInfo = new HashMap<>();
            auditInfo.put(fieldName, fieldValue);

        } else {
            logger.trace("AuditInfo is not null, inserting the key-value pair '{}:{}'", fieldName, fieldValue);

            if (auditInfo.containsKey(fieldName))
                logger.debug("AuditInfo already contains the field '{}'! Replacing the value '{}' with '{}'.", fieldName, auditInfo.get(fieldName), fieldValue);

            auditInfo.put(fieldName, fieldValue);
        }
        exchange.putAttachment(AttachmentConstants.AUDIT_INFO, auditInfo);
    }

}
