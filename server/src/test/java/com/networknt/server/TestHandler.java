package com.networknt.server;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * Created by steve on 03/02/17.
 */
public class TestHandler implements HttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseSender().send("Hello World!");
    }
}