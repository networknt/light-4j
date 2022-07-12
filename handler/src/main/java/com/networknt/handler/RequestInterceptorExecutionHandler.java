package com.networknt.handler;

import com.networknt.service.SingletonServiceFactory;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Handles the execution of request interceptors.
 *
 * @author Kalev Gonvick
 *
 */
public class RequestInterceptorExecutionHandler implements MiddlewareHandler {

    static final Logger logger = LoggerFactory.getLogger(RequestContentInjectorHandler.class);
    private volatile HttpHandler next;
    private RequestInterceptorExecutionConfig config;
    private RequestInterceptorHandler[] interceptors = null;

    public RequestInterceptorExecutionHandler() {
        config = RequestInterceptorExecutionConfig.load();
        logger.info("SourceConduitInjectorHandler is loaded!");
        interceptors = SingletonServiceFactory.getBeans(RequestInterceptorHandler.class);
    }

    public RequestInterceptorExecutionHandler(RequestInterceptorExecutionConfig cfg) {
        config = cfg;
        logger.info("SourceConduitInjectorHandler is loaded!");
        interceptors = SingletonServiceFactory.getBeans(RequestInterceptorHandler.class);
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
        return this.config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(SinkConduitConfig.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        if(interceptors.length > 0) {
            Arrays.stream(interceptors).forEach((ri -> {
                try {
                    ri.handleRequest(httpServerExchange);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }));
        }
        Handler.next(httpServerExchange, next);
    }
}
