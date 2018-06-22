package com.networknt.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * @author Nicholas Azar
 */
public class SampleMiddlewareHandler2 implements MiddlewareHandler {
    @Override
    public HttpHandler getNext() {
        return null;
    }

    @Override
    public MiddlewareHandler setNext(HttpHandler next) {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void register() {

    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

    }
}
