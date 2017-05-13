package com.networknt.security.inbound;

/**
 * Verification Exception is a runtime exception that is thrown during
 * message verification.
 *
 * @author Steve Hu
 */
public class VerificationException extends RuntimeException {
    public VerificationException(String message, Throwable cause) {
        super(message, cause);
    }
    public VerificationException(String message) {
        super(message);
    }
}
