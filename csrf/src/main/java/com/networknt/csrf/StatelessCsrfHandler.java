/*
 * Copyright (c) 2018 Network New Technologies Inc.
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

package com.networknt.csrf;

import com.networknt.audit.AuditHandler;
import com.networknt.config.Config;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.status.Status;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.Util;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.jose4j.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This is a handler that checks if CSRF token exists in the header for services exposed to Single Page
 * Application running on browsers. Normally, this handler only needs to be injected on the services in
 * the DMZ. For example aggregators or light-router to aggregate calls to multiple services or router
 * calls to multiple services from internal network.
 *
 * It compares the token from header to the token inside the JWT to ensure that it matches. If token does
 * not exist or is not matched, an error will be returned.
 *
 * This handler is a middleware handler and must be injected in service.yml if needed. As it is depending
 * on the JWT token claims, it must be injected after JWT token verifier.
 *
 * @author Steve Hu
 */
public class StatelessCsrfHandler implements MiddlewareHandler {
    private static final Logger logger = LoggerFactory.getLogger(StatelessCsrfHandler.class);
    private static final String CONFIG_NAME = "statelessCsrf";
    private static final String CSRF_HEADER_MISSING = "ERR10036";
    private static final String SUBJECT_JWT_CLAIMS_NOT_FOUND = "ERR10037";
    private static final String CSRF_TOKEN_MISSING_IN_JWT = "ERR10038";
    private static final String HEADER_CSRF_JWT_CSRF_NOT_MATCH = "ERR10039";


    public static StatelessCsrfConfig config = (StatelessCsrfConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, StatelessCsrfConfig.class);

    private volatile HttpHandler next;

    public StatelessCsrfHandler() {
        logger.info("StatelessCsrfHandler is constructed");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("StatelessCsrfHandler.handleRequest is called.");
        // get CSRF token from the header. Return error is it doesn't exist.
        String headerCsrf = exchange.getRequestHeaders().getFirst(Constants.CSRF_TOKEN);
        if(headerCsrf == null || headerCsrf.trim().length() == 0) {
            Status status = new Status(CSRF_HEADER_MISSING);
            exchange.setStatusCode(status.getStatusCode());
            exchange.getResponseSender().send(status.toString());
            logger.error("ValidationError:" + status.toString());
            return;
        }
        // now we have the csrf header, compare it with the one in JWT token.
        Map<String, Object> auditInfo = exchange.getAttachment(AuditHandler.AUDIT_INFO);
        JwtClaims claims = (JwtClaims)auditInfo.get(Constants.SUBJECT_CLAIMS);
        if(claims == null) {
            Status status = new Status(SUBJECT_JWT_CLAIMS_NOT_FOUND);
            exchange.setStatusCode(status.getStatusCode());
            exchange.getResponseSender().send(status.toString());
            logger.error("ValidationError:" + status.toString());
            return;
        }
        // get csrf from JwtClaims
        String jwtCsrf = claims.getStringClaimValue(Constants.CSRF_STRING);
        if(jwtCsrf == null || jwtCsrf.trim().length() == 0) {
            Status status = new Status(CSRF_TOKEN_MISSING_IN_JWT);
            exchange.setStatusCode(status.getStatusCode());
            exchange.getResponseSender().send(status.toString());
            logger.error("ValidationError:" + status.toString());
            return;
        }

        if(logger.isDebugEnabled()) logger.debug("headerCsrf = " + headerCsrf + " jwtCsrf = " + jwtCsrf);
        if(!headerCsrf.equals(jwtCsrf)) {
            Status status = new Status(HEADER_CSRF_JWT_CSRF_NOT_MATCH, headerCsrf, jwtCsrf);
            exchange.setStatusCode(status.getStatusCode());
            exchange.getResponseSender().send(status.toString());
            logger.error("ValidationError:" + status.toString());
            return;
        }
        next.handleRequest(exchange);
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
        ModuleRegistry.registerModule(StatelessCsrfHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }

}
