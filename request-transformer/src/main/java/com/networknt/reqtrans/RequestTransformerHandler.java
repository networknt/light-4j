package com.networknt.reqtrans;

import com.networknt.handler.InterceptorHandler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestTransformerHandler implements InterceptorHandler {
    static final Logger logger = LoggerFactory.getLogger(RequestTransformerHandler.class);

    private RequestTransformerConfig config;
    private volatile HttpHandler next;

    public RequestTransformerHandler() {
        if(logger.isInfoEnabled()) logger.info("RequestManipulatorHandler is loaded");
        config = RequestTransformerConfig.load();
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(RequestTransformerHandler.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void reload() {
        config.reload();
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

    }

    @Override
    public boolean isRequiredContent() {
        return true;
    }
}
