package com.networknt.handler;

import com.networknt.handler.MiddlewareHandler;

/**
 * This is the interface for the request interceptors. It is just a normal middleware handler with some extra
 * indicators.
 */
public interface RequestInterceptorHandler extends MiddlewareHandler {
    boolean isRequiredContent();
}
