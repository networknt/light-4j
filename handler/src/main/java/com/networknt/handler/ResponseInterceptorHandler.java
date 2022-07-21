package com.networknt.handler;

/**
 * This handler is special middleware handler, and it is used to inject response interceptors in the request/response
 * chain to modify/transform the response before calling the next middleware handler.
 *
 */
public interface ResponseInterceptorHandler extends MiddlewareHandler {
    /**
     * This is an indicator to load modifiable sink conduit to allow the response body to be updated. It is true
     * if the interceptor wants to update the response body.
     * @return boolean indicator
     */
    boolean isRequiredContent();

    /**
     * Indicate if the interceptor handler will be executed in synchronous or asynchronous. By default, it is
     * executed asynchronously.
     * @return boolean indicator
     */
    default boolean isAsync() {return false;};

}
