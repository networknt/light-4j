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

package com.networknt.basicauth;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.ldap.LdapUtil;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.StringUtils;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This is a middleware handler that handles basic authentication in restful APIs. It is not used
 * in most situations as OAuth 2.0 is the standard. In certain cases for example, the server is
 * deployed to IoT devices, basic authentication can be used to replace OAuth 2.0 handlers.
 * <p>
 * There are multiple users that can be defined in basic.yml config file. Password can be stored in plain or
 * encrypted format in basic.yml. In case of password encryption, please remember to add corresponding
 * com.networknt.utility.Decryptor in service.yml. And access is logged into audit.log if audit middleware is used.
 *
 * @author Steve Hu
 */
public class BasicAuthHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(BasicAuthHandler.class);
    static final String BEARER_PREFIX = "BEARER";
    static final String BASIC_PREFIX = "BASIC";
    static BasicAuthConfig config;

    static final String MISSING_AUTH_TOKEN = "ERR10002";
    static final String INVALID_BASIC_HEADER = "ERR10046";
    static final String INVALID_USERNAME_OR_PASSWORD = "ERR10047";
    static final String NOT_AUTHORIZED_REQUEST_PATH = "ERR10071";
    static final String INVALID_AUTHORIZATION_HEADER = "ERR12003";
    static final String BEARER_USER_NOT_FOUND = "ERR10072";

    private volatile HttpHandler next;

    public BasicAuthHandler() {
        config = BasicAuthConfig.load();
        if (logger.isInfoEnabled()) logger.info("BasicAuthHandler is loaded.");
    }

    /**
     * This is a constructor for test cases only. Please don't use it.
     *
     * @param cfg BasicAuthConfig
     */
    @Deprecated
    public BasicAuthHandler(BasicAuthConfig cfg) {
        config = cfg;
        if (logger.isInfoEnabled()) logger.info("BasicAuthHandler is loaded.");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("BasicAuthHandler.handleRequest starts.");
        String auth = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
        String requestPath = exchange.getRequestPath();

        /* no auth header */
        if (auth == null || auth.trim().length() == 0) {
            this.handleAnonymousAuth(exchange, requestPath);

        /* contains auth header */
        } else {
            // verify the header with the config file. assuming it is basic authentication first.
            if (BASIC_PREFIX.equalsIgnoreCase(auth.substring(0, 5))) {
                // check if the length is greater than 6 for issue1513
                if(auth.trim().length() == 5) {
                    logger.error("Invalid/Unsupported authorization header {}", auth);
                    setExchangeStatus(exchange, INVALID_AUTHORIZATION_HEADER, auth);
                    exchange.endExchange();
                    return;
                } else {
                    this.handleBasicAuth(exchange, requestPath, auth);
                }
            } else if (BEARER_PREFIX.equalsIgnoreCase(auth.substring(0, 6))) {
                this.handleBearerToken(exchange, requestPath, auth);
            } else {
                logger.error("Invalid/Unsupported authorization header {}", auth.substring(0, 10));
                setExchangeStatus(exchange, INVALID_AUTHORIZATION_HEADER, auth.substring(0, 10));
                exchange.endExchange();
                return;
            }
        }
        if(logger.isDebugEnabled()) logger.debug("BasicAuthHandler.handleRequest ends.");
        Handler.next(exchange, next);

    }

    /**
     * Handle anonymous authentication.
     * If requests are anonymous and do not have a path entry, we block the request.
     *
     * @param exchange - current exchange.
     * @param requestPath - path for current request.
     */
    private void handleAnonymousAuth(HttpServerExchange exchange, String requestPath) {
        if (config.isAllowAnonymous() && config.getUsers().containsKey(BasicAuthConfig.ANONYMOUS)) {
            List<String> paths = config.getUsers().get(BasicAuthConfig.ANONYMOUS).getPaths();
            boolean match = false;
            for (String path : paths) {
                if (requestPath.startsWith(path)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                logger.error("Request path '{}' is not authorized for user '{}'", requestPath, BasicAuthConfig.ANONYMOUS);
                setExchangeStatus(exchange, NOT_AUTHORIZED_REQUEST_PATH, requestPath, BasicAuthConfig.ANONYMOUS);
                if(logger.isDebugEnabled())
                    logger.debug("BasicAuthHandler.handleRequest ends with an error.");
                exchange.endExchange();
            }
        } else {
            logger.error("Anonymous is not allowed and authorization header is missing.");
            setExchangeStatus(exchange, MISSING_AUTH_TOKEN);
            if(logger.isDebugEnabled())
                logger.debug("BasicAuthHandler.handleRequest ends with an error.");
            exchange.endExchange();
        }
    }

    /**
     * Handle basic authentication header.
     * If the request coming in has an incorrect format for basic auth, we block the request.
     * We also block the request if the path is not configured to have basic authentication.
     *
     * @param exchange - current exchange.
     * @param requestPath - path found within current request.
     * @param auth - auth string
     */
    public void handleBasicAuth(HttpServerExchange exchange, String requestPath, String auth) {
        String credentials = auth.substring(6);
        int pos = credentials.indexOf(':');
        if (pos == -1) {
            credentials = new String(org.apache.commons.codec.binary.Base64.decodeBase64(credentials), UTF_8);
        }

        pos = credentials.indexOf(':');
        if (pos != -1) {
            String username = credentials.substring(0, pos);
            String password = credentials.substring(pos + 1);
            UserAuth user = config.getUsers().get(username);
            // if user name matches config, no password entry in config and enableAD is true, use LDAP auth
            if (user != null
                    && username.equals(user.getUsername())
                    && StringUtils.isEmpty(user.getPassword())
                    && config.enableAD) {
                // Call LdapUtil with LDAP authentication and authorization
                if (!handleLdapAuth(user, password)) {
                    setExchangeStatus(exchange, INVALID_USERNAME_OR_PASSWORD);
                    exchange.endExchange();
                    if(logger.isDebugEnabled())
                        logger.debug("BasicAuthHandler.handleRequest ends with an error.");
                    return;
                }
            } else {
                //
                if (user == null || !(user.getUsername().equals(username)
                        && password.equals(user.getPassword()))) {
                    logger.error("Invalid username or password with authorization header starts = {}", auth.substring(0, 10));
                    setExchangeStatus(exchange, INVALID_USERNAME_OR_PASSWORD);
                    exchange.endExchange();
                    if (logger.isDebugEnabled())
                        logger.debug("BasicAuthHandler.handleRequest ends with an error.");
                    return;
                }
            }
            // Here we have passed the authentication. Let's do the authorization with the paths.
            boolean match = false;
            for (String path : user.getPaths()) {
                if (requestPath.startsWith(path)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                logger.error("Request path '{}' is not authorized for user '{}", requestPath, user.getUsername());
                setExchangeStatus(exchange, NOT_AUTHORIZED_REQUEST_PATH, requestPath, user.getUsername());
                if(logger.isDebugEnabled())
                    logger.debug("BasicAuthHandler.handleRequest ends with an error.");
                exchange.endExchange();
            }
        } else {
            logger.error("Invalid basic authentication header. It must be username:password base64 encode.");
            setExchangeStatus(exchange, INVALID_BASIC_HEADER, auth.substring(0, 10));
            if(logger.isDebugEnabled())
                logger.debug("BasicAuthHandler.handleRequest ends with an error.");
            exchange.endExchange();
        }
    }

    /**
     * Handle LDAP authentication and authorization
     * @param user
     * @return true if Ldap auth success, false if Ldap auth failure
     */
    private static boolean handleLdapAuth(UserAuth user, String password) {
        boolean isAuthenticated = LdapUtil.authenticate(user.getUsername(), password);
        if (!isAuthenticated) {
            logger.error("user '" + user.getUsername() + "' Ldap authentication failed");
            return false;
        }
        return true;
    }

    /**
     * Handle Bearer token authentication.
     * We block requests that are not configured to have bearer tokens.
     * We also block requests that are configured to have a bearer token
     *
     * @param exchange - current exchange.
     * @param requestPath - path for request
     * @param auth - auth string
     */
    private void handleBearerToken(HttpServerExchange exchange, String requestPath, String auth) {
        // not basic token. check if the OAuth 2.0 bearer token is allowed.
        if (!this.config.allowBearerToken) {
            logger.error("Not a basic authentication header, and bearer token is not allowed.");
            setExchangeStatus(exchange, INVALID_BASIC_HEADER, auth.substring(0, 10));
            if(logger.isDebugEnabled())
                logger.debug("BasicAuthHandler.handleRequest ends with an error.");
            exchange.endExchange();
        } else {
            // bearer token is allowed, we need to validate it and check the allowed paths.
            UserAuth user = this.config.getUsers().get(BasicAuthConfig.BEARER);
            if (user != null) {
                // check the path for authorization
                List<String> paths = user.getPaths();
                boolean match = false;
                for (String path : paths) {
                    if (requestPath.startsWith(path)) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    logger.error("Request path '{}' is not authorized for user '{}' ", requestPath, BasicAuthConfig.BEARER);
                    setExchangeStatus(exchange, NOT_AUTHORIZED_REQUEST_PATH, requestPath, BasicAuthConfig.BEARER);
                    if(logger.isDebugEnabled())
                        logger.debug("BasicAuthHandler.handleRequest ends with an error.");
                    exchange.endExchange();
                }
            } else {
                logger.error("Bearer token is allowed but missing the bearer user path definitions for authorization");
                setExchangeStatus(exchange, BEARER_USER_NOT_FOUND);
                if(logger.isDebugEnabled())
                    logger.debug("BasicAuthHandler.handleRequest ends with an error.");
                exchange.endExchange();
            }
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
        // As passwords are in the config file, we need to mask them.
        List<String> masks = new ArrayList<>();
        masks.add("password");
        ModuleRegistry.registerModule(BasicAuthHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(BasicAuthConfig.CONFIG_NAME), masks);
    }

    @Override
    public void reload() {
        config = BasicAuthConfig.load();
    }
}
