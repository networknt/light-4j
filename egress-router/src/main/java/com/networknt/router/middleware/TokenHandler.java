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

import com.networknt.client.ClientConfig;
import com.networknt.client.oauth.Jwt;
import com.networknt.client.oauth.OauthHelper;
import com.networknt.client.oauth.TokenKeyRequest;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.monad.Result;
import com.networknt.status.Status;
import com.networknt.utility.ConcurrentHashSet;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a middleware handler that is responsible for getting a JWT access token from
 * OAuth 2.0 provider for the particular router client. The only token that is supported in
 * this handler is client credentials token as there is no user information available here.
 *
 * The client_id and client_secret will be retrieved from client.yml and client_secret should
 * be encrypted or set as an environment variable. In Kubernetes cluster, you can create a
 * sealed secret for it.
 *
 * This handler will also responsible for checking if the cached token is about expired
 * or not. In which case, it will renew the token in another thread. When request comes and
 * the cached token is already expired, then it will block the request and go to the OAuth
 * provider to get a new token and then resume the request to the next handler in the chain.
 *
 * The logic is very similar with client module in light-4j but this is implemented in a
 * handler instead. Multiple OAuth 2.0 providers are supported and the token cache strategy
 * can be defined based on your OAuth 2.0 providers.
 *
 * This light-router is designed for standalone or client that is not implemented in Java
 * Otherwise, you should use client module instead of this one. In the future, we might
 * add Authorization Code grant type support by providing an endpoint in the light-router
 * to accept Authorization Code redirect and then get the token from OAuth 2.0 provider.
 *
 * There is no specific configuration file for this handler just to enable or disable it. If
 * you want to bypass this handler, you can comment it out from handler.yml middleware
 * handler section or change the token.yml to disable it.
 *
 * Once the token is retrieved from OAuth 2.0 provider, it will be placed in the header as
 * Authorization Bearer token according to the OAuth 2.0 specification.
 *
 */
public class TokenHandler implements MiddlewareHandler {
    private static final String HANDLER_DEPENDENCY_ERROR = "ERR10074";

    private static TokenConfig config;
    static Logger logger = LoggerFactory.getLogger(TokenHandler.class);
    protected volatile HttpHandler next;
    // Cached jwt token for this handler on behalf of a client by serviceId as the key
    private final Map<String, Jwt> cache = new ConcurrentHashMap();
    public TokenHandler() {
        if(logger.isInfoEnabled()) logger.info("TokenHandler is loaded.");
        config = TokenConfig.load();
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        // This handler must be put after the prefix or dict handler so that the serviceId is
        // readily available in the header resolved by the path or the endpoint from the request.
        if(logger.isTraceEnabled()) logger.trace("TokenHandler.handleRequest is called.");
        String requestPath = exchange.getRequestPath();
        // this handler will only work with a list of applied path prefixes in the token.yml config file.
        if (config.getAppliedPathPrefixes() != null && config.getAppliedPathPrefixes().stream().anyMatch(s -> requestPath.startsWith(s))) {
            HeaderValues headerValues = exchange.getRequestHeaders().get(HttpStringConstants.SERVICE_ID);
            String serviceId = null;
            if(headerValues != null) serviceId = headerValues.getFirst();
            if(serviceId == null) {
                // this handler should be before the router and after the handler to resolve the serviceId from path
                // or endpoint like the PathPrefixServiceHandler or ServiceDictHandler.
                logger.error("The serviceId cannot be resolved. Do you have PathPrefixServiceHandler or ServiceDictHandler before this handler?");
                setExchangeStatus(exchange, HANDLER_DEPENDENCY_ERROR, "TokenHandler", "PathPrefixServiceHandler");
                return;
            }
            ClientConfig clientConfig = ClientConfig.get();
            Map<String, Object> tokenConfig = clientConfig.getTokenConfig();
            Map<String, Object> ccConfig = (Map<String, Object>)tokenConfig.get(ClientConfig.CLIENT_CREDENTIALS);

            Jwt cachedJwt = cache.get(serviceId);
            // get a new token if cachedJwt is null or the jwt is about expired.
            if(cachedJwt == null || cachedJwt.getExpire() - Long.valueOf((Integer)tokenConfig.get(ClientConfig.TOKEN_RENEW_BEFORE_EXPIRED)) < System.currentTimeMillis()) {
                Jwt.Key key = new Jwt.Key(serviceId);
                cachedJwt = new Jwt(key); // create a new instance if the cache is empty for the serviceId.

                if(clientConfig.isMultipleAuthServers()) {
                    // get the right client credentials configuration based on the serviceId
                    Map<String, Object> serviceIdAuthServers = (Map<String, Object>)ccConfig.get(ClientConfig.SERVICE_ID_AUTH_SERVERS);
                    if(serviceIdAuthServers == null) {
                        throw new RuntimeException("serviceIdAuthServers property is missing in the token client credentials configuration");
                    }
                    Map<String, Object> authServerConfig = (Map<String, Object>)serviceIdAuthServers.get(serviceId);
                    // overwrite some elements in the auth server config if it is not defined.
                    if(authServerConfig.get(ClientConfig.PROXY_HOST) == null) authServerConfig.put(ClientConfig.PROXY_HOST, tokenConfig.get(ClientConfig.PROXY_HOST));
                    if(authServerConfig.get(ClientConfig.PROXY_PORT) == null) authServerConfig.put(ClientConfig.PROXY_PORT, tokenConfig.get(ClientConfig.PROXY_PORT));
                    if(authServerConfig.get(ClientConfig.TOKEN_RENEW_BEFORE_EXPIRED) == null) authServerConfig.put(ClientConfig.TOKEN_RENEW_BEFORE_EXPIRED, tokenConfig.get(ClientConfig.TOKEN_RENEW_BEFORE_EXPIRED));
                    if(authServerConfig.get(ClientConfig.EXPIRED_REFRESH_RETRY_DELAY) == null) authServerConfig.put(ClientConfig.EXPIRED_REFRESH_RETRY_DELAY, tokenConfig.get(ClientConfig.EXPIRED_REFRESH_RETRY_DELAY));
                    if(authServerConfig.get(ClientConfig.EARLY_REFRESH_RETRY_DELAY) == null) authServerConfig.put(ClientConfig.EARLY_REFRESH_RETRY_DELAY, tokenConfig.get(ClientConfig.EARLY_REFRESH_RETRY_DELAY));
                    cachedJwt.setCcConfig(authServerConfig);
                } else {
                    // only one client credentials configuration, populate some common elements to the ccConfig from tokenConfig.
                    ccConfig.put(ClientConfig.PROXY_HOST, tokenConfig.get(ClientConfig.PROXY_HOST));
                    ccConfig.put(ClientConfig.PROXY_PORT, tokenConfig.get(ClientConfig.PROXY_PORT));
                    ccConfig.put(ClientConfig.TOKEN_RENEW_BEFORE_EXPIRED, tokenConfig.get(ClientConfig.TOKEN_RENEW_BEFORE_EXPIRED));
                    ccConfig.put(ClientConfig.EXPIRED_REFRESH_RETRY_DELAY, tokenConfig.get(ClientConfig.EXPIRED_REFRESH_RETRY_DELAY));
                    ccConfig.put(ClientConfig.EARLY_REFRESH_RETRY_DELAY, tokenConfig.get(ClientConfig.EARLY_REFRESH_RETRY_DELAY));
                    cachedJwt.setCcConfig(ccConfig);
                }
                Result result = OauthHelper.populateCCToken(cachedJwt);
                if(result.isFailure()) {
                    logger.error("Cannot populate or renew jwt for client credential grant type: " + result.getError().toString());
                    setExchangeStatus(exchange, result.getError());
                    return;
                }
                // put the cachedJwt to the cache.
                cache.put(serviceId, cachedJwt);
            }
            // check if there is a bear token in the authorization header in the request. If there
            // is one, then this must be the subject token that is linked to the original user.
            // We will keep this token in the Authorization header but create a new token with
            // client credentials grant type with scopes for the particular client. (Can we just
            // assume that the subject token has the scope already?)
            String token = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
            if(token == null) {
                if(logger.isTraceEnabled()) logger.trace("Adding jwt token to Authorization header with Bearer " + cachedJwt.getJwt().substring(0, 20));
                exchange.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer " + cachedJwt.getJwt());
            } else {
                if(logger.isTraceEnabled()) {
                    logger.trace("Authorization header is used with " + token.substring(0, 20));
                    logger.trace("Adding jwt token to X-Scope-Token header with Bearer " + cachedJwt.getJwt().substring(0, 20));
                }
                exchange.getRequestHeaders().put(HttpStringConstants.SCOPE_TOKEN, "Bearer " + cachedJwt.getJwt());
            }
        }
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
        ModuleRegistry.registerModule(TokenHandler.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(TokenHandler.class.getName(), config.getMappedConfig(), null);
    }
}
