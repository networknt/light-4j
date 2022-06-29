package com.networknt.handler;

/**
 * This handler is special middleware handler, and it is used to inject interceptors in the request/response chain
 * to modify/transform the request or response before calling the next middleware handler.
 *
 */
public interface InterceptorHandler extends MiddlewareHandler {
    /**
     * This is an indicator to load modifiable sink conduit to allow the response body to be updated. It is true
     * if the interceptor wants to update the response body.
     * @return boolean indicator
     */
    boolean isRequiredContent();
}
