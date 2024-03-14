package com.networknt.client.simplepool.exceptions;

public class SimplePoolConnectionFailureException extends RuntimeException {
    public SimplePoolConnectionFailureException(String message) { super(message); }
    public SimplePoolConnectionFailureException(String message, Exception e) { super(message, e); }
}
