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

import com.networknt.client.oauth.OauthHelper;
import com.networknt.client.oauth.SAMLBearerRequest;
import com.networknt.client.oauth.TokenResponse;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
import com.networknt.status.Status;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This is a middleware handler that is responsible for getting a JWT access token from
 * OAuth 2.0 provider for the particular router client. In this use case it is assumed the client
 * has a signed saml and a signed JWT token in the incoming HTTP headers. These two tokens will be
 * passed to the authorization server to get the JWT access token.
 * This handler will also be responsible for checking if the cached token is about to expired
 * or not. In which case, it will renew the token in another thread. When request comes and
 * the cached token is already expired, then it will block the request and go to the OAuth
 * provider to get a new token and then resume the request to the next handler in the chain.
 *
 * This handler is very similar to the TokenHandler except it doesn't use the client credential
 * grant type.
 *
 */
public class SAMLTokenHandler implements MiddlewareHandler {
    public static final String CLIENT_CONFIG_NAME = "client";
    public static final String CONFIG_SECURITY = "security";
    static final TokenConfig config = TokenConfig.load();
    static Logger logger = LoggerFactory.getLogger(SAMLTokenHandler.class);
    protected volatile HttpHandler next;

    static final String OAUTH = "oauth";
    static final String TOKEN = "token";
    static final String OAUTH_HTTP2_SUPPORT = "oauthHttp2Support";

    static final String SAMLAssertionHeader = "assertion";
    static final String JWTAssertionHeader = "client_assertion";

    static Map<String, Object> clientConfig;
    static Map<String, Object> tokenConfig;
    static boolean oauthHttp2Support;

    private final Object lock = new Object();

    public SAMLTokenHandler() {
        clientConfig = Config.getInstance().getJsonMapConfig(CLIENT_CONFIG_NAME);
        if(clientConfig != null) {
            Map<String, Object> oauthConfig = (Map<String, Object>)clientConfig.get(OAUTH);
            if(oauthConfig != null) {
                tokenConfig = (Map<String, Object>)oauthConfig.get(TOKEN);
            }
        }
        Map<String, Object> securityConfig = Config.getInstance().getJsonMapConfig(CONFIG_SECURITY);
        if(securityConfig != null) {
            Boolean b = (Boolean)securityConfig.get(OAUTH_HTTP2_SUPPORT);
            oauthHttp2Support = (b == null ? false : b.booleanValue());
        }
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        // check if there is a bear token in the authorization header in the request. If this
        // is one, then this must be the subject token that is linked to the original user.
        // We will keep this token in the Authorization header but create a new token with
        // client credentials grant type with scopes for the particular client. (Can we just
        // assume that the subject token has the scope already?)
        logger.debug(exchange.toString());
        Result<String> result = getSAMLBearerToken(exchange.getRequestHeaders().getFirst(SAMLAssertionHeader), exchange.getRequestHeaders().getFirst(JWTAssertionHeader));
        if(result.isFailure()) {
            sendStatusToResponse(exchange, result.getError());
            return;
        }
        exchange.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer " + result.getResult());
        exchange.getRequestHeaders().remove(SAMLAssertionHeader);
        exchange.getRequestHeaders().remove(JWTAssertionHeader);
        Handler.next(exchange, next);
    }

    public static void sendStatusToResponse(HttpServerExchange exchange, Status status) {
        exchange.setStatusCode(status.getStatusCode());
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(status.toString());
        logger.error(status.toString());
        // in case to trace where the status is created, enable the trace level logging to diagnose.
        if (logger.isTraceEnabled()) {
            StackTraceElement[] elements = Thread.currentThread().getStackTrace();
            String stackTrace = Arrays.stream(elements)
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining("\n"));
            logger.trace(stackTrace);
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
        ModuleRegistry.registerModule(TokenConfig.CONFIG_NAME, SAMLTokenHandler.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(TokenConfig.CONFIG_NAME, SAMLTokenHandler.class.getName(), config.getMappedConfig(), null);
    }

    private Result<String> getSAMLBearerToken(String samlAssertion , String jwtAssertion) {
        SAMLBearerRequest tokenRequest = new SAMLBearerRequest(samlAssertion , jwtAssertion);
        Result<TokenResponse> tokenResponse = OauthHelper.getTokenFromSamlResult(tokenRequest);
        if(tokenResponse.isSuccess()) {
            String jwt = tokenResponse.getResult().getAccessToken();
            logger.debug("SAMLBearer Grant Type jwt: ", jwt);
            return Success.of(jwt);
        } else {
            return Failure.of(tokenResponse.getError());
        }
    }
}
