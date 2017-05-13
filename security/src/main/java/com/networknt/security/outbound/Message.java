package com.networknt.security.outbound;

/**
 * Message interface
 *
 * @author Steve Hu
 */
public interface Message {
    String getIssuer();
    String getAudience();
    String getOperation();
    String getContentType();
    byte[] getBody();
}
