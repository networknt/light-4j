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
import io.undertow.server.ConduitWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.ConduitFactory;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.conduits.StreamSinkConduit;

import java.util.List;
import java.util.Map;

/**
 * This is a handler that manipulate request and response headers based on the configuration.
 * <p>
 * Although one header key can support multiple values in HTTP, but it is not supported here.
 * If the key exists during update, the original value will be replaced by the new value.
 * <p>
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
        config = HeaderConfig.load();
    }

    /**
     * Please don't use this constructor. It is used by test case only to inject config object.
     *
     * @param cfg HeaderConfig
     * @deprecated
     */
    public HeaderHandler(HeaderConfig cfg) {
        config = cfg;
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
        logger.debug("HeaderHandler.handleRequest starts.");

        // handle all request header
        List<String> requestHeaderRemove = config.getRequestRemoveList();
        if (requestHeaderRemove != null) {
            requestHeaderRemove.forEach(s -> exchange.getRequestHeaders().remove(s));
        }

        Map<String, String> requestHeaderUpdate = config.getRequestUpdateMap();
        if (requestHeaderUpdate != null) {
            requestHeaderUpdate.forEach((k, v) -> exchange.getRequestHeaders().put(new HttpString(k), v));
        }

        // handle all response header
        List<String> responseHeaderRemove = config.getResponseRemoveList();
        if (responseHeaderRemove != null) {
            responseHeaderRemove.forEach(s -> exchange.getResponseHeaders().remove(s));
        }

        Map<String, String> responseHeaderUpdate = config.getResponseUpdateMap();
        if (responseHeaderUpdate != null) {
            responseHeaderUpdate.forEach((k, v) -> exchange.getResponseHeaders().put(new HttpString(k), v));
        }

        // handler per path prefix header if configured.
        Map<String, HeaderPathPrefixConfig> pathPrefixHeader = config.getPathPrefixHeader();

        if (pathPrefixHeader != null) {
            String requestPath = exchange.getRequestPath();

            for (Map.Entry<String, HeaderPathPrefixConfig> entry : config.getPathPrefixHeader().entrySet()) {

                if (requestPath.startsWith(entry.getKey())) {
                    logger.trace("found with requestPath = {} prefix = {}", requestPath, entry.getKey());

                    HeaderPathPrefixConfig pathPrefixConfig = entry.getValue();

                    // handle the request header for the request path
                    HeaderRequestConfig requestHeaderMap = pathPrefixConfig.getRequest();
                    if (requestHeaderMap != null) {

                        List<String> requestHeaderRemoveList = requestHeaderMap.getRemove();
                        if (requestHeaderRemoveList != null) {
                            requestHeaderRemoveList.forEach(s -> {
                                exchange.getRequestHeaders().remove(s);
                                logger.trace("remove request header {}", s);
                            });
                        }

                        Map<String, String> requestHeaderUpdateMap = requestHeaderMap.getUpdate();
                        if (requestHeaderUpdateMap != null) {
                            requestHeaderUpdateMap.forEach((k, v) -> {
                                exchange.getRequestHeaders().put(new HttpString(k), v);
                                logger.trace("update request header {} with value {}", k, v);
                            });
                        }
                    }

                    if (pathPrefixConfig.getResponse() != null) {

                        // Add response header manipulation after the response is ready to send back.
                        exchange.addResponseWrapper(new ConduitWrapper<>() {
                            final HeaderResponseConfig responseHeaderMap = pathPrefixConfig.getResponse();
                            @Override
                            public StreamSinkConduit wrap(ConduitFactory<StreamSinkConduit> factory, HttpServerExchange responseExchange) {
                                List<String> responseHeaderRemoveList = responseHeaderMap.getRemove();
                                if (responseHeaderRemoveList != null) {
                                    responseHeaderRemoveList.forEach(s -> {
                                        responseExchange.getResponseHeaders().remove(s);
                                        logger.trace("remove response header {}", s);
                                    });
                                }

                                Map<String, String> responseHeaderUpdateMap = responseHeaderMap.getUpdate();
                                if (responseHeaderUpdateMap != null) {
                                    responseHeaderUpdateMap.forEach((k, v) -> {
                                        responseExchange.getResponseHeaders().put(new HttpString(k), v);
                                        logger.trace("update response header {} with value {}", k, v);
                                    });
                                }
                                return factory.create();
                            }
                        });

                    }


                }
            }
        }
        logger.debug("HeaderHandler.handleRequest ends.");
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
        ModuleRegistry.registerModule(HeaderConfig.CONFIG_NAME, HeaderHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(HeaderConfig.CONFIG_NAME), null);
    }

    @Override
    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(HeaderConfig.CONFIG_NAME, HeaderHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(HeaderConfig.CONFIG_NAME), null);
        if (logger.isInfoEnabled()) logger.info("HeaderHandler is reloaded.");
    }
}
