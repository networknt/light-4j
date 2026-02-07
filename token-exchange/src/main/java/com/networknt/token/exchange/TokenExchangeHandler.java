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

package com.networknt.token.exchange;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.handler.LightHttpHandler;
import com.networknt.monad.Result;
import com.networknt.status.Status;
import com.networknt.utility.StringUtils;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.server.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class TokenExchangeHandler implements MiddlewareHandler {
    private static final Logger logger = LoggerFactory.getLogger(TokenExchangeHandler.class);
    private static final String GRANT_TYPE_TOKEN_EXCHANGE = "urn:ietf:params:oauth:grant-type:token-exchange";
    private static final String STATUS_TOKEN_EXCHANGE_FAILED = "ERR10001"; // Placeholder error code

    private static TokenExchangeConfig config;
    private volatile HttpHandler next;

    public TokenExchangeHandler() {
        config = TokenExchangeConfig.load();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (config.isEnabled()) {
            String token = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
            if (token != null && token.startsWith("Bearer ")) {
                String subjectToken = token.substring(7);
                String newToken = exchangeToken(subjectToken);
                
                if (newToken != null) {
                    exchange.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer " + newToken);
                } else {
                    // Start of error handling
                    logger.error("Token exchange failed for token: " + (subjectToken.length() > 4 ? subjectToken.substring(subjectToken.length() - 4) : subjectToken));
                    setExchangeStatus(exchange, new Status(STATUS_TOKEN_EXCHANGE_FAILED, "Token exchange failed"));
                    return;
                    // End of error handling
                }
            }
        }
        
        Handler.next(exchange, next);
    }

    private String exchangeToken(String subjectToken) {
        // This is a synchronous implementation for simplicity.
        // In a real high-throughput scenario, async or caching is recommended.
        
        try {
            URI uri = new URI(config.getTokenExUri());
            Http2Client client = Http2Client.getInstance();
            // We use a latch to wait for the async client response
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<ClientResponse> reference = new AtomicReference<>();
            
            ClientConnection connection = client.borrowConnection(5000, uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
            try {
                ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath(uri.getPath());
                request.getRequestHeaders().put(Headers.HOST, "localhost");
                request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/x-www-form-urlencoded");
                
                // Build body
                Map<String, String> params = new HashMap<>();
                params.put("grant_type", GRANT_TYPE_TOKEN_EXCHANGE);
                params.put("subject_token", subjectToken);
                params.put("subject_token_type", config.getSubjectTokenType());
                params.put("requested_token_type", config.getRequestedTokenType());
                params.put("client_id", config.getTokenExClientId());
                params.put("client_secret", config.getTokenExClientSecret());
                if(config.getTokenExScope() != null && !config.getTokenExScope().isEmpty()) {
                    params.put("scope", String.join(" ", config.getTokenExScope()));
                }

                String body = getFormDataString(params);
                
                request.getRequestHeaders().put(Headers.CONTENT_LENGTH, body.length());
                
                connection.sendRequest(request, client.createClientCallback(reference, latch, body));
                latch.await();
                
                ClientResponse response = reference.get();
                if (response != null && response.getResponseCode() == 200) {
                    String responseBody = response.getAttachment(Http2Client.RESPONSE_BODY);
                    Map<String, Object> respMap = Config.getInstance().getMapper().readValue(responseBody, Map.class);
                    return (String) respMap.get("access_token");
                } else {
                    logger.error("Error response from token exchange: " + (response != null ? response.getResponseCode() : "null"));
                    if(response != null) {
                        logger.error("Response body: " + response.getAttachment(Http2Client.RESPONSE_BODY));
                    }
                }
            } finally {
                client.returnConnection(connection);
            }
        } catch (Exception e) {
            logger.error("Exception during token exchange", e);
        }
        return null;
    }
    
    private String getFormDataString(Map<String, String> params) throws Exception {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");
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

    static {
        ModuleRegistry.registerModule(TokenExchangeConfig.CONFIG_NAME, TokenExchangeHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(TokenExchangeConfig.CONFIG_NAME), null);
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }
}
