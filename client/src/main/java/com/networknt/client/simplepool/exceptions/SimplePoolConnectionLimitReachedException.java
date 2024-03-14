package com.networknt.client.simplepool.exceptions;

public class SimplePoolConnectionLimitReachedException extends RuntimeException {
    public SimplePoolConnectionLimitReachedException(String message) { super(message); }
    public SimplePoolConnectionLimitReachedException(String message, Exception e) { super(message, e); }
}
