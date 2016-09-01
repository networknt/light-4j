package com.networknt.server;

import io.undertow.server.HttpHandler;

public interface HandlerProvider {
    HttpHandler getHandler();
}
