package com.networknt.security.outbound;

/**
 * Signed Message
 *
 * @author Steve Hu
 */
public interface SignedMessage {
    String getEnvelope();
    String getBody();
}
