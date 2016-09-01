package com.networknt.server;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathTemplateHandler;

public class ApiInfoHandlerProvider implements HandlerProvider {
    public void register(PathTemplateHandler handler) {
        handler.add("/api/info", new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        exchange.getResponseSender().send("api info");
                    }
                });

    }
}
