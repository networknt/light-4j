package com.networknt.httpstring;

import java.util.Map;

import io.undertow.util.AttachmentKey;

/**
 * This class allows the definition of attachment keys shared by multiple modules and thus eliminates dependencies between them  
 * 
 * @author ddobrin
 *
 */
public class AttachmentConstants {
    // The key to the audit info attachment in exchange. Allow other handlers to set values.
    public static final AttachmentKey<Map> AUDIT_INFO = AttachmentKey.create(Map.class);
}
