package com.networknt.security.inbound;

/**
 * Created by steve on 10/04/17.
 */
public interface VerifiedMessage {

    String getContentType();
    byte[] getBody();

}
