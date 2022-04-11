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
import com.networknt.utility.ModuleRegistry;
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
 *
 * There are multiple users that can be defined in basic.yml config file. Password can be stored in plain or
 * encrypted format in basic.yml. In case of password encryption, please remember to add corresponding
 * com.networknt.utility.Decryptor in service.yml. And access is logged into audit.log if audit middleware is used.
 *
 * @author Steve Hu
 */
public class BasicAuthHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(BasicAuthHandler.class);
    static BasicAuthConfig config = BasicAuthConfig.load();

    static final String MISSING_AUTH_TOKEN = "ERR10002";
    static final String INVALID_BASIC_HEADER = "ERR10046";
    static final String INVALID_USERNAME_OR_PASSWORD = "ERR10047";
    static final String NOT_AUTHORIZED_REQUEST_PATH = "ERR10071";

    private volatile HttpHandler next;

    public BasicAuthHandler() {
        if(logger.isInfoEnabled()) logger.info("BasicAuthHandler is loaded.");
    }

    /**
     * This is a constructor for test cases only. Please don't use it.
     * @param cfg BasicAuthConfig
     */
    @Deprecated
    public BasicAuthHandler(BasicAuthConfig cfg) {
        config = cfg;
        if(logger.isInfoEnabled()) logger.info("BasicAuthHandler is loaded.");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        String auth = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
        String requestPath = exchange.getRequestPath();
        if(auth == null || auth.trim().length() == 0) {
            if(config.isAllowAnonymous() && config.getUsers().containsKey(BasicAuthConfig.ANONYMOUS)) {
                List<String> paths = config.getUsers().get(BasicAuthConfig.ANONYMOUS).getPaths();
                boolean match = false;
                for(String path: paths) {
                    if(requestPath.startsWith(path)) {
                        match = true;
                        break;
                    }
                }
                if(!match) {
                    logger.error("Request path" + requestPath + " is not authorized for user " + BasicAuthConfig.ANONYMOUS);
                    setExchangeStatus(exchange, NOT_AUTHORIZED_REQUEST_PATH, requestPath, BasicAuthConfig.ANONYMOUS);
                    return;
                }
            } else {
                logger.error("Anonymous is not allowed and authorization header is missing.");
                setExchangeStatus(exchange, MISSING_AUTH_TOKEN);
                return;
            }
        } else {
            // verify the header with the config file. assuming it is basic authentication first.
            String basic = auth.substring(0, 5);
            if("BASIC".equalsIgnoreCase(basic)) {
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
                    if(user == null || !(user != null && user.getUsername().equals(username) && user.getPassword().equals(password))) {
                        logger.error("Invalid username or password with authorization header starts = " + auth.substring(0, 10));
                        setExchangeStatus(exchange, INVALID_USERNAME_OR_PASSWORD);
                        return;
                    }
                    // Here we have passed the authentication. Let's do the authorization with the paths.
                    boolean match = false;
                    for(String path: user.getPaths()) {
                        if(requestPath.startsWith(path)) {
                            match = true;
                            break;
                        }
                    }
                    if(!match) {
                        logger.error("Request path" + requestPath + " is not authorized for user " + user.getUsername());
                        setExchangeStatus(exchange, NOT_AUTHORIZED_REQUEST_PATH, requestPath, user.getUsername());
                        return;
                    }
                } else {
                    logger.error("Invalid basic authentication header. It must be username:password base64 encode.");
                    setExchangeStatus(exchange, INVALID_BASIC_HEADER, auth.substring(0, 10));
                    return;
                }
            } else {
                // not basic token.
                if(!config.allowOtherAuth) {
                    logger.error("Not a basic authentication header and other authentication header is not allowed.");
                    setExchangeStatus(exchange, INVALID_BASIC_HEADER, auth.substring(0, 10));
                    return;
                }
            }
            Handler.next(exchange, next);
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
        ModuleRegistry.registerModule(BasicAuthHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), masks);
    }

    @Override
    public void reload() {
        config = BasicAuthConfig.load();
    }
}
