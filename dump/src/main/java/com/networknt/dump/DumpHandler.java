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
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler that dumps request and response to a log based on the dump.json config
 * <p>
 * Created by steve on 01/09/16.
 */
public class DumpHandler implements MiddlewareHandler {
    public static final String CONFIG_NAME = "dump";
    public static final String ENABLED = "enabled";
    static final String REQUEST = "request";
    static final String RESPONSE = "response";
    static final String HEADERS = "headers";
    static final String COOKIES = "cookies";
    static final String QUERY_PARAMETERS = "queryParameters";

    static final Logger audit = LoggerFactory.getLogger(Constants.AUDIT_LOGGER);
    static final Logger logger = LoggerFactory.getLogger(DumpHandler.class);

    public static Map<String, Object> config;


    private volatile HttpHandler next;

    static {
        config = Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME);
    }


    public DumpHandler() {

    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
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
        Object object = config.get(ENABLED);
        return object != null && (Boolean)object == true;
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(DumpHandler.class.getName(), config, null);
    }

    private void dumpRequest(Map<String, Object> result, HttpServerExchange exchange, Object configObject) {
        Map<String, Object> requestMap = new LinkedHashMap<>();
        if (configObject instanceof Boolean) {
            if ((Boolean) configObject) {

            }
        } else if (configObject instanceof List<?>) {

        }
    }

    private void dumpRequestHeaders(Map<String, Object> result, HttpServerExchange exchange, Object configObject) {
        Map<String, Object> headerMap = new LinkedHashMap<>();
        if (configObject instanceof Boolean) {
            if ((Boolean) configObject) {
                for (HeaderValues header : exchange.getRequestHeaders()) {
                    for (String value : header) {
                        headerMap.put(header.getHeaderName().toString(), value);
                    }
                }
            }
        } else if (configObject instanceof List<?>) {
            // configObject is a list of header names
            List headerList = (List<String>) configObject;
            for (HeaderValues header : exchange.getRequestHeaders()) {
                for (String value : header) {
                    String name = header.getHeaderName().toString();
                    if (headerList.contains(name)) {
                        headerMap.put(header.getHeaderName().toString(), value);
                    }
                }
            }
        } else {
            logger.error("Header configuration is incorrect.");
        }
        if (headerMap.size() > 0) {
            result.put(HEADERS, headerMap);
        }
    }


}
