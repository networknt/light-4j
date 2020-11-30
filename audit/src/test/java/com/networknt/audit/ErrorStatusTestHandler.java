package com.networknt.audit;

import com.networknt.handler.LightHttpHandler;
import com.networknt.status.Status;
import io.undertow.server.HttpServerExchange;

public class ErrorStatusTestHandler implements LightHttpHandler {
    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) {
        setExchangeStatus(httpServerExchange, new Status("ERR10001"));
    }
}
