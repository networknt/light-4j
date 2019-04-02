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

package com.networknt.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * @author Nicholas Azar
 */
public class OrchestrationHandler implements LightHttpHandler {

    static final String MISSING_HANDlER = "ERR10048";

    public OrchestrationHandler() {

    }

    public OrchestrationHandler(HttpHandler lastHandler) {
        Handler.setLastHandler(lastHandler);
    }

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
