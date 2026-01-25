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

import com.networknt.client.ClientConfig;
import com.networknt.client.Http2Client;
import com.networknt.client.ssl.TLSConfig;
import com.networknt.config.Config;
import com.networknt.handler.ProxyHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.metrics.AbstractMetricsHandler;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingRouterProxyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.util.Map;

import static io.undertow.client.http.HttpClientProvider.DISABLE_HTTPS_ENDPOINT_IDENTIFICATION_PROPERTY;

/**
 * This is a wrapper class for ProxyHandler as it is implemented as final. This class implements
 * the HttpHandler which can be injected into the handler.yml configuration file as another option
 * for the handler injection. The other option is to use RouterHandlerProvider in service.yml file.
 *
 * @author Steve Hu
 */
public class RouterHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(RouterHandler.class);
    private RouterConfig config;

    protected volatile ProxyHandler proxyHandler;
    protected volatile AbstractMetricsHandler metricsHandler;

    public RouterHandler() {
        config = RouterConfig.load();
        buildProxy();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("RouterHandler.handleRequest starts.");
        RouterConfig newConfig = RouterConfig.load();
        if(newConfig != config) {
            synchronized (this) {
                newConfig = RouterConfig.load();
                if(newConfig != config) {
                    config = newConfig;
                    buildProxy();
                    if(logger.isInfoEnabled()) logger.info("RouterHandler is reloaded.");
                }
            }
        }

        if(metricsHandler == null) metricsHandler = AbstractMetricsHandler.lookupMetricsHandler();
        if(metricsHandler != null) {
            exchange.putAttachment(AttachmentConstants.METRICS_HANDLER, metricsHandler);
            exchange.putAttachment(AttachmentConstants.DOWNSTREAM_METRICS_NAME, config.getMetricsName());
            exchange.putAttachment(AttachmentConstants.DOWNSTREAM_METRICS_START, System.nanoTime());
        }
        proxyHandler.handleRequest(exchange);
        if(logger.isDebugEnabled()) logger.debug("RouterHandler.handleRequest ends.");
    }

    private void buildProxy() {
        ClientConfig clientConfig = ClientConfig.get();
        Map<String, Object> tlsMap = clientConfig.getTlsConfig();
        // disable the hostname verification based on the config. We need to do it here as the LoadBalancingRouterProxyClient uses the Undertow HttpClient.
        if(tlsMap == null || tlsMap.get(TLSConfig.VERIFY_HOSTNAME) == null || Boolean.FALSE.equals(Config.loadBooleanValue(TLSConfig.VERIFY_HOSTNAME, tlsMap.get(TLSConfig.VERIFY_HOSTNAME)))) {
            System.setProperty(DISABLE_HTTPS_ENDPOINT_IDENTIFICATION_PROPERTY, "true");
        }
        // As we are building a client side router for the light platform, the assumption is the server will
        // be on HTTP 2.0 TSL always. No need to handle HTTP 1.1 case here.
        LoadBalancingRouterProxyClient client = new LoadBalancingRouterProxyClient();
        if(config.isHttpsEnabled()) client.setSsl(Http2Client.getInstance().getDefaultXnioSsl());
        if(config.isHttp2Enabled()) {
            client.setOptionMap(OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));
        } else {
            client.setOptionMap(OptionMap.EMPTY);
        }
        proxyHandler = ProxyHandler.builder()
                .setProxyClient(client)
                .setMaxConnectionRetries(config.getMaxConnectionRetries())
                .setMaxQueueSize(config.getMaxQueueSize())
                .setMaxRequestTime(config.getMaxRequestTime())
                .setPathPrefixMaxRequestTime(config.getPathPrefixMaxRequestTime())
                .setReuseXForwarded(config.isReuseXForwarded())
                .setRewriteHostHeader(config.isRewriteHostHeader())
                .setUrlRewriteRules(config.getUrlRewriteRules())
                .setMethodRewriteRules(config.getMethodRewriteRules())
                .setQueryParamRewriteRules(config.getQueryParamRewriteRules())
                .setHeaderRewriteRules(config.getHeaderRewriteRules())
                .setNext(ResponseCodeHandler.HANDLE_404)
                .build();
        if(config.isMetricsInjection()) metricsHandler = AbstractMetricsHandler.lookupMetricsHandler();
    }

    public void reload() {
        // reload logic is now handled in handleRequest with hot reload
        config = RouterConfig.load();
        buildProxy();
    }
}
