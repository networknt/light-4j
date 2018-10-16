package com.networknt.handler;

import io.undertow.server.HttpServerExchange;

/**
 * @author Nicholas Azar
 */
public class OrchestrationHandler implements LightHttpHandler {

	static final String MISSING_HANDlER = "ERR10016";

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (Handler.start(exchange)) {
            Handler.next(exchange);
        } else {
            String methodPath = String.format("Method: %s, RequestPath: %s", exchange.getRequestMethod(), exchange.getRequestPath());
            setExchangeStatus(exchange, MISSING_HANDlER, methodPath);
        }
    }
}
