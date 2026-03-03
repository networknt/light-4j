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

package com.networknt.token.exchange.handler;

import com.networknt.status.Status;
import com.networknt.token.exchange.RequestContext;
import com.networknt.token.exchange.TokenExchangeConfig;
import com.networknt.token.exchange.TokenExchangeService;
import com.networknt.token.exchange.extract.AuthType;
import com.networknt.token.exchange.extract.ClientIdentity;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Token Exchange Handler that exchanges an incoming JWT token for a new token
 * from an OAuth 2.0 provider using the Token Exchange grant type (RFC 8693).
 * <p>
 * This handler uses the {@link TokenExchangeService} to perform token
 * transformation based on the configured token schemas and client mappings.
 * <p>
 * Configuration is loaded from token-transformer.yml which supports:
 * <ul>
 *   <li>Multiple token schemas for different clients/scenarios</li>
 *   <li>Client ID to schema mappings for automatic schema selection</li>
 *   <li>Path-based authentication type resolution</li>
 *   <li>Token caching with configurable expiration</li>
 * </ul>
 *
 * @author Steve Hu
 */
public class TokenExchangeHandler implements MiddlewareHandler {
    private static final Logger logger = LoggerFactory.getLogger(TokenExchangeHandler.class);
    private static final String STATUS_TOKEN_EXCHANGE_FAILED = "ERR10001";

    // Only store the config name, not the config itself (Config Stateless pattern)
    private final String configName = TokenExchangeConfig.CONFIG_NAME;

    private volatile HttpHandler next;
    private final TokenExchangeService tokenService;

    public TokenExchangeHandler() {
        // Load once during construction to ensure config is valid
        TokenExchangeConfig.load(configName);
        this.tokenService = new TokenExchangeService();
        logger.info("TokenExchangeHandler is loaded.");
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        // Load config from cache on each request - if config is reloaded,
        // the next request will automatically use the new config
        TokenExchangeConfig config = TokenExchangeConfig.load(configName);

        if (config.isEnabled()) {
            String authHeader = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
            if (authHeader != null && !authHeader.isEmpty()) {
                try {
                    // Build client-ID based request context parser
                    RequestContext.Parser requestParser = new ClientIdBasedRequestParser(exchange, config);

                    // Use the TokenExchangeService to transform the token
                    this.tokenService.transform(requestParser);

                    logger.debug("Token exchange successful.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Token exchange was interrupted", e);
                    setExchangeStatus(exchange, new Status(STATUS_TOKEN_EXCHANGE_FAILED, "Token exchange was interrupted"));
                    return;
                } catch (Exception e) {
                    logger.error("Token exchange failed", e);
                    setExchangeStatus(exchange, new Status(STATUS_TOKEN_EXCHANGE_FAILED, "Token exchange failed: " + e.getMessage()));
                    return;
                }
            }
        }

        Handler.next(exchange, next);
    }

    /**
     * Client-ID based request parser.
     * Resolves schema by extracting client identity from Authorization header.
     */
    private static class ClientIdBasedRequestParser implements RequestContext.Parser {
        private final HttpServerExchange exchange;
        private final TokenExchangeConfig config;

        private ClientIdBasedRequestParser(final HttpServerExchange exchange, final TokenExchangeConfig config) {
            this.exchange = exchange;
            this.config = config;
        }

        @Override
        public RequestContext parseContext() {
            // Build context from exchange headers and path
            Map<String, String> headers = new HashMap<>();
            exchange.getRequestHeaders().forEach(header ->
                headers.put(header.getHeaderName().toString().toLowerCase(),
                           exchange.getRequestHeaders().getFirst(header.getHeaderName()))
            );

            String path = exchange.getRequestPath();

            // Resolve auth type from path
            AuthType authType = config.resolveAuthTypeFromPath(path);
            if (authType == null) {
                logger.warn("No auth type resolved for path: {}", path);
                return null;
            }

            // Extract client identity
            String authHeader = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
            ClientIdentity identity = authType.extractor().extract(authHeader);
            if (identity == null) {
                logger.warn("Failed to extract client identity from Authorization header");
                return null;
            }

            // Resolve schema key from client ID
            String schemaKey = config.resolveSchemaFromClientId(identity.id());
            if (schemaKey == null) {
                logger.warn("No schema mapping found for client ID: {}", identity.id());
                return null;
            }

            return new RequestContext(schemaKey, headers, Map.of(), path);
        }

        @Override
        public void updateRequest(Map<String, Object> resultMap) {
            // Apply transformed headers to the exchange
            Object requestHeaders = resultMap.get(TokenExchangeService.REQUEST_HEADERS);
            if (requestHeaders instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> headersMap = (Map<String, Object>) requestHeaders;
                Object update = headersMap.get(TokenExchangeService.UPDATE);
                if (update instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> updateMap = (Map<String, String>) update;
                    for (Map.Entry<String, String> entry : updateMap.entrySet()) {
                        exchange.getRequestHeaders().put(
                                io.undertow.util.HttpString.tryFromString(entry.getKey()),
                                entry.getValue());
                        logger.trace("Updated header '{}' with new value", entry.getKey());
                    }
                }
            }
        }
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
