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

package com.networknt.router.middleware;

import com.networknt.client.oauth.Jwt;
import com.networknt.client.oauth.OauthHelper;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.monad.Result;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This is a middleware handler that is responsible for getting a JWT access token from
 * OAuth 2.0 provider for the particular router client. The only token that is supported in
 * this handler is client credentials token as there is no user information available here.
 *
 * The client_id will be retrieved from client.yml and client_secret will be retrieved from
 * secret.yml
 *
 * This handler will also responsible for checking if the cached token is about to expired
 * or not. In which case, it will renew the token in another thread. When request comes and
 * the cached token is already expired, then it will block the request and go to the OAuth
 * provider to get a new token and then resume the request to the next handler in the chain.
 *
 * The logic is very similar with client module in light-4j but this is implemented in a
 * handler instead.
 *
 * This light-router is designed for standalone or client that is not implemented in Java
 * Otherwise, you should use client module instead of this one. In the future, we might
 * add Authorization Code grant type support by providing an endpoint in the light-router
 * to accept Authorization Code redirect and then get the token from OAuth 2.0 provider.
 *
 * There is no specific configuration file for this handler just to enable or disable it. If
 * you want to bypass this handler, you can comment it out from service.yml middleware
 * handler section or change the token.yml to disable it.
 *
 * Once the token is retrieved from OAuth 2.0 provider, it will be placed in the header as
 * Authorization Bearer token according to the OAuth 2.0 specification.
 *
 */
public class TokenHandler implements MiddlewareHandler {
    public static final String CONFIG_NAME = "token";
    public static final String ENABLED = "enabled";

    public static Map<String, Object> config = Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME);
    static Logger logger = LoggerFactory.getLogger(TokenHandler.class);
    private volatile HttpHandler next;
    // Cached jwt token for this handler on behalf of a client.
    private final Jwt cachedJwt = new Jwt();
    public TokenHandler() { }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        // check if there is a bear token in the authorization header in the request. If this
        // is one, then this must be the subject token that is linked to the original user.
        // We will keep this token in the Authorization header but create a new token with
        // client credentials grant type with scopes for the particular client. (Can we just
        // assume that the subject token has the scope already?)
        Result result = OauthHelper.populateCCToken(cachedJwt);
        if(result.isFailure()) {
            logger.error("cannot populate or renew jwt for client credential grant type");
            OauthHelper.sendStatusToResponse(exchange, result.getError());
            return;
        }
        exchange.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer " + cachedJwt.getJwt());
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
        Object object = config.get(ENABLED);
        return object != null && (Boolean) object;
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(TokenHandler.class.getName(), config, null);
    }
}
