package com.networknt.client.simplepool.exceptions;

/**
 * SimplePoolConnectionFailureException class.
 */
public class SimplePoolConnectionFailureException extends RuntimeException {
    /**
     * Constructor.
     * @param message the message
     */
    public SimplePoolConnectionFailureException(String message) {
        super(message);
    }

    /**
     * Constructor.
     * @param message the message
     * @param cause the cause
     */
    public SimplePoolConnectionFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
