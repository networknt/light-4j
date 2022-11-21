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
 * A new feature is added to the handler to manipulate the headers per request path basis to
 * support the light-gateway use cases with multiple downstream APIs.
 *
 * @author Steve Hu
 * @since 1.4.7
 */
public class HeaderHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(HeaderHandler.class);

    private static HeaderConfig config;

    private volatile HttpHandler next;

    public HeaderHandler() {
        this.config = HeaderConfig.load();
    }

    /**
     * Please don't use this constructor. It is used by test case only to inject config object.
     * @param cfg HeaderConfig
     * @deprecated
     */
    public HeaderHandler(HeaderConfig cfg) {
        this.config = cfg;
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
        if(logger.isDebugEnabled()) logger.debug("HeaderHandler.handleRequest starts.");
        // handle all request header
        List<String> requestHeaderRemove = config.getRequestRemoveList();
        if(requestHeaderRemove != null) {
            requestHeaderRemove.forEach(s -> exchange.getRequestHeaders().remove(s));
        }
        Map<String, Object> requestHeaderUpdate = config.getRequestUpdateMap();
        if(requestHeaderUpdate != null) {
            requestHeaderUpdate.forEach((k, v) -> exchange.getRequestHeaders().put(new HttpString(k), (String)v));
        }

        // handle all response header
        List<String> responseHeaderRemove = config.getResponseRemoveList();
        if(responseHeaderRemove != null) {
            responseHeaderRemove.forEach(s -> exchange.getResponseHeaders().remove(s));
        }
        Map<String, Object> responseHeaderUpdate = config.getResponseUpdateMap();
        if(responseHeaderUpdate != null) {
            responseHeaderUpdate.forEach((k, v) -> exchange.getResponseHeaders().put(new HttpString(k), (String)v));
        }
        // handler per path prefix header if configured.
        Map<String, Object> pathPrefixHeader = config.getPathPrefixHeader();
        if(pathPrefixHeader != null) {
            String requestPath = exchange.getRequestPath();
            for(Map.Entry<String, Object> entry: config.getPathPrefixHeader().entrySet()) {
                if(requestPath.startsWith(entry.getKey())) {
                    if(logger.isTraceEnabled()) logger.trace("found with requestPath = " + requestPath + " prefix = " + entry.getKey());
                    Map<String, Object> valueMap = (Map<String, Object>)entry.getValue();
                    // handle the request header for the request path
                    Map<String, Object> requestHeaderMap = (Map<String, Object>)valueMap.get(HeaderConfig.REQUEST);
                    if(requestHeaderMap != null) {
                        List<String> requestHeaderRemoveList = (List<String>)requestHeaderMap.get(HeaderConfig.REMOVE);
                        if(requestHeaderRemoveList != null) {
                            requestHeaderRemoveList.forEach(s -> exchange.getRequestHeaders().remove(s));
                        }
                        Map<String, Object> requestHeaderUpdateMap = (Map<String, Object>)requestHeaderMap.get(HeaderConfig.UPDATE);
                        if(requestHeaderUpdateMap != null) {
                            requestHeaderUpdateMap.forEach((k, v) -> exchange.getRequestHeaders().put(new HttpString(k), (String)v));
                        }
                    }
                    // handle the response header for the request path
                    Map<String, Object> responseHeaderMap = (Map<String, Object>)valueMap.get(HeaderConfig.RESPONSE);
                    if(responseHeaderMap != null) {
                        List<String> responseHeaderRemoveList = (List<String>)responseHeaderMap.get(HeaderConfig.REMOVE);
                        if(responseHeaderRemoveList != null) {
                            responseHeaderRemoveList.forEach(s -> exchange.getResponseHeaders().remove(s));
                        }
                        Map<String, Object> responseHeaderUpdateMap = (Map<String, Object>)responseHeaderMap.get(HeaderConfig.UPDATE);
                        if(responseHeaderUpdateMap != null) {
                            responseHeaderUpdateMap.forEach((k, v) -> exchange.getResponseHeaders().put(new HttpString(k), (String)v));
                        }
                    }
                }
            }
        }
        if(logger.isDebugEnabled()) logger.debug("HeaderHandler.handleRequest ends.");
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
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(HeaderHandler.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(HeaderHandler.class.getName(), config.getMappedConfig(), null);
    }
}
