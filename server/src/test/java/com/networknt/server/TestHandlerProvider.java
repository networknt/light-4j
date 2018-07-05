package com.networknt.server;

import com.networknt.handler.HandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.util.Methods;

/**
 * Created by steve on 03/02/17.
 */
public class TestHandlerProvider implements HandlerProvider {
    @Override
    public HttpHandler getHandler() {
        return Handlers.routing()
                .add(Methods.GET, "/test", new TestHandler());
    }
}
