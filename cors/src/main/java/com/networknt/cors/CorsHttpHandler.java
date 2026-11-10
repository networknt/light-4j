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

import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.status.Status;
import com.networknt.utility.Constants;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.util.*;

import java.util.*;

import static com.networknt.cors.CorsHeaders.*;
import static com.networknt.cors.CorsUtil.defaultOrigin;
import static io.undertow.server.handlers.ResponseCodeHandler.HANDLE_200;

/**
 * Undertow handler for CORS headers.
 * @see "http://www.w3.org/TR/cors"
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class CorsHttpHandler implements MiddlewareHandler {
    public static final String CORS_PREFLIGHT_REQUEST_FAILED =  "ERR10092";
    private String configName = CorsConfig.CONFIG_NAME;
    private volatile HttpHandler next;
    private volatile LegacyPolicy legacyPolicy;

    private record LegacyPolicy(List<?> origins, CorsOriginPolicy policy) { }
    /** Default max age **/
    private static final long ONE_HOUR_IN_SECONDS = 60 * 60;

    public CorsHttpHandler() {
        CorsConfig.load(configName);
        if(logger.isInfoEnabled()) logger.info("CorsHttpHandler is loaded.");
    }

    public CorsHttpHandler(String configName) {
        this.configName = configName;
        CorsConfig.load(configName);
        if(logger.isInfoEnabled()) logger.info("CorsHttpHandler is loaded with {}.", configName);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        CorsConfig config = CorsConfig.load(configName);
        if (!config.isEnabled()) {
            Handler.next(exchange, next);
            return;
        }
        // Merge after the proxy/downstream handler has supplied its response headers.
        exchange.addResponseCommitListener(CorsHttpHandler::addVaryOrigin);
        if (isCorsRequest(exchange.getRequestHeaders())) {
            CorsConfig.Policy policy = config.policyForPath(exchange.getRequestPath());
            String origin = matchOrigin(exchange, policy.origins());
            if (origin == null) {
                rejectOrigin(exchange);
                return;
            }
            setCorsResponseHeaders(exchange, origin, policy.methods());
            if (isPreflightedRequest(exchange)) {
                HANDLE_200.handleRequest(exchange);
                return;
            }
        }
        Handler.next(exchange, next);
    }

    private static void addVaryOrigin(HttpServerExchange exchange) {
        HeaderValues vary = exchange.getResponseHeaders().get(Headers.VARY);
        if (vary != null) {
            for (String value : vary) {
                for (String token : value.split(",")) {
                    if (token.trim().equals("*") || token.trim().equalsIgnoreCase("Origin")) return;
                }
            }
        }
        exchange.getResponseHeaders().add(Headers.VARY, Headers.ORIGIN_STRING);
    }

    private void setCorsResponseHeaders(HttpServerExchange exchange, String matchingOrigin,
                                        List<String> allowedMethods) {
        HeaderMap headers = exchange.getRequestHeaders();
        exchange.getResponseHeaders().put(new HttpString(ACCESS_CONTROL_ALLOW_ORIGIN), matchingOrigin);
        exchange.getResponseHeaders().addAll(new HttpString(ACCESS_CONTROL_ALLOW_METHODS), allowedMethods);
        HeaderValues requestedHeaders = headers.get(ACCESS_CONTROL_REQUEST_HEADERS);
        if (requestedHeaders != null && !requestedHeaders.isEmpty()) {
            exchange.getResponseHeaders().addAll(new HttpString(ACCESS_CONTROL_ALLOW_HEADERS), requestedHeaders);
        } else {
            exchange.getResponseHeaders().add(new HttpString(ACCESS_CONTROL_ALLOW_HEADERS), Headers.CONTENT_TYPE_STRING);
            exchange.getResponseHeaders().add(new HttpString(ACCESS_CONTROL_ALLOW_HEADERS), Headers.WWW_AUTHENTICATE_STRING);
            exchange.getResponseHeaders().add(new HttpString(ACCESS_CONTROL_ALLOW_HEADERS), Headers.AUTHORIZATION_STRING);
        }
        exchange.getResponseHeaders().put(new HttpString(ACCESS_CONTROL_ALLOW_CREDENTIALS), "true");
        exchange.getResponseHeaders().put(new HttpString(ACCESS_CONTROL_MAX_AGE), ONE_HOUR_IN_SECONDS);
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
        return CorsConfig.load(configName).isEnabled();
    }

    /**
     * Match the Origin header with the allowed origins.
     * If it doesn't match then a 403 response code is set on the response and it returns null.
     * @param exchange the current HttpExchange.
     * @param allowedOrigins list of sanitized allowed origins.
     * @param handler retained for source compatibility; request decisions are exchange-local.
     * @return the single validated request origin, null otherwise.
     * @throws Exception the checked exception
     */
    public static String matchOrigin(HttpServerExchange exchange, Collection<String> allowedOrigins, CorsHttpHandler handler) throws Exception {
        CorsOriginPolicy policy = handler == null
                ? CorsOriginPolicy.compileLegacy(allowedOrigins == null ? List.of() : allowedOrigins)
                : handler.policyForOrigins(allowedOrigins);
        String origin = matchOrigin(exchange, policy);
        if (origin == null) rejectOrigin(exchange);
        return origin;
    }

    private CorsOriginPolicy policyForOrigins(Collection<?> allowedOrigins) {
        List<?> snapshot = allowedOrigins == null ? List.of() : new ArrayList<>(allowedOrigins);
        LegacyPolicy cached = legacyPolicy;
        if (cached != null && cached.origins.equals(snapshot)) return cached.policy;
        synchronized (this) {
            cached = legacyPolicy;
            if (cached == null || !cached.origins.equals(snapshot)) {
                cached = new LegacyPolicy(Collections.unmodifiableList(snapshot), CorsOriginPolicy.compileLegacy(snapshot));
                legacyPolicy = cached;
            }
            return cached.policy;
        }
    }

    private static String matchOrigin(HttpServerExchange exchange, CorsOriginPolicy policy) {
        HeaderValues origins = exchange.getRequestHeaders().get(Headers.ORIGIN);
        if (origins == null || origins.size() != 1) return null;
        String origin = origins.getFirst();
        return policy.matches(origin, () -> defaultOrigin(exchange.getRequestScheme(),
                NetworkUtils.formatPossibleIpv6Address(exchange.getHostName()), exchange.getHostPort())) ? origin : null;
    }

    private static void rejectOrigin(HttpServerExchange exchange) throws Exception {
        logger.debug("Request rejected due to invalid or untrusted Origin.");
        if (AUDIT_ON_ERROR) {
            Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
            if (auditInfo == null) {
                auditInfo = new HashMap<>();
                exchange.putAttachment(AttachmentConstants.AUDIT_INFO, auditInfo);
            }
            auditInfo.put(Constants.STATUS, new Status(CORS_PREFLIGHT_REQUEST_FAILED));
        }
        ResponseCodeHandler.HANDLE_403.handleRequest(exchange);
    }

    public static boolean isCorsRequest(HeaderMap headers) {
        return headers.contains(ORIGIN);
    }

    public static boolean isPreflightedRequest(HttpServerExchange exchange) {
        return Methods.OPTIONS.equals(exchange.getRequestMethod());
    }

}
