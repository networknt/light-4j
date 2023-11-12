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

package com.networknt.limit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.status.HttpStatus;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A handler which limits the maximum number of concurrent requests.  Requests beyond the limit will
 * be queued with limited size of queue. If the queue is full, then request will be dropped.
 *
 * @author Steve Hu
 */
public class LimitHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(LimitHandler.class);

    private volatile HttpHandler next;
    private static RateLimiter rateLimiter;
    private final LimitConfig config;
    private static final ObjectMapper mapper = Config.getInstance().getMapper();


    public LimitHandler() throws Exception{
        config = LimitConfig.load();
        logger.info("RateLimit started with key type:" + config.getKey().name());
        rateLimiter = new RateLimiter(config);
    }

    /**
     * This is a constructor for test cases only. Please don't use it.
     *
     * @param cfg limit config
     * @throws Exception thrown when config is wrong.
     *
     */
    @Deprecated
    public LimitHandler(LimitConfig cfg) throws Exception{
        config = cfg;
        logger.info("RateLimit started with key type:" + config.getKey().name());
        rateLimiter = new RateLimiter(cfg);
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("LimitHandler.handleRequest starts.");
        RateLimitResponse rateLimitResponse = rateLimiter.handleRequest(exchange, config.getKey());
        if (rateLimitResponse.allow) {
            if(logger.isDebugEnabled()) logger.debug("LimitHandler.handleRequest ends.");
            Handler.next(exchange, next);
        } else {
            exchange.getResponseHeaders().add(new HttpString(Constants.RATELIMIT_LIMIT), rateLimitResponse.getHeaders().get(Constants.RATELIMIT_LIMIT));
            exchange.getResponseHeaders().add(new HttpString(Constants.RATELIMIT_REMAINING), rateLimitResponse.getHeaders().get(Constants.RATELIMIT_REMAINING));
            exchange.getResponseHeaders().add(new HttpString(Constants.RATELIMIT_RESET), rateLimitResponse.getHeaders().get(Constants.RATELIMIT_RESET));

            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            int statusCode = config.getErrorCode()==0 ? HttpStatus.TOO_MANY_REQUESTS.value():config.getErrorCode();
            exchange.setStatusCode(statusCode);
            if(logger.isDebugEnabled()) logger.warn("LimitHandler.handleRequest ends with an error code {}", statusCode);
            exchange.getResponseSender().send(mapper.writeValueAsString(rateLimitResponse));
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
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(LimitConfig.CONFIG_NAME, LimitHandler.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void reload() {
        config.reload();
        try {
            rateLimiter = new RateLimiter(config);
        } catch (Exception e) {
            logger.error("Failed to recreate RateLimiter with reloaded config.", e);
        }
        // after reload, we need to update the config in the module registry to ensure that server info returns the latest configuration.
        ModuleRegistry.registerModule(LimitConfig.CONFIG_NAME, LimitHandler.class.getName(), config.getMappedConfig(), null);
        if(logger.isInfoEnabled()) logger.info("LimitHandler is reloaded.");
    }
}
