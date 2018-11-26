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
    private static final String DUMP_METHOD_PREFIX = "dump";
    static final String HEADERS = "headers";
    static final String COOKIES = "cookies";
    static final String QUERY_PARAMETERS = "queryParameters";
    static final String BODY = "body";
    static final String STATUS_CODE = "statusCode";
    static final String INDENT_SIZE = "indentSize";
    static final int DEFAULT_INDENT_SIZE = 4;
    static final String[] REQUEST_OPTIONS = {HEADERS, COOKIES, QUERY_PARAMETERS, BODY};
    static final String[] RESPONSE_OPTIONS = {HEADERS, COOKIES, BODY, STATUS_CODE};
    static final Logger logger = LoggerFactory.getLogger(DumpHandler.class);

    public static Map<String, Object> config =
            Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME);

    private volatile HttpHandler next;

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
        Object requestConfig = config.get(IDumpable.HttpMessageType.REQUEST.value());
        Object responseConfig = config.get(IDumpable.HttpMessageType.RESPONSE.value());
        if(isEnabled()) {
            //dump request info right away
            dumpHttpMessage(result, exchange, requestConfig, IDumpable.HttpMessageType.REQUEST);
            //if response config is not set to "false"
            if(DumpHelper.checkOptionNotFalse(responseConfig)) {
                //set Conduit to the conduit chain to store response body
                exchange.addResponseWrapper(new ConduitWrapper<StreamSinkConduit>() {
                    @Override
                    public StreamSinkConduit wrap(ConduitFactory<StreamSinkConduit> factory, HttpServerExchange exchange) {
                        return new StoreResponseStreamSinkConduit(factory.create(), exchange);
                    }
                });
            }
            //when complete exchange, dump http message info
            exchange.addExchangeCompleteListener((exchange1, nextListener) ->{
                    dumpHttpMessage(result, exchange1, responseConfig, IDumpable.HttpMessageType.RESPONSE);
                    DumpHelper.logResult(result, getIndentSize());
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

    /**
     * dump request/response Message based on response/request Option
     * @param result result to be logged
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
                                //if request/response option is true, put all supported request child options with true
                                dumpHttpMessageBasedOnOptionName(requestOption, httpMessageMap, exchange, true, type);
                            }
                        }
                    }

                    @Override
                    public void dumpOption(Map configObject) {
                        String[] configOptions = type == HttpMessageType.RESPONSE ? RESPONSE_OPTIONS : REQUEST_OPTIONS;
                        for(String requestOrResponseOption: configOptions) {
                            if(configObject.containsKey(requestOrResponseOption)) {
                                dumpHttpMessageBasedOnOptionName(requestOrResponseOption, httpMessageMap, exchange, configObject.get(requestOrResponseOption), type);
                            }
                        }

                    }
                });
        if(httpMessageMap.size() > 0) {
            result.put(type.name(), httpMessageMap);
        }
    }

    //Based on option name inside "request" or "response", call related handle method. e.g.  "cookies: true" inside "header", will call "dumpCookie"
    private void dumpHttpMessageBasedOnOptionName(String httpMessageOption, Map<String, Object> result, HttpServerExchange exchange, Object configObject, IDumpable.HttpMessageType type) {
        String composedHttpMessageOptionMethodName = DUMP_METHOD_PREFIX + httpMessageOption.substring(0, 1).toUpperCase() + httpMessageOption.substring(1);
        try {
            Method dumpHttpMessageOptionMethod = DumpHandler.class.getDeclaredMethod(composedHttpMessageOptionMethodName, Map.class, HttpServerExchange.class, Object.class, IDumpable.HttpMessageType.class);
            dumpHttpMessageOptionMethod.invoke(this, result, exchange, configObject, type);
        } catch (NoSuchMethodException e) {
            logger.error("Cannot find a method for this request option: {}", httpMessageOption);
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
                                //filter out listed headers
                                if (!headerList.contains(name)) {
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
                            Map<String, String> queryParametersMap = new LinkedHashMap<>();
                            exchange.getQueryParameters().forEach((k, v) ->
                                queryParametersMap.put(k, v.getFirst())
                            );
                            result.put(QUERY_PARAMETERS, queryParametersMap);
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
                                byte[] responseBodyAttachment = exchange.getAttachment(StoreResponseStreamSinkConduit.RESPONSE);
                                if(responseBodyAttachment != null) {
                                    String responseBody = new String();
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

    private void dumpStatusCode(Map<String, Object> result, HttpServerExchange exchange, Object configObject, IDumpable.HttpMessageType type) {
        if(type.equals(IDumpable.HttpMessageType.REQUEST)) {
            logger.error("Http type: \'{}\' doesn't support \'{}\' option", type.name(), configObject.toString());
            return;
        }

        DumpHelper.dumpBasedOnOption(configObject,
                new IDumpable() {
                    @Override
                    public void dumpOption(Boolean configObject) {
                        if(configObject) {
                            result.put(STATUS_CODE, String.valueOf(exchange.getStatusCode()));
                        }
                    }
                });

    }
}
