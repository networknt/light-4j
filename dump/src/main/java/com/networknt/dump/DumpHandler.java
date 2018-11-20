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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Handler that dumps request and response to a log based on the dump.json config
 * <p>
 * Created by steve on 01/09/16.
 * To handle options in request, should name method dumpRequest[OPTION_NAME]
 */
public class DumpHandler implements MiddlewareHandler {
    public static final String CONFIG_NAME = "dump";
    public static final String ENABLED = "enabled";
    static final String DUMP_METHOD_PREFIX = "dump";
    static final String DUMP_REQUEST_METHOD_PREFIX = "dumpRequest";
    static final String REQUEST = "request";
    static final String RESPONSE = "response";
    static final String HEADERS = "headers";
    static final String COOKIES = "cookies";
    static final String QUERY_PARAMETERS = "queryParameters";
    static final String BODY = "body";
    static final String STATUS_CODE = "statusCode";
    static final String CONTENT_LENGTH = "contentLength";
    static final String[] REQUEST_OPTIONS = {HEADERS, COOKIES, QUERY_PARAMETERS, BODY};
    static final String[] RESPONSE_OPTIONS = {HEADERS, COOKIES, BODY, STATUS_CODE, CONTENT_LENGTH};
    static final Logger audit = LoggerFactory.getLogger(Constants.AUDIT_LOGGER);
    static final Logger logger = LoggerFactory.getLogger(DumpHandler.class);

    public static Map<String, Object> config =
            Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME);

    private volatile HttpHandler next;

    private static boolean isEnabled = false;

    public DumpHandler() {

    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
        Object requestConfig = config.get(REQUEST);
        if(isEnabled()) {
            dumpRequest(result, exchange, requestConfig);
        }

        logResult(result);
    }

    private void logResult(Map<String, Object> result) {
        for(Entry<String, Object> entry: result.entrySet()) {
            if(entry.getValue() instanceof Map<?, ?>) {
                logResult((Map)entry.getValue());
            } else if(entry.getValue() instanceof String) {
                logger.info("{}: {}",entry.getKey(), (String)entry.getValue());
            } else {
                logger.debug("Cannot handle this type: {}", entry.getKey());
            }
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
        Object object = config.get(ENABLED);
        return object != null && (Boolean) object;
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(DumpHandler.class.getName(), config, null);
    }

    private void dumpRequest(Map<String, Object> result, HttpServerExchange exchange, Object requestConfigObject) {
        DumpHelper.dumpBasedOnOption(result, exchange, requestConfigObject,
                new IDumpable(){
                    @Override
                    public void dumpOption(Map<String, Object> result, HttpServerExchange exchange, Map requestConfigObject) {
                        Map<String, Object> requestMap = new LinkedHashMap<>();
                        Map<String, Object> requestConfigMap = ((Map)requestConfigObject);
                        for(String requestOption: REQUEST_OPTIONS) {
                            if(requestConfigMap.containsKey(requestOption)) {
                                dumpRequestOptions(requestOption, requestMap, exchange, requestConfigMap.get(requestOption));
                            }
                        }
                        if(requestMap.size() > 0) {
                            result.put(REQUEST, requestMap);
                        }
                    }

                    @Override
                    public void dumpOption(Map<String, Object> result, HttpServerExchange exchange, Boolean requestConfigObject) {
                        Map<String, Object> requestMap = new LinkedHashMap<>();
                        if ((Boolean) requestConfigObject) {
                            dumpRequestHeaders(result, exchange, true);
                        }
                        if(requestMap.size() > 0) {
                            result.put(REQUEST, requestMap);
                        }
                    }
                });
    }

    //Based on option name inside "request", call related handle method. e.g.  "cookies: true" inside "header", will call "dumpRequestCookie"
    private void dumpRequestOptions(String requestOption, Map<String, Object> result, HttpServerExchange exchange, Object requestConfigObject) {
        String dumpRequestOptionMethodName = DUMP_REQUEST_METHOD_PREFIX + requestOption.substring(0, 1).toUpperCase() + requestOption.substring(1);
        try {
            Method dumpRequestOptionMethod = DumpHandler.class.getDeclaredMethod(dumpRequestOptionMethodName, Map.class, HttpServerExchange.class, Object.class);
            dumpRequestOptionMethod.invoke(this, result, exchange, requestConfigObject);
        } catch (NoSuchMethodException e) {
            logger.error("Cannot find a method for this request option: {}", requestOption);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    //configObject is on "header" level
    private void dumpRequestHeaders(Map<String, Object> result, HttpServerExchange exchange, Object configObject) {
        DumpHelper.dumpBasedOnOption(result, exchange, configObject,
                new IDumpable() {
                    @Override
                    public void dumpOption(Map<String, Object> result, HttpServerExchange exchange, Boolean configObject) {
                        Map<String, Object> headerMap = new LinkedHashMap<>();
                        if (configObject) {
                            for (HeaderValues header : exchange.getRequestHeaders()) {
                                for (String value : header) {
                                    headerMap.put(header.getHeaderName().toString(), value);
                                }
                            }
                        }
                        if (headerMap.size() > 0) {
                            result.put(HEADERS, headerMap);
                        }
                    }

                    @Override
                    public void dumpOption(Map<String, Object> result, HttpServerExchange exchange, List<?> configObject) {
                        Map<String, Object> headerMap = new LinkedHashMap<>();
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
                        if (headerMap.size() > 0) {
                            result.put(HEADERS, headerMap);
                        }
                    }
                });
    }

    private void dumpRequestCookies(Map<String, Object> result, HttpServerExchange exchange, Object requestConfigObject) {
        DumpHelper.dumpBasedOnOption(result, exchange, requestConfigObject,
                new IDumpable() {
                    @Override
                    public void dumpOption(Map<String, Object> result, HttpServerExchange exchange, Boolean requestConfigObject) {
                        if (requestConfigObject) {
                            result.put(COOKIES, exchange.getRequestCookies());
                        }
                    }
                });
    }

    private void dumpRequestQueryParameters(Map<String, Object> result, HttpServerExchange exchange, Object requestConfigObject) {
        DumpHelper.dumpBasedOnOption(result, exchange, requestConfigObject,
                new IDumpable() {
                    @Override
                    public void dumpOption(Map<String, Object> result, HttpServerExchange exchange, Boolean requestConfigObject) {
                        if(requestConfigObject) {
                            result.put(QUERY_PARAMETERS, exchange.getQueryParameters());
                        }
                    }
                });
    }

    private void dumpRequestBody(Map<String, Object> result, HttpServerExchange exchange, Object requestConfigObject) {
        DumpHelper.dumpBasedOnOption(result, exchange, requestConfigObject,
                new IDumpable() {
                    @Override
                    public void dumpOption(Map<String, Object> result, HttpServerExchange exchange, Boolean requestConfigObject) {
                        if(requestConfigObject) {
                            exchange.startBlocking();
                            InputStream inputStream = exchange.getInputStream();
                            ByteArrayOutputStream body = new ByteArrayOutputStream();
                            byte[] buffer = new byte[1024];
                            int length;
                            try {
                                while ((length = inputStream.read(buffer)) != -1) {
                                    body.write(buffer, 0, length);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            result.put(BODY, body.toString());
                        }
                    }
                });
    }
}
