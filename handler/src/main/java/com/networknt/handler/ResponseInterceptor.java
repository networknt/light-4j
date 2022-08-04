package com.networknt.handler;

import com.networknt.httpstring.AttachmentConstants;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;

/**
 * This handler is special middleware handler, and it is used to inject response interceptors in the request/response
 * chain to modify/transform the response before calling the next middleware handler.
 *
 */
public interface ResponseInterceptor extends MiddlewareHandler {
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

    /**
     * A default interface method to get the buffer from the exchange attachment for response body.
     * @param exchange HttpServerExchange
     * @return PooledByteBuffer[] array
     */
    default PooledByteBuffer[] getBuffer(HttpServerExchange exchange) {
        PooledByteBuffer[] buffer = exchange.getAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY);
        if (buffer == null) {
            throw new IllegalStateException("Response content is not available in exchange attachment as there is no interceptors.");
        }
        return buffer;
    }
}
