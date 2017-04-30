package com.networknt.security.inbound;

/**
 * Created by steve on 10/04/17.
 */
public class VerificationException extends RuntimeException {
    public VerificationException(String message, Throwable cause) {
        super(message, cause);
    }
    public VerificationException(String message) {
        super(message);
    }
}
