package com.networknt.handler;

import io.undertow.server.HttpServerExchange;

/**
 * @author Nicholas Azar
 */
public class OrchestrationHandler implements LightHttpHandler {

    static final String MISSING_HANDlER = "ERR10048";

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (Handler.start(exchange)) {
            Handler.next(exchange);
        } else {
            // There is no matching path/method combination. Check if there are defaultHandlers defined.
            if(Handler.startDefaultHandlers(exchange)) {
                Handler.next(exchange);
            } else {
                setExchangeStatus(exchange, MISSING_HANDlER, exchange.getRequestPath());
            }
        }
    }
}
