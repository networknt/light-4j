package com.networknt.security.inbound;

/**
 * Verified message
 *
 * @author Steve Hu
 */
public interface VerifiedMessage {

    String getContentType();
    byte[] getBody();

}
