/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

package com.networknt.dump;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handler that dumps request and response to a log based on the dump.json config
 * <p>
 * Created by steve on 01/09/16.
 * To handle options in request, should name method dumpRequest[OPTION_NAME]
 */
public class DumpHandler implements MiddlewareHandler {
    public static final String CONFIG_NAME = "dump";
    public static final String ENABLED = "enabled";

    private static final String INDENT_SIZE = "indentSize";
    private static final int DEFAULT_INDENT_SIZE = 4;

    private static final Logger logger = LoggerFactory.getLogger(DumpHandler.class);

    private static Map<String, Object> config =
            Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME);

    private volatile HttpHandler next;

    public DumpHandler() { }

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
        Object object = config.get(ENABLED);
        return object != null && (Boolean) object;
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(DumpHandler.class.getName(), config, null);
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
        if(isEnabled()) {
            Map<String, Object> result = new LinkedHashMap<>();
            //dump request info into result right away
            RequestResponseDumper requestDumper = new RequestResponseDumper(config, exchange, IDumpable.HttpMessageType.REQUEST);
            requestDumper.dump();
            requestDumper.putResultTo(result);
            //only add response wrapper when response config is not set to "false"
            if(DumpHelper.checkOptionNotFalse(config.get(DumpConstants.RESPONSE))) {
                //set Conduit to the conduit chain to store response body
                exchange.addResponseWrapper((factory, exchange12) -> new StoreResponseStreamSinkConduit(factory.create(), exchange12));
            }
            //when complete exchange, dump response info to result, and log the result.
            exchange.addExchangeCompleteListener((exchange1, nextListener) ->{
                RequestResponseDumper responseDumper = new RequestResponseDumper(config, exchange, IDumpable.HttpMessageType.RESPONSE);
                responseDumper.dump();
                responseDumper.putResultTo(result);
                DumpHelper.logResult(result, getIndentSize(), checkIfUseJson());
                nextListener.proceed();
            });
        }
        Handler.next(exchange, next);
    }

    private int getIndentSize() {
        Object indentSize = config.get(INDENT_SIZE);
        if(indentSize instanceof Integer) {
            return (int)config.get(INDENT_SIZE);
        } else {
            return DEFAULT_INDENT_SIZE;
        }
    }

    private boolean checkIfUseJson() {
        Object useJson = config.get(DumpConstants.USE_JSON);
        return useJson instanceof Boolean && (Boolean) useJson;
    }
}
