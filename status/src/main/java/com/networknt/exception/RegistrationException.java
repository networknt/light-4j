package com.networknt.exception;

/**
 * This is an RuntimeException used to wrap the exception during the service
 * registration process when server start in order to distinguish between
 * exceptions caused by port bind and exceptions caused by registration.
 * Since these two need to be handled in different way.
 */
public class RegistrationException extends RuntimeException {
    public RegistrationException(String message) {
        super(message);
    }
}
