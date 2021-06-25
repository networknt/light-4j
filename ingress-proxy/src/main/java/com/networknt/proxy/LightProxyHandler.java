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

package com.networknt.proxy;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;


/**
 * This is a wrapper class for LightProxyHandler as it is implemented as final. This class implements
 * the HttpHandler which can be injected into the handler.yml configuration file as another option
 * for the handlers injection. The other option is to use RouterHandlerProvider in service.yml file.
 *
 * @author Steve Hu
 */
public class LightProxyHandler implements HttpHandler {
    static final String CONFIG_NAME = "proxy";
    static final Logger logger = LoggerFactory.getLogger(LightProxyHandler.class);
    static ProxyConfig config = (ProxyConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ProxyConfig.class);

    ProxyHandler proxyHandler;

    public LightProxyHandler() {
        List<String> hosts = Arrays.asList(config.getHosts().split(","));
        if(config.httpsEnabled) {
            LoadBalancingProxyClient loadBalancer = new LoadBalancingProxyClient()
                    .setConnectionsPerThread(config.getConnectionsPerThread());
            hosts.forEach(handlingConsumerWrapper(host -> loadBalancer.addHost(new URI(host), Http2Client.getInstance().getDefaultXnioSsl()), URISyntaxException.class));
            proxyHandler = ProxyHandler.builder()
                    .setProxyClient(loadBalancer)
                    .setMaxConnectionRetries(config.maxConnectionRetries)
                    .setMaxRequestTime(config.maxRequestTime)
                    .setReuseXForwarded(config.reuseXForwarded)
                    .setRewriteHostHeader(config.rewriteHostHeader)
                    .setNext(ResponseCodeHandler.HANDLE_404)
                    .build();
        } else {
            LoadBalancingProxyClient loadBalancer = new LoadBalancingProxyClient()
                    .setConnectionsPerThread(config.getConnectionsPerThread());
            hosts.forEach(handlingConsumerWrapper(host -> loadBalancer.addHost(new URI(host)), URISyntaxException.class));
            proxyHandler = ProxyHandler.builder()
                    .setProxyClient(loadBalancer)
                    .setMaxConnectionRetries(config.maxConnectionRetries)
                    .setMaxRequestTime(config.maxRequestTime)
                    .setReuseXForwarded(config.reuseXForwarded)
                    .setRewriteHostHeader(config.rewriteHostHeader)
                    .setNext(ResponseCodeHandler.HANDLE_404)
                    .build();
        }
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        proxyHandler.handleRequest(httpServerExchange);
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T, E extends Exception> {
        void accept(T t) throws E;
    }
    static <T, E extends Exception> Consumer<T> handlingConsumerWrapper(
            ThrowingConsumer<T, E> throwingConsumer, Class<E> exceptionClass) {

        return i -> {
            try {
                throwingConsumer.accept(i);
            } catch (Exception ex) {
                try {
                    E exCast = exceptionClass.cast(ex);
                    logger.error("Exception occured :", ex);
                } catch (ClassCastException ccEx) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

}
