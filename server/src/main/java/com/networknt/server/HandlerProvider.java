package com.networknt.server;

import io.undertow.server.handlers.PathTemplateHandler;

public interface HandlerProvider {
    public void register(PathTemplateHandler pathTemplateHandler);
}
