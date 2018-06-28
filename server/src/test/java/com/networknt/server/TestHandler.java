package com.networknt.server;

import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * Created by steve on 03/02/17.
 */
public class TestHandler implements LightHttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        System.out.println("Hello World!");
        exchange.getResponseSender().send("Hello World!");
    }
}