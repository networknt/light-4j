package com.networknt.handler;

import com.networknt.httpstring.AttachmentConstants;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;

/**
 * This handler is special middleware handler, and it is used to inject response interceptors in the request/response
 * chain to modify/transform the response before calling the next middleware handler.
 *
 */
public interface ResponseInterceptor extends Interceptor {

    String RESPONSE_HEADERS = "responseHeaders";
    String REQUEST_HEADERS = "requestHeaders";
    String RESPONSE_BODY = "responseBody";
    String REMOVE = "remove";
    String UPDATE = "update";
    String QUERY_PARAMETERS = "queryParameters";
    String PATH_PARAMETERS = "pathParameters";
    String METHOD = "method";
    String REQUEST_URL = "requestURL";
    String REQUEST_URI = "requestURI";
    String REQUEST_PATH = "requestPath";
    String POST = "post";
    String PUT = "put";
    String PATCH = "patch";
    String REQUEST_BODY = "requestBody";
    String AUDIT_INFO = "auditInfo";
    String STATUS_CODE = "statusCode";

    String STARTUP_HOOK_NOT_LOADED = "ERR11019";
    String RESPONSE_TRANSFORM = "response-transform";

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
        return exchange.getAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY);
    }
}
