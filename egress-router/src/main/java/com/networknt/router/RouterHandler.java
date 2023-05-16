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

import com.networknt.client.Http2Client;
import com.networknt.handler.Handler;
import com.networknt.handler.ProxyHandler;
import com.networknt.metrics.MetricsConfig;
import com.networknt.metrics.AbstractMetricsHandler;
import com.networknt.utility.ModuleRegistry;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingRouterProxyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.util.Map;

/**
 * This is a wrapper class for ProxyHandler as it is implemented as final. This class implements
 * the HttpHandler which can be injected into the handler.yml configuration file as another option
 * for the handler injection. The other option is to use RouterHandlerProvider in service.yml file.
 *
 * @author Steve Hu
 */
public class RouterHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(RouterHandler.class);
    private static RouterConfig config;

    protected static ProxyHandler proxyHandler;
    protected static AbstractMetricsHandler metricsHandler;

    public RouterHandler() {
        config = RouterConfig.load();
        ModuleRegistry.registerModule(RouterHandler.class.getName(), config.getMappedConfig(), null);
        // As we are building a client side router for the light platform, the assumption is the server will
        // be on HTTP 2.0 TSL always. No need to handle HTTP 1.1 case here.
        LoadBalancingRouterProxyClient client = new LoadBalancingRouterProxyClient();
        if(config.httpsEnabled) client.setSsl(Http2Client.getInstance().getDefaultXnioSsl());
        if(config.http2Enabled) {
            client.setOptionMap(OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));
        } else {
            client.setOptionMap(OptionMap.EMPTY);
        }
        proxyHandler = ProxyHandler.builder()
                .setProxyClient(client)
                .setMaxConnectionRetries(config.maxConnectionRetries)
                .setMaxQueueSize(config.maxQueueSize)
                .setMaxRequestTime(config.maxRequestTime)
                .setPathPrefixMaxRequestTime(config.pathPrefixMaxRequestTime)
                .setReuseXForwarded(config.reuseXForwarded)
                .setRewriteHostHeader(config.rewriteHostHeader)
                .setUrlRewriteRules(config.urlRewriteRules)
                .setMethodRewriteRules(config.methodRewriteRules)
                .setQueryParamRewriteRules(config.queryParamRewriteRules)
                .setHeaderRewriteRules(config.headerRewriteRules)
                .setNext(ResponseCodeHandler.HANDLE_404)
                .build();
        if(config.isMetricsInjection()) {
            // get the metrics handler from the handler chain for metrics registration. If we cannot get the
            // metrics handler, then an error message will be logged.
            Map<String, HttpHandler> handlers = Handler.getHandlers();
            metricsHandler = (AbstractMetricsHandler) handlers.get(MetricsConfig.CONFIG_NAME);
            if(metricsHandler == null) {
                logger.error("An instance of MetricsHandler is not configured in the handler.yml.");
            }
        }
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("RouterHandler.handleRequest starts.");
        long startTime = System.nanoTime();
        proxyHandler.handleRequest(httpServerExchange);
        if(config.isMetricsInjection() && metricsHandler != null) metricsHandler.injectMetrics(httpServerExchange, startTime, config.getMetricsName());
        if(logger.isDebugEnabled()) logger.debug("RouterHandler.handleRequest ends.");
    }

    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(RouterHandler.class.getName(), config.getMappedConfig(), null);
        LoadBalancingRouterProxyClient client = new LoadBalancingRouterProxyClient();
        if(config.httpsEnabled) client.setSsl(Http2Client.getInstance().getDefaultXnioSsl());
        if(config.http2Enabled) {
            client.setOptionMap(OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));
        } else {
            client.setOptionMap(OptionMap.EMPTY);
        }
        proxyHandler = ProxyHandler.builder()
                .setProxyClient(client)
                .setMaxConnectionRetries(config.maxConnectionRetries)
                .setMaxQueueSize(config.maxQueueSize)
                .setMaxRequestTime(config.maxRequestTime)
                .setPathPrefixMaxRequestTime(config.pathPrefixMaxRequestTime)
                .setReuseXForwarded(config.reuseXForwarded)
                .setRewriteHostHeader(config.rewriteHostHeader)
                .setUrlRewriteRules(config.urlRewriteRules)
                .setMethodRewriteRules(config.methodRewriteRules)
                .setQueryParamRewriteRules(config.queryParamRewriteRules)
                .setHeaderRewriteRules(config.headerRewriteRules)
                .setNext(ResponseCodeHandler.HANDLE_404)
                .build();
        if(config.isMetricsInjection()) {
            // get the metrics handler from the handler chain for metrics registration. If we cannot get the
            // metrics handler, then an error message will be logged.
            Map<String, HttpHandler> handlers = Handler.getHandlers();
            metricsHandler = (AbstractMetricsHandler) handlers.get(MetricsConfig.CONFIG_NAME);
            if(metricsHandler == null) {
                logger.error("An instance of MetricsHandler is not configured in the handler.yml.");
            }
        }
    }
}
