package com.networknt.client.simplepool.exceptions;

public class SimplePoolConnectionClosureException extends RuntimeException {
    public SimplePoolConnectionClosureException(String message) { super(message); }
    public SimplePoolConnectionClosureException(String message, Exception e) { super(message, e); }
}
