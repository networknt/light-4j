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

package com.networknt.security;

import com.networknt.config.Config;
import com.networknt.status.Status;
import com.networknt.utility.Constants;
import com.networknt.utility.ExpiredTokenException;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Created by steve on 01/09/16.
 */
public class JwtVerifyHandler implements HttpHandler {
    static final Logger logger = LoggerFactory.getLogger(JwtVerifyHandler.class);

    static final String STATUS_INVALID_JWT_TOKEN = "ERR1000";
    static final String STATUS_JWT_TOKEN_EXPIRED = "ERR1001";
    static final String STATUS_MISSING_JWT_TOKEN = "ERR1002";


    private volatile HttpHandler next;

    public JwtVerifyHandler(final HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        HeaderMap headerMap = exchange.getRequestHeaders();
        String authorization = headerMap.getFirst(Headers.AUTHORIZATION);
        String jwt = JwtHelper.getJwtFromAuthorization(authorization);
        if(jwt != null) {
            try {
                JwtClaims claims = JwtHelper.verifyJwt(jwt);
                // put claims into request header so that scope can be verified per endpoint.
                headerMap.add(new HttpString(Constants.CLIENT_ID), claims.getStringClaimValue(Constants.CLIENT_ID));
                headerMap.add(new HttpString(Constants.USER_ID), claims.getStringClaimValue(Constants.USER_ID));
                headerMap.add(new HttpString(Constants.SCOPE), claims.getStringListClaimValue(Constants.SCOPE).toString());


                next.handleRequest(exchange);
            } catch (InvalidJwtException e) {
                // only log it and unauthorized is returned.
                logger.error("Exception: ", e);
                Status status = new Status(STATUS_INVALID_JWT_TOKEN);
                exchange.setStatusCode(status.getStatusCode());
                exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(status));
            } catch (ExpiredTokenException e) {
                Status status = new Status(STATUS_JWT_TOKEN_EXPIRED);
                exchange.setStatusCode(status.getStatusCode());
                exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(status));
            }
        } else {
            Status status = new Status(STATUS_MISSING_JWT_TOKEN);
            exchange.setStatusCode(status.getStatusCode());
            exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(status));
        }
    }

    public HttpHandler getNext() {
        return next;
    }

    public JwtVerifyHandler setNext(final HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }
}
