package com.networknt.handler;

import com.networknt.httpstring.AttachmentConstants;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for managing audit info attachments in the HttpServerExchange.
 */
public class AuditAttachmentUtil {
    private AuditAttachmentUtil() {
        /* This utility class should not be instantiated */
    }

    private static final Logger logger = LoggerFactory.getLogger(AuditAttachmentUtil.class);

    /**
     * Populates a field in the audit attachment within the HttpServerExchange.
     * @param exchange The current HttpServerExchange.
     * @param fieldName The name of the field to be populated.
     * @param fieldValue The value of the field to be populated.
     */
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
