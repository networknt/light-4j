package com.networknt.security.inbound;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * This is a middleware handler that is responsible for filter out some fields from the request body
 * for security reasons. Normally it is based on client_id, or other JWT claims to do the filter. The
 * real implementation must be extend this and need some domain knowledge to complete the filter logic.
 *
 * @author Steve Hu
 *
 */
public class RequestFilterHandler implements HttpHandler {

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

    }
}
