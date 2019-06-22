/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.exception;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.exception.ApiException;
import com.networknt.exception.FrameworkException;
import com.networknt.exception.ClientException;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * This is a middleware handler that should be put in the beginning of request/response
 * chain. It wraps the entire chain so that any un-handled exceptions will finally reach
 * here and to be handled gracefully. It is encouraged to handle exceptions in business
 * handler because the context is clear and the exchange will be terminated at the right
 * place.
 *
 * This handler is plugged in by default from light-codegen and it should be enabled on
 * production as the last defence line. It also dispatch the request to worker thread
 * pool from IO thread pool. Only turn this off if you understand the impact and have a
 * very good reason to do so.
 *
 * @author Steve Hu
 */
public class ExceptionHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);

    public static final String CONFIG_NAME = "exception";
    static final ExceptionConfig config =
            (ExceptionConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ExceptionConfig.class);

    static final String STATUS_RUNTIME_EXCEPTION = "ERR10010";
    static final String STATUS_UNCAUGHT_EXCEPTION = "ERR10011";

    private volatile HttpHandler next;

    public ExceptionHandler() {

    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        // dispatch here to make sure that all exceptions will be capture in this handler
        // otherwise, some of the exceptions will be captured in Connectors class in Undertow
        // As we've updated Server.java to redirect the logs to slf4j but still it make sense
        // to handle the exception on our ExcpetionHandler.
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        try {
            Handler.next(exchange, next);
        } catch (Throwable e) {
            logger.error("Exception:", e);
            if(exchange.isResponseChannelAvailable()) {
                //handle exceptions
                if(e instanceof RuntimeException) {
                    // check if it is FrameworkException which is subclass of RuntimeException.
                    if(e instanceof FrameworkException) {
                        FrameworkException fe = (FrameworkException)e;
                        exchange.setStatusCode(fe.getStatus().getStatusCode());
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                        exchange.getResponseSender().send(fe.getStatus().toString());
                        logger.error(fe.getStatus().toString(), e);
                    } else {
                        setExchangeStatus(exchange, STATUS_RUNTIME_EXCEPTION);
                    }
                } else {
                    if(e instanceof ApiException) {
                        ApiException ae = (ApiException)e;
                        exchange.setStatusCode(ae.getStatus().getStatusCode());
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                        exchange.getResponseSender().send(ae.getStatus().toString());
                        logger.error(ae.getStatus().toString(), e);
                    } else if(e instanceof ClientException){
                        ClientException ce = (ClientException)e;
                        if(ce.getStatus().getStatusCode() == 0){
                            setExchangeStatus(exchange, STATUS_UNCAUGHT_EXCEPTION);
                        } else {
                            exchange.setStatusCode(ce.getStatus().getStatusCode());
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                            exchange.getResponseSender().send(ce.getStatus().toString());
                        }

                    } else {
                        setExchangeStatus(exchange, STATUS_UNCAUGHT_EXCEPTION);
                    }
                }
            }
        } finally {
            // at last, clean the MDC. Most likely, correlationId in side.
            //logger.debug("Clear MDC");
            MDC.clear();
        }
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(final HttpHandler next) {
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
        ModuleRegistry.registerModule(ExceptionHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }

}
