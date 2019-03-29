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

package com.networknt.header;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * This is a handler that manipulate request and response headers based on the configuration.
 *
 * Although one header key can support multiple values in HTTP, but it is not supported here.
 * If the key exists during update, the original value will be replaced by the new value.
 *
 * @author Steve Hu
 * @since 1.4.7
 */
public class HeaderHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(HeaderHandler.class);

    public static final String CONFIG_NAME = "header";
    public static final String ENABLED = "enabled";
    public static final String REQUEST = "request";
    public static final String RESPONSE = "response";
    public static final String REMOVE = "remove";
    public static final String UPDATE = "update";

    public static final Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);

    private volatile HttpHandler next;

    public HeaderHandler() {

    }

    /**
     * Check iterate the configuration on both request and response section and update
     * headers accordingly.
     *
     * @param exchange HttpServerExchange
     * @throws Exception Exception
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        // handle request header
        Map<String, Object> requestHeaderMap = (Map<String, Object>)config.get(REQUEST);
        if(requestHeaderMap != null) {
            List<String> requestHeaderRemove = (List<String>)requestHeaderMap.get(REMOVE);
            if(requestHeaderRemove != null) {
                requestHeaderRemove.forEach(s -> exchange.getRequestHeaders().remove(s));
            }
            Map<String, String> requestHeaderUpdate = (Map<String, String>)requestHeaderMap.get(UPDATE);
            if(requestHeaderUpdate != null) {
                requestHeaderUpdate.forEach((k, v) -> exchange.getRequestHeaders().put(new HttpString(k), v));
            }
        }

        // handle response header
        Map<String, Object> responseHeaderMap = (Map<String, Object>)config.get(RESPONSE);
        if(responseHeaderMap != null) {
            List<String> responseHeaderRemove = (List<String>)responseHeaderMap.get(REMOVE);
            if(responseHeaderRemove != null) {
                responseHeaderRemove.forEach(s -> exchange.getResponseHeaders().remove(s));
            }
            Map<String, String> responseHeaderUpdate = (Map<String, String>)responseHeaderMap.get(UPDATE);
            if(responseHeaderUpdate != null) {
                responseHeaderUpdate.forEach((k, v) -> exchange.getResponseHeaders().put(new HttpString(k), v));
            }
        }
        Handler.next(exchange, next);
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
        Object object = config.get(HeaderHandler.ENABLED);
        return object != null && (Boolean) object;
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(HeaderHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }
}
