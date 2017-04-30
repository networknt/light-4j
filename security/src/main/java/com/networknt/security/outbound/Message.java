package com.networknt.security.outbound;

/**
 * Created by steve on 10/04/17.
 */
public interface Message {
    String getIssuer();
    String getAudience();
    String getOperation();
    String getContentType();
    byte[] getBody();
}
