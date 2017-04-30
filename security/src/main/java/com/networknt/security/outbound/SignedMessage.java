package com.networknt.security.outbound;

/**
 * Created by steve on 10/04/17.
 */
public interface SignedMessage {
    String getEnvelope();
    String getBody();
}
