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
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.StringUtils;
import io.undertow.Handlers;
import io.undertow.server.ConduitWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.ConduitFactory;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.conduits.StreamSinkConduit;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
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
    static final String DUMP_METHOD_PREFIX = "dump";
    static final String DUMP_REQUEST_METHOD_PREFIX = "dumpRequest";
    static final String DUMP_RESPONSE_METHOD_PREFIX = "dumpResponse";
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

    private static int START_LEVEL = -1;

    public static Map<String, Object> config =
            Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME);

    private volatile HttpHandler next;

    private static boolean isEnabled = false;

    public DumpHandler() {

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

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
        Object requestConfig = config.get(REQUEST);
        Object responseConfig = config.get(RESPONSE);
        if(isEnabled()) {
            dumpHttpMessage(result, exchange, requestConfig, IDumpable.HttpMessageType.REQUEST);
            if(DumpHelper.checkOptionNotFalse(requestConfig)) {
                exchange.addResponseWrapper(new ConduitWrapper<StreamSinkConduit>() {
                    @Override
                    public StreamSinkConduit wrap(ConduitFactory<StreamSinkConduit> factory, HttpServerExchange exchange) {
                        return new StoreResponseStreamSinkConduit(factory.create(), exchange);
                    }
                });
            }
            exchange.addExchangeCompleteListener((exchange1, nextListener) ->{
                    dumpHttpMessage(result, exchange1, responseConfig, IDumpable.HttpMessageType.RESPONSE);
                    DumpHelper.logResult(result, START_LEVEL);
                    nextListener.proceed();
                });
        }
        Handler.next(exchange, next);
    }

    /**
     *
     * @param result
     * @param exchange
     * @param configObject
     * @param type IDumpable.HttpMessageType
     */
    private void dumpHttpMessage(Map<String, Object> result, HttpServerExchange exchange, Object configObject, IDumpable.HttpMessageType type) {
        Map<String, Object> httpMessageMap = new LinkedHashMap<>();
        DumpHelper.dumpBasedOnOption(configObject,
                new IDumpable(){
                    @Override
                    public void dumpOption(Boolean configObject) {
                        if(configObject){
                            for(String requestOption: DumpHelper.getSupportHttpMessageOptions(type)) {
                                //if request option is true, put all supported request child options with true
                                dumpHttpMessageOptions(requestOption, httpMessageMap, exchange, configObject, type);
                            }
                        }
                    }

                    @Override
                    public void dumpOption(Map requestConfigObject) {
                        Map<String, Object> requestConfigMap = ((Map)requestConfigObject);
                        for(String requestOption: REQUEST_OPTIONS) {
                            if(requestConfigMap.containsKey(requestOption)) {
                                dumpHttpMessageOptions(requestOption, httpMessageMap, exchange, requestConfigMap.get(requestOption), type);
                            }
                        }

                    }
                });
        if(httpMessageMap.size() > 0) {
            result.put(type.name(), httpMessageMap);
        }
    }

    //Based on option name inside "request" or "response", call related handle method. e.g.  "cookies: true" inside "header", will call "dumpCookie"
    private void dumpHttpMessageOptions(String requestOption, Map<String, Object> result, HttpServerExchange exchange, Object requestConfigObject, IDumpable.HttpMessageType type) {
        String composedHttpMessageOptionMethodName = DUMP_METHOD_PREFIX + requestOption.substring(0, 1).toUpperCase() + requestOption.substring(1);
        try {
            Method dumpHttpMessageOptionMethod = DumpHandler.class.getDeclaredMethod(composedHttpMessageOptionMethodName, Map.class, HttpServerExchange.class, Object.class, IDumpable.HttpMessageType.class);
            dumpHttpMessageOptionMethod.invoke(this, result, exchange, requestConfigObject, type);
        } catch (NoSuchMethodException e) {
            logger.error("Cannot find a method for this request option: {}", requestOption);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    //configObject is on "header" level
    private void dumpHeaders(Map<String, Object> result, HttpServerExchange exchange, Object configObject, IDumpable.HttpMessageType type) {
        Map<String, Object> headerMap = new LinkedHashMap<>();
        //all raw unfiltered headers
        HeaderMap headers = type.equals(IDumpable.HttpMessageType.RESPONSE) ? exchange.getResponseHeaders(): exchange.getRequestHeaders();
        DumpHelper.dumpBasedOnOption(configObject,
                new IDumpable() {
                    @Override
                    public void dumpOption(Boolean configObject) {
                        if (configObject) {
                            for (HeaderValues header : headers) {
                                for (String value : header) {
                                    headerMap.put(header.getHeaderName().toString(), value);
                                }
                            }
                        }
                    }

                    @Override
                    public void dumpOption(List<?> configObject) {
                        // configObject is a list of header names
                        List headerList = (List<String>) configObject;
                        for (HeaderValues header : headers) {
                            for (String value : header) {
                                String name = header.getHeaderName().toString();
                                if (headerList.contains(name)) {
                                    headerMap.put(header.getHeaderName().toString(), value);
                                }
                            }
                        }
                    }
                });
        if (headerMap.size() > 0) {
            result.put(HEADERS, headerMap);
        }
    }

    private void dumpCookies(Map<String, Object> result, HttpServerExchange exchange, Object configObject, IDumpable.HttpMessageType type) {
        Map<String, Cookie> cookiesMap = type.equals(IDumpable.HttpMessageType.RESPONSE) ? exchange.getResponseCookies() : exchange.getRequestCookies();
        DumpHelper.dumpBasedOnOption(configObject,
                new IDumpable() {
                    @Override
                    public void dumpOption(Boolean configObject) {
                        if (configObject) {
                            result.put(COOKIES, cookiesMap);
                        }
                    }
                });
    }

    private void dumpQueryParameters(Map<String, Object> result, HttpServerExchange exchange, Object configObject, IDumpable.HttpMessageType type) {
        if(type.equals(IDumpable.HttpMessageType.RESPONSE)) {
            logger.error("Http type: \'{}\' doesn't support \'{}\' option", type.name(), configObject.toString());
            return;
        }
        DumpHelper.dumpBasedOnOption(configObject,
                new IDumpable() {
                    @Override
                    public void dumpOption(Boolean configObject) {
                        if(configObject) {
                            result.put(QUERY_PARAMETERS, exchange.getQueryParameters());
                        }
                    }
                });
    }

    private void dumpBody(Map<String, Object> result, HttpServerExchange exchange, Object configObject, IDumpable.HttpMessageType type) {

        if(type.equals(IDumpable.HttpMessageType.RESPONSE)) {
            DumpHelper.dumpBasedOnOption(configObject,
                    new IDumpable() {
                        @Override
                        public void dumpOption(Boolean configObject) {
                            if(configObject) {
                                String responseBody = new String(exchange.getAttachment(StoreResponseStreamSinkConduit.RESPONSE));
                                if(responseBody != null) {
                                    result.put(BODY, responseBody);
                                }
                            }
                        }
                    });
        } else {
            DumpHelper.dumpBasedOnOption(configObject,
                    new IDumpable() {
                        @Override
                        public void dumpOption(Boolean configObject) {
                            if(configObject) {
                                exchange.startBlocking();
                                String body = "";
                                InputStream inputStream = exchange.getInputStream();
                                try {
                                    body = StringUtils.inputStreamToString(inputStream, StandardCharsets.UTF_8);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                result.put(BODY, body);
                            }
                        }
                    });
        }
    }
}
