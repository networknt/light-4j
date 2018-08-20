/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

package com.networknt.basic;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.status.Status;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This is a middleware handler that handles basic authentication in restful APIs. It is not used
 * in most situations as OAuth 2.0 is the standard. In certain cases for example, the server is
 * deployed to IoT devices, basic authentication can be used to replace OAuth 2.0 handlers.
 *
 * There are multiple users that can be defined in basic.yml config file. And access is logged into
 * th audit.log
 *
 * @author Steve Hu
 */
public class BasicAuthHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(BasicAuthHandler.class);
    static final String CONFIG_NAME = "basic";
    static final BasicConfig config = (BasicConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, BasicConfig.class);

    static final String MISSING_AUTH_TOKEN = "ERR10002";
    static final String INVALID_BASIC_HEADER = "ERR10046";
    static final String INVALID_USERNAME_OR_PASSWORD = "ERR10047";

    private volatile HttpHandler next;

    public BasicAuthHandler() {
        if(logger.isInfoEnabled()) logger.info("BasicAuthHandler is constructed.");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        String auth = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
        if(auth == null || auth.trim().length() == 0) {
            setExchangeStatus(exchange, MISSING_AUTH_TOKEN);
            return;
        } else {
            // verify the header with the config file.
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
                    List<Map<String, Object>> users = config.getUsers();
                    Optional<Map<String, Object>> result = config.getUsers().stream()
                            .filter(user -> user.get("username").equals(username) && user.get("password").equals(password))
                            .findFirst();
                    if(!result.isPresent()) {
                        setExchangeStatus(exchange, INVALID_USERNAME_OR_PASSWORD);
                        return;
                    }
                } else {
                    setExchangeStatus(exchange, INVALID_BASIC_HEADER, auth);
                    return;
                }
            } else {
                setExchangeStatus(exchange, INVALID_BASIC_HEADER, auth);
                return;
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
        masks.add("");
        ModuleRegistry.registerModule(BasicAuthHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }
}
