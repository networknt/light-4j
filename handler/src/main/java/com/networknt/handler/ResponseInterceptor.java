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

    /** Response headers constant */
    String RESPONSE_HEADERS = "responseHeaders";
    /** Request headers constant */
    String REQUEST_HEADERS = "requestHeaders";
    /** Response body constant */
    String RESPONSE_BODY = "responseBody";
    /** Remove operation constant */
    String REMOVE = "remove";
    /** Update operation constant */
    String UPDATE = "update";
    /** Query parameters constant */
    String QUERY_PARAMETERS = "queryParameters";
    /** Path parameters constant */
    String PATH_PARAMETERS = "pathParameters";
    /** HTTP method constant */
    String METHOD = "method";
    /** Request URL constant */
    String REQUEST_URL = "requestURL";
    /** Request URI constant */
    String REQUEST_URI = "requestURI";
    /** Request path constant */
    String REQUEST_PATH = "requestPath";
    /** POST method constant */
    String POST = "post";
    /** PUT method constant */
    String PUT = "put";
    /** PATCH method constant */
    String PATCH = "patch";
    /** Request body constant */
    String REQUEST_BODY = "requestBody";
    /** Audit info constant */
    String AUDIT_INFO = "auditInfo";
    /** Status code constant */
    String STATUS_CODE = "statusCode";

    /** Error code for startup hook not loaded */
    String STARTUP_HOOK_NOT_LOADED = "ERR11019";
    /** Response transform constant */
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
