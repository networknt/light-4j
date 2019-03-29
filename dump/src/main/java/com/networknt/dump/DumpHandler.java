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

package com.networknt.dump;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handler that dumps request and response to a log based on the dump.json config
 * This handler should be after Body Handler, otherwise body handler won't get info from inputstream.
 * <p>
 * Created by steve on 01/09/16.
 * To handle options in request, should name method dumpRequest[OPTION_NAME]
 */
public class DumpHandler implements MiddlewareHandler {
    private static final String CONFIG_NAME = "dump";

    private static DumpConfig config = (DumpConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, DumpConfig.class);

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
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(DumpHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
        if(isEnabled()) {
            Map<String, Object> result = new LinkedHashMap<>();
            //create rootDumper which will do dumping.
            RootDumper rootDumper = new RootDumper(config, exchange);
            //dump request info into result right away
            rootDumper.dumpRequest(result);
            //only add response wrapper when response config is not set to "false"
            if(config.isResponseEnabled()) {
                //set Conduit to the conduit chain to store response body
                exchange.addResponseWrapper((factory, exchange12) -> new StoreResponseStreamSinkConduit(factory.create(), exchange12));
            }
            //when complete exchange, dump response info to result, and log the result.
            exchange.addExchangeCompleteListener((exchange1, nextListener) ->{
                rootDumper.dumpResponse(result);
                //log the result
                DumpHelper.logResult(result, config);
                nextListener.proceed();
            });
        }
        Handler.next(exchange, next);
    }
}
