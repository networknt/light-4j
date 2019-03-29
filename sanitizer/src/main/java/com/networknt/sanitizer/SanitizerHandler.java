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

package com.networknt.sanitizer;

import com.networknt.body.BodyHandler;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This is a middleware component that sanitize cross site scripting tags in request. As potentially
 * sanitizing body of the request, this middleware must be plugged into the chain after body parser.
 *
 * @author Steve Hu
 */
public class SanitizerHandler implements MiddlewareHandler {
    public static final String CONFIG_NAME = "sanitizer";


    static final Logger logger = LoggerFactory.getLogger(SanitizerHandler.class);

    static SanitizerConfig config = (SanitizerConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, SanitizerConfig.class);

    private volatile HttpHandler next;

    public SanitizerHandler() {

    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        String method = exchange.getRequestMethod().toString();
        if(config.isSanitizeHeader()) {
            HeaderMap headerMap = exchange.getRequestHeaders();
            if(headerMap != null) {
                for (HeaderValues values : headerMap) {
                    if (values != null) {
                        ListIterator<String> itValues = values.listIterator();
                        while (itValues.hasNext()) {
                            String value = Encode.forJavaScriptSource(itValues.next());
                            itValues.set(value);
                        }
                    }
                }
            }
        }
        /*
        It looks like undertow has done a lot of things to prevent passing in invalid query parameters,
        Until there are some use cases, this is not implemented.

        if(config.isSanitizeParameter()) {
            if (!exchange.getQueryString().isEmpty()) {
                final TreeMap<String, Deque<String>> newParams = new TreeMap<>();
                for (Map.Entry<String, Deque<String>> param : exchange.getQueryParameters().entrySet()) {
                    final Deque<String> newVales = new ArrayDeque<>(param.getValue().size());
                    for (String val : param.getValue()) {
                        newVales.add(Encode.forJavaScriptSource(val));
                    }
                    newParams.put(param.getKey(), newVales);
                }
                exchange.getQueryParameters().clear();
                exchange.getQueryParameters().putAll(newParams);
            }
        }
        */
        if(config.isSanitizeBody() && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method))) {
            // assume that body parser is installed before this middleware and body is parsed as a map.
            // we are talking about JSON api now.
            Object body = exchange.getAttachment(BodyHandler.REQUEST_BODY);
            if(body != null) {
                if(body instanceof List) {
                    encodeList((List<Map<String, Object>>)body);
                } else {
                    // assume it is a map here.
                    encodeNode((Map<String, Object>)body);
                }
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
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(SanitizerHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }

    public void encodeNode(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String)
                map.put(key, Encode.forJavaScriptSource((String) value));
            else if (value instanceof Map)
                encodeNode((Map) value);
            else if (value instanceof List) {
                encodeList((List)value);
            }
        }
    }

    public void encodeList(List list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof String) {
                list.set(i, Encode.forJavaScriptSource((String)list.get(i)));
            } else if(list.get(i) instanceof Map) {
                encodeNode((Map<String, Object>)list.get(i));
            } else if(list.get(i) instanceof List) {
                encodeList((List)list.get(i));
            }
        }
    }

}
