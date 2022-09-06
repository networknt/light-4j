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

package com.networknt.router;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.server.ServerConfig;
import com.networknt.url.HttpURL;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;

import java.util.Map;

import static com.networknt.httpstring.HttpStringConstants.SERVICE_ID;

/**
 * Router handle light-gateway.
 * It use gateway.egressIngressIndicator for config to trigger gateway to router request
 * If gateway.egressIngressIndicator is header, then gateway will lookup header key: service_id/service_url
 * If gateway.egressIngressIndicator is protocol, then gateway will check if protocol is https
 *
 * @author Gavin Chen
 */
public class GatewayRouterHandler extends RouterHandler implements MiddlewareHandler{


    private volatile HttpHandler next;
    public static final String ROUTER_CONFIG_NAME = "router";

    public static Map<String, Object> config = Config.getInstance().getJsonMapConfigNoCache(ROUTER_CONFIG_NAME);
    public static GatewayConfig gatewayConfig = (GatewayConfig)Config.getInstance().getJsonObjectConfig(GatewayConfig.CONFIG_NAME, GatewayConfig.class);

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        if (Constants.HEADER.equalsIgnoreCase(gatewayConfig.getEgressIngressIndicator())) {
            HeaderValues serviceIdHeader = httpServerExchange.getRequestHeaders().get(SERVICE_ID);
            String serviceId = serviceIdHeader != null ? serviceIdHeader.peekFirst() : null;
            String serviceUrl = httpServerExchange.getRequestHeaders().getFirst(HttpStringConstants.SERVICE_URL);
            if (serviceId != null || serviceUrl != null) {
                proxyHandler.handleRequest(httpServerExchange);
                // get the serviceId and put it into the request as callerId for metrics
                Map<String, Object> serverConfig = Config.getInstance().getJsonMapConfigNoCache(ServerConfig.CONFIG_NAME);
                if(serverConfig != null) {
                    httpServerExchange.getRequestHeaders().put(HttpStringConstants.CALLER_ID, (String)serverConfig.get(SERVICE_ID));
                }
            } else {
                Handler.next(httpServerExchange, next);
            }
        } else if (Constants.PROTOCOL.equalsIgnoreCase(gatewayConfig.getEgressIngressIndicator()) && HttpURL.PROTOCOL_HTTP.equalsIgnoreCase(httpServerExchange.getRequestScheme())){
            proxyHandler.handleRequest(httpServerExchange);
        } else {
            Handler.next(httpServerExchange, next);
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
        return true;
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(GatewayRouterHandler.class.getName(), config, null);
    }

}
