/*
 * Copyright (c) 2026 Network New Technologies Inc.
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

package com.networknt.token.exchange;

import com.networknt.client.Http2Client;
import com.networknt.client.simplepool.SimpleConnectionHolder;
import com.networknt.config.Config;
import com.networknt.status.Status;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Token Exchange Handler that exchanges an incoming JWT token for a new token
 * from an OAuth 2.0 provider using the Token Exchange grant type (RFC 8693).
 * <p>
 * This handler follows the Config Stateless pattern - it loads configuration
 * fresh from cache on each request, allowing dynamic config updates to take
 * effect without restart.
 *
 * @author Steve Hu
 */
public class TokenExchangeHandler implements MiddlewareHandler {
    private static final Logger logger = LoggerFactory.getLogger(TokenExchangeHandler.class);
    private static final String GRANT_TYPE_TOKEN_EXCHANGE = "urn:ietf:params:oauth:grant-type:token-exchange";
    private static final String STATUS_TOKEN_EXCHANGE_FAILED = "ERR10001";

    // Only store the config name, not the config itself (Config Stateless pattern)
    private String configName = TokenExchangeConfig.CONFIG_NAME;

    private volatile HttpHandler next;

    public TokenExchangeHandler() {
        // Load once during construction to ensure config is valid
        TokenExchangeConfig.load(configName);
        if (logger.isInfoEnabled()) logger.info("TokenExchangeHandler is loaded.");
    }

    /**
     * Alternative constructor for testing with different config files.
     * @param configName the name of the config file
     * @deprecated Use default constructor in production
     */
    @Deprecated
    public TokenExchangeHandler(String configName) {
        this.configName = configName;
        TokenExchangeConfig.load(configName);
        if (logger.isInfoEnabled()) logger.info("TokenExchangeHandler is loaded with {}.", configName);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (logger.isDebugEnabled()) logger.debug("TokenExchangeHandler.handleRequest starts.");

        // Load config from cache on each request - if config is reloaded,
        // the next request will automatically use the new config
        TokenExchangeConfig config = TokenExchangeConfig.load(configName);

        if (config.isEnabled()) {
            String token = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
            if (token != null && token.startsWith("Bearer ")) {
                String subjectToken = token.substring(7);
                String newToken = exchangeToken(subjectToken, config);

                if (newToken != null) {
                    exchange.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer " + newToken);
                } else {
                    logger.error("Token exchange failed for token: {}", 
                            subjectToken.length() > 4 ? "..." + subjectToken.substring(subjectToken.length() - 4) : subjectToken);
                    setExchangeStatus(exchange, new Status(STATUS_TOKEN_EXCHANGE_FAILED, "Token exchange failed"));
                    if (logger.isDebugEnabled()) logger.debug("TokenExchangeHandler.handleRequest ends with error.");
                    return;
                }
            }
        }

        if (logger.isDebugEnabled()) logger.debug("TokenExchangeHandler.handleRequest ends.");
        Handler.next(exchange, next);
    }

    /**
     * Exchange the subject token for a new token using the OAuth 2.0 Token Exchange grant type.
     *
     * @param subjectToken the subject token to exchange
     * @param config the configuration to use for the token exchange
     * @return the new access token, or null if the exchange failed
     */
    private String exchangeToken(String subjectToken, TokenExchangeConfig config) {
        SimpleConnectionHolder.ConnectionToken token = null;
        try {
            URI uri = new URI(config.getTokenExUri());
            Http2Client client = Http2Client.getInstance();
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<ClientResponse> reference = new AtomicReference<>();

            token = client.borrow(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
            ClientConnection connection = (ClientConnection) token.getRawConnection();

            ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(uri.getPath());
            request.getRequestHeaders().put(Headers.HOST, uri.getHost());
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded");

            // Build form data body
            Map<String, String> params = new HashMap<>();
            params.put("grant_type", GRANT_TYPE_TOKEN_EXCHANGE);
            params.put("subject_token", subjectToken);
            params.put("subject_token_type", config.getSubjectTokenType());
            params.put("requested_token_type", config.getRequestedTokenType());
            params.put("client_id", config.getTokenExClientId());
            params.put("client_secret", config.getTokenExClientSecret());
            if (config.getTokenExScope() != null && !config.getTokenExScope().isEmpty()) {
                params.put("scope", String.join(" ", config.getTokenExScope()));
            }

            String body = getFormDataString(params);
            request.getRequestHeaders().put(Headers.CONTENT_LENGTH, body.length());

            connection.sendRequest(request, client.createClientCallback(reference, latch, body));
            latch.await();

            ClientResponse response = reference.get();
            if (response != null && response.getResponseCode() == 200) {
                String responseBody = response.getAttachment(Http2Client.RESPONSE_BODY);
                @SuppressWarnings("unchecked")
                Map<String, Object> respMap = Config.getInstance().getMapper().readValue(responseBody, Map.class);
                return (String) respMap.get("access_token");
            } else {
                logger.error("Error response from token exchange: {}", response != null ? response.getResponseCode() : "null");
                if (response != null) {
                    logger.error("Response body: {}", response.getAttachment(Http2Client.RESPONSE_BODY));
                }
            }
        } catch (Exception e) {
            logger.error("Exception during token exchange", e);
        } finally {
            if (token != null) {
                Http2Client.getInstance().restore(token);
            }
        }
        return null;
    }

    /**
     * Convert a map of parameters to a URL-encoded form data string.
     *
     * @param params the parameters to encode
     * @return the URL-encoded form data string
     * @throws Exception if encoding fails
     */
    private String getFormDataString(Map<String, String> params) throws Exception {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first) {
                first = false;
            } else {
                result.append("&");
            }
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return result.toString();
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return TokenExchangeConfig.load(configName).isEnabled();
    }
}

