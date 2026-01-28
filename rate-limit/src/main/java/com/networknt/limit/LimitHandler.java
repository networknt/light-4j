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
    private volatile RateLimiter rateLimiter;
    private volatile LimitConfig config;
    private final String configName;
    private static final ObjectMapper mapper = Config.getInstance().getMapper();


    public LimitHandler() {
        this(LimitConfig.CONFIG_NAME);
    }

    public LimitHandler(String configName) {
        this.configName = configName;
        this.config = LimitConfig.load(configName);
        try {
            this.rateLimiter = new RateLimiter(config);
        } catch (Exception e) {
            logger.error("Exception in constructing RateLimiter", e);
        }
        logger.info("RateLimit started with key type {}", config.getKey().name());
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("LimitHandler.handleRequest starts.");
        LimitConfig newConfig = LimitConfig.load(configName);
        if (newConfig != config) {
            synchronized (this) {
                if (newConfig != config) {
                    config = newConfig;
                    rateLimiter = new RateLimiter(config);
                }
            }
        }
        RateLimitResponse rateLimitResponse = rateLimiter.handleRequest(exchange, config);
        if (rateLimitResponse.allow) {
            if(logger.isDebugEnabled()) logger.debug("LimitHandler.handleRequest ends.");
            // limit is not reached, return the limit, remaining and reset headers for client to manage the request flow.
            if(rateLimitResponse.getHeaders() != null && rateLimitResponse.getHeaders().get(Constants.RATELIMIT_LIMIT) != null)
                exchange.getResponseHeaders().add(new HttpString(Constants.RATELIMIT_LIMIT), rateLimitResponse.getHeaders().get(Constants.RATELIMIT_LIMIT));
            if(rateLimitResponse.getHeaders() != null && rateLimitResponse.getHeaders().get(Constants.RATELIMIT_REMAINING) != null)
                exchange.getResponseHeaders().add(new HttpString(Constants.RATELIMIT_REMAINING), rateLimitResponse.getHeaders().get(Constants.RATELIMIT_REMAINING));
            if(rateLimitResponse.getHeaders() != null && rateLimitResponse.getHeaders().get(Constants.RATELIMIT_RESET) != null)
                exchange.getResponseHeaders().add(new HttpString(Constants.RATELIMIT_RESET), rateLimitResponse.getHeaders().get(Constants.RATELIMIT_RESET));
            Handler.next(exchange, next);
        } else {
            // limit is reached, return the Retry-After header.
            exchange.getResponseHeaders().add(new HttpString(Constants.RATELIMIT_LIMIT), rateLimitResponse.getHeaders().get(Constants.RATELIMIT_LIMIT));
            exchange.getResponseHeaders().add(new HttpString(Constants.RATELIMIT_REMAINING), rateLimitResponse.getHeaders().get(Constants.RATELIMIT_REMAINING));
            exchange.getResponseHeaders().add(new HttpString(Constants.RATELIMIT_RESET), rateLimitResponse.getHeaders().get(Constants.RATELIMIT_RESET));
            exchange.getResponseHeaders().add(new HttpString(Constants.RETRY_AFTER), rateLimitResponse.getHeaders().get(Constants.RETRY_AFTER));
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
        return LimitConfig.load(configName).isEnabled();
    }

}
