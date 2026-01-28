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

import com.networknt.client.AuthServerConfig;
import com.networknt.client.ClientConfig;
import com.networknt.client.OAuthTokenClientCredentialConfig;
import com.networknt.client.OAuthTokenConfig;
import com.networknt.client.oauth.Jwt;
import com.networknt.client.oauth.OauthHelper;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
import com.networknt.server.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.tokens.Token;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a middleware handler that is responsible for getting a JWT access token from
 * OAuth 2.0 provider for the particular router client. The only token that is supported in
 * this handler is client credentials token as there is no user information available here.
 * <p>
 * The client_id and client_secret will be retrieved from client.yml and client_secret should
 * be encrypted or set as an environment variable. In Kubernetes cluster, you can create a
 * sealed secret for it.
 * <p>
 * This handler will also responsible for checking if the cached token is about expired
 * or not. In which case, it will renew the token in another thread. When request comes and
 * the cached token is already expired, then it will block the request and go to the OAuth
 * provider to get a new token and then resume the request to the next handler in the chain.
 * <p>
 * The logic is very similar with client module in light-4j but this is implemented in a
 * handler instead. Multiple OAuth 2.0 providers are supported and the token cache strategy
 * can be defined based on your OAuth 2.0 providers.
 * <p>
 * This light-router is designed for standalone or client that is not implemented in Java
 * Otherwise, you should use client module instead of this one. In the future, we might
 * add Authorization Code grant type support by providing an endpoint in the light-router
 * to accept Authorization Code redirect and then get the token from OAuth 2.0 provider.
 * <p>
 * There is no specific configuration file for this handler just to enable or disable it. If
 * you want to bypass this handler, you can comment it out from handler.yml middleware
 * handler section or change the token.yml to disable it.
 * <p>
 * Once the token is retrieved from OAuth 2.0 provider, it will be placed in the header as
 * Authorization Bearer token according to the OAuth 2.0 specification.
 */
public class TokenHandler implements MiddlewareHandler {
    private static final String HANDLER_DEPENDENCY_ERROR = "ERR10074";

    static Logger logger = LoggerFactory.getLogger(TokenHandler.class);
    protected volatile HttpHandler next;

    // Cached jwt token for this handler on behalf of a client by serviceId as the key
    public final static Map<String, Jwt> cache = new ConcurrentHashMap<>();

    public TokenHandler() {
        TokenConfig.load();
        logger.info("TokenHandler is loaded.");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        // This handler must be put after the prefix or dict handler so that the serviceId is
        // readily available in the header resolved by the path or the endpoint from the request.
        logger.debug("TokenHandler.handleRequest starts.");
        TokenConfig config = TokenConfig.load();
        String requestPath = exchange.getRequestPath();

        // this handler will only work with a list of applied path prefixes in the token.yml config file.
        if (config.getAppliedPathPrefixes() != null && config.getAppliedPathPrefixes().stream().anyMatch(requestPath::startsWith)) {
            HeaderValues headerValues = exchange.getRequestHeaders().get(HttpStringConstants.SERVICE_ID);
            String serviceId = null;

            if (headerValues != null)
                serviceId = headerValues.getFirst();

            if (serviceId == null) {

                // this handler should be before the router and after the handler to resolve the serviceId from path
                // or endpoint like the PathPrefixServiceHandler or ServiceDictHandler.
                logger.error("The serviceId cannot be resolved. Do you have PathPrefixServiceHandler or ServiceDictHandler before this handler?");
                setExchangeStatus(exchange, HANDLER_DEPENDENCY_ERROR, "TokenHandler", "PathPrefixServiceHandler");
                logger.debug("TokenHandler.handleRequest ends with an error.");
                return;
            }

            Result<Jwt> result = getJwtToken(serviceId);

            if (result.isFailure()) {
                logger.error("Cannot populate or renew jwt for client credential grant type: {}", result.getError().toString());
                setExchangeStatus(exchange, result.getError());
                logger.debug("TokenHandler.handleRequest ends with an error.");
                return;

            } else {
                Jwt cachedJwt = result.getResult();
                // check if there is a bear token in the authorization header in the request. If there
                // is one, then this must be the subject token that is linked to the original user.
                // We will keep this token in the Authorization header but create a new token with
                // client credentials grant type with scopes for the particular client. (Can we just
                // assume that the subject token has the scope already?)
                String token = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
                if (token == null) {
                    if (logger.isTraceEnabled())
                        logger.trace("Adding jwt token to Authorization header with Bearer {}", cachedJwt.getJwt().substring(0, 20));
                    exchange.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer " + cachedJwt.getJwt());
                } else {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Authorization header is used with {}", token.length() > 10 ? token.substring(0, 10) : token); // it could be "Basic "
                        logger.trace("Adding jwt token to X-Scope-Token header with Bearer {}", cachedJwt.getJwt().substring(0, 20));
                    }
                    exchange.getRequestHeaders().put(HttpStringConstants.SCOPE_TOKEN, "Bearer " + cachedJwt.getJwt());
                }
            }
        }
        if (logger.isDebugEnabled()) logger.debug("TokenHandler.handleRequest ends.");
        Handler.next(exchange, next);
    }

    public static AuthServerConfig buildAuthServerConfig(final OAuthTokenConfig tokenConfig, final OAuthTokenClientCredentialConfig ccConfig) {
        AuthServerConfig authServerConfig = new AuthServerConfig();
        if(tokenConfig.getProxyHost() != null) authServerConfig.setProxyHost(tokenConfig.getProxyHost());
        if(tokenConfig.getProxyPort() != null) authServerConfig.setProxyPort(tokenConfig.getProxyPort());
        if(tokenConfig.getServerUrl() != null) authServerConfig.setServerUrl(tokenConfig.getServerUrl());
        if(tokenConfig.isEnableHttp2() != null) authServerConfig.setEnableHttp2(tokenConfig.isEnableHttp2());
        if(tokenConfig.getTokenRenewBeforeExpired() != null) authServerConfig.setTokenRenewBeforeExpired(tokenConfig.getTokenRenewBeforeExpired());
        if(tokenConfig.getEarlyRefreshRetryDelay() != null) authServerConfig.setEarlyRefreshRetryDelay(tokenConfig.getEarlyRefreshRetryDelay());
        if(tokenConfig.getExpiredRefreshRetryDelay() != null) authServerConfig.setExpiredRefreshRetryDelay(tokenConfig.getExpiredRefreshRetryDelay());
        if(ccConfig.getScope() != null) authServerConfig.setScope(ccConfig.getScope());
        if(ccConfig.getClientId() != null) authServerConfig.setClientId(ccConfig.getClientId());
        if(ccConfig.getClientSecret() != null) authServerConfig.setClientSecret(ccConfig.getClientSecret());
        if(ccConfig.getUri() != null) authServerConfig.setUri(ccConfig.getUri());
        return authServerConfig;
    }

    public static AuthServerConfig enrichAuthServerConfig(final AuthServerConfig baseConfig, final OAuthTokenConfig tokenConfig) {
        if(baseConfig.getProxyHost() == null && tokenConfig.getProxyHost() != null) baseConfig.setProxyHost(tokenConfig.getProxyHost());
        if(baseConfig.getProxyPort() == null && tokenConfig.getProxyPort() != null) baseConfig.setProxyPort(tokenConfig.getProxyPort());
        if(baseConfig.getServerUrl() == null && tokenConfig.getServerUrl() != null) baseConfig.setServerUrl(tokenConfig.getServerUrl());
        if(baseConfig.isEnableHttp2() == null && tokenConfig.isEnableHttp2() != null) baseConfig.setEnableHttp2(tokenConfig.isEnableHttp2());
        if(tokenConfig.getTokenRenewBeforeExpired() != null) baseConfig.setTokenRenewBeforeExpired(tokenConfig.getTokenRenewBeforeExpired());
        if(tokenConfig.getEarlyRefreshRetryDelay() != null) baseConfig.setEarlyRefreshRetryDelay(tokenConfig.getEarlyRefreshRetryDelay());
        if(tokenConfig.getExpiredRefreshRetryDelay() != null) baseConfig.setExpiredRefreshRetryDelay(tokenConfig.getExpiredRefreshRetryDelay());
        return baseConfig;
    }
    public static Result<Jwt> getJwtToken(final String serviceId) {
        ClientConfig clientConfig = ClientConfig.get();
        OAuthTokenConfig tokenConfig = clientConfig.getOAuth().getToken();
        OAuthTokenClientCredentialConfig ccConfig = tokenConfig.getClientCredentials();
        Result<Jwt> result;
        Jwt cachedJwt = cache.get(serviceId);
        // get a new token if cachedJwt is null or the jwt is about expired.
        if (cachedJwt == null || cachedJwt.getExpire() - (long) tokenConfig.getTokenRenewBeforeExpired() < System.currentTimeMillis()) {
            Jwt.Key key = new Jwt.Key(serviceId);
            cachedJwt = new Jwt(key); // create a new instance if the cache is empty for the serviceId.

            final var mapper = Config.getInstance().getMapper();
            if (clientConfig.getOAuth().isMultipleAuthServers()) {

                // get the right client credentials configuration based on the serviceId
                Map<String, AuthServerConfig> serviceIdAuthServers = ccConfig.getServiceIdAuthServers();
                if (serviceIdAuthServers == null) {
                    throw new RuntimeException("serviceIdAuthServers property is missing in the token client credentials configuration");
                }
                AuthServerConfig authServerConfig = serviceIdAuthServers.get(serviceId);
                cachedJwt.setAuthServerConfig(enrichAuthServerConfig(authServerConfig, tokenConfig));
            } else {
                cachedJwt.setAuthServerConfig(buildAuthServerConfig(tokenConfig, ccConfig));
            }
            result = OauthHelper.populateCCToken(cachedJwt);
            if (result.isSuccess()) {
                // put the cachedJwt to the cache.
                cache.put(serviceId, cachedJwt);
            }
        } else {
            // the cached jwt is not null and still valid.
            result = Success.of(cachedJwt);
        }
        return result;
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
        return TokenConfig.load().isEnabled();
    }

}
