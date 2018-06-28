package com.networknt.server;

import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;

public class Test1MiddlewareHandler implements MiddlewareHandler {

    private volatile HttpHandler next;
    public static final AttachmentKey<String> CHAIN_ID = AttachmentKey.create(String.class);
    public static final AttachmentKey<Integer> CHAIN_SEQ = AttachmentKey.create(Integer.class);

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(HttpHandler next) {
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void register() {

    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        System.out.println("Test1MiddlewareHandler is called");

//        next.handleRequest(httpServerExchange);
        Handler.next(httpServerExchange);
    }
}
