/*
 * Copyright (C) 2015 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.networknt.cors;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.networknt.cors.CorsHeaders.*;
import static com.networknt.cors.CorsUtil.isPreflightedRequest;
import static com.networknt.cors.CorsUtil.matchOrigin;
import static io.undertow.server.handlers.ResponseCodeHandler.HANDLE_200;

/**
 * Undertow handler for CORS headers.
 * @see "http://www.w3.org/TR/cors"
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class CorsHttpHandler implements MiddlewareHandler {

    public static CorsConfig config;
    private List<String> allowedOrigins;
    private List<String> allowedMethods;

    private volatile HttpHandler next;
    /** Default max age **/
    private static final long ONE_HOUR_IN_SECONDS = 60 * 60;

    public CorsHttpHandler() {
        config = CorsConfig.load();
        allowedOrigins = config.getAllowedOrigins();
        allowedMethods = config.getAllowedMethods();
        if(logger.isInfoEnabled()) logger.info("CorsHttpHandler is loaded.");
    }

    /**
     * Please don't use this constructor. It is used by test case only to inject config object.
     * @param configName config name
     */
    @Deprecated
    public CorsHttpHandler(String configName) {
        config = CorsConfig.load(configName);
        allowedOrigins = config.getAllowedOrigins();
        allowedMethods = config.getAllowedMethods();
        if(logger.isInfoEnabled()) logger.info("CorsHttpHandler is loaded.");
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("CorsHttpHandler.handleRequest starts.");
        HeaderMap headers = exchange.getRequestHeaders();
        if (CorsUtil.isCoreRequest(headers)) {
            // cors headers available in the request. Set the allowedOrigins and allowedMethods based on the
            // path prefix if it is configured. Otherwise, use the global configuration set in the constructor.
            if (config.getPathPrefixAllowed() != null) {
                String requestPath = exchange.getRequestPath();
                for(Map.Entry<String, Object> entry: config.getPathPrefixAllowed().entrySet()) {
                    if (requestPath.startsWith(entry.getKey())) {
                        Map endpointCorsMap = (Map) entry.getValue();
                        allowedOrigins = (List<String>) endpointCorsMap.get(CorsConfig.ALLOWED_ORIGINS);
                        allowedMethods = (List<String>) endpointCorsMap.get(CorsConfig.ALLOWED_METHODS);
                        break;
                    }
                }
            }
            if (isPreflightedRequest(exchange)) {
                // it is a preflight request.
                handlePreflightRequest(exchange, allowedOrigins, allowedMethods);
                return;
            }
            setCorsResponseHeaders(exchange, allowedOrigins, allowedMethods);
        }
        if(logger.isDebugEnabled()) logger.debug("CorsHttpHandler.handleRequest ends.");
        Handler.next(exchange, next);
    }

    private void handlePreflightRequest(HttpServerExchange exchange, List<String> allowedOrigins, List<String> allowedMethods) throws Exception {
        setCorsResponseHeaders(exchange, allowedOrigins, allowedMethods);
        HANDLE_200.handleRequest(exchange);
    }

    private void setCorsResponseHeaders(HttpServerExchange exchange, List<String> allowedOrigins, List<String> allowedMethods) throws Exception {
        HeaderMap headers = exchange.getRequestHeaders();
        if (headers.contains(Headers.ORIGIN)) {
            if(matchOrigin(exchange, allowedOrigins) != null) {
                exchange.getResponseHeaders().addAll(ACCESS_CONTROL_ALLOW_ORIGIN, headers.get(Headers.ORIGIN));
                exchange.getResponseHeaders().add(Headers.VARY, Headers.ORIGIN_STRING);
            }
        }
        exchange.getResponseHeaders().addAll(ACCESS_CONTROL_ALLOW_METHODS, allowedMethods);
        HeaderValues requestedHeaders = headers.get(ACCESS_CONTROL_REQUEST_HEADERS);
        if (requestedHeaders != null && !requestedHeaders.isEmpty()) {
            exchange.getResponseHeaders().addAll(ACCESS_CONTROL_ALLOW_HEADERS, requestedHeaders);
        } else {
            exchange.getResponseHeaders().add(ACCESS_CONTROL_ALLOW_HEADERS, Headers.CONTENT_TYPE_STRING);
            exchange.getResponseHeaders().add(ACCESS_CONTROL_ALLOW_HEADERS, Headers.WWW_AUTHENTICATE_STRING);
            exchange.getResponseHeaders().add(ACCESS_CONTROL_ALLOW_HEADERS, Headers.AUTHORIZATION_STRING);
        }
        exchange.getResponseHeaders().add(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        exchange.getResponseHeaders().add(ACCESS_CONTROL_MAX_AGE, ONE_HOUR_IN_SECONDS);
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
        ModuleRegistry.registerModule(CorsConfig.CONFIG_NAME, CorsHttpHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(CorsConfig.CONFIG_NAME), null);
    }

    @Override
    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(CorsConfig.CONFIG_NAME, CorsHttpHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(CorsConfig.CONFIG_NAME), null);
        if(logger.isInfoEnabled()) {
            logger.info("CorsHttpHandler is enabled.");
        }
    }
}
