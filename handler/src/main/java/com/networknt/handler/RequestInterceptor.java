package com.networknt.handler;

/**
 * This is the interface for the request interceptors. It is just a normal middleware handler with some extra
 * indicators.
 */
public interface RequestInterceptor extends Interceptor {
    /**
     * Indicates if the interceptor requires the request content.
     * @return true if required, false otherwise.
     */
    boolean isRequiredContent();

}
