package com.networknt.handler;

import io.undertow.server.HttpServerExchange;

/**
 * @author Nicholas Azar
 */
public class OrchestrationHandler implements LightHttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (Handler.start(exchange)) {
            Handler.next(exchange);
        } else {
            String methodPath = String.format("%s %s", exchange.getRequestMethod(), exchange.getRequestPath());
            setExchangeStatus(exchange, "ERR10048", methodPath);
        }
    }
}
