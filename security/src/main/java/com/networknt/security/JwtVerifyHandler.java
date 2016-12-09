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
import com.networknt.handler.MiddlewareHandler;
import com.networknt.status.Status;
import com.networknt.swagger.*;
import com.networknt.utility.Constants;
import com.networknt.exception.ExpiredTokenException;
import com.networknt.utility.ModuleRegistry;
import io.swagger.models.*;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by steve on 01/09/16.
 */
public class JwtVerifyHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(JwtVerifyHandler.class);

    static final String ENABLE_VERIFY_SCOPE = "enableVerifyScope";

    static final String STATUS_INVALID_AUTH_TOKEN = "ERR10000";
    static final String STATUS_AUTH_TOKEN_EXPIRED = "ERR10001";
    static final String STATUS_MISSING_AUTH_TOKEN = "ERR10002";
    static final String STATUS_INVALID_SCOPE_TOKEN = "ERR10003";
    static final String STATUS_SCOPE_TOKEN_EXPIRED = "ERR10004";
    static final String STATUS_AUTH_TOKEN_SCOPE_MISMATCH = "ERR10005";
    static final String STATUS_SCOPE_TOKEN_SCOPE_MISMATCH = "ERR10006";
    static final String STATUS_INVALID_REQUEST_PATH = "ERR10007";
    static final String STATUS_METHOD_NOT_ALLOWED = "ERR10008";

    static final Map<String, Object> config = Config.getInstance().getJsonMapConfig(JwtHelper.SECURITY_CONFIG);

    private volatile HttpHandler next;

    public JwtVerifyHandler() {}

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
                if(config != null && (Boolean)config.get(ENABLE_VERIFY_SCOPE) && SwaggerHelper.swagger != null) {
                    Operation operation;
                    SwaggerOperation swaggerOperation = exchange.getAttachment(SwaggerHandler.SWAGGER_OPERATION);
                    if(swaggerOperation == null) {
                        final NormalisedPath requestPath = new ApiNormalisedPath(exchange.getRequestURI());
                        final Optional<NormalisedPath> maybeApiPath = SwaggerHelper.findMatchingApiPath(requestPath);
                        if (!maybeApiPath.isPresent()) {
                            Status status = new Status(STATUS_INVALID_REQUEST_PATH);
                            exchange.setStatusCode(status.getStatusCode());
                            exchange.getResponseSender().send(status.toString());
                            return;
                        }

                        final NormalisedPath swaggerPathString = maybeApiPath.get();
                        final Path swaggerPath = SwaggerHelper.swagger.getPath(swaggerPathString.original());

                        final HttpMethod httpMethod = HttpMethod.valueOf(exchange.getRequestMethod().toString());
                        operation = swaggerPath.getOperationMap().get(httpMethod);

                        if (operation == null) {
                            Status status = new Status(STATUS_METHOD_NOT_ALLOWED);
                            exchange.setStatusCode(status.getStatusCode());
                            exchange.getResponseSender().send(status.toString());
                            return;
                        }
                        swaggerOperation = new SwaggerOperation(swaggerPathString, swaggerPath, httpMethod, operation);
                        swaggerOperation.setEndpoint(swaggerPathString.normalised() + "@" + httpMethod);
                        swaggerOperation.setClientId(claims.getStringClaimValue(Constants.CLIENT_ID));
                        exchange.putAttachment(SwaggerHandler.SWAGGER_OPERATION, swaggerOperation);
                    } else {
                        operation = swaggerOperation.getOperation();
                        swaggerOperation.setClientId(claims.getStringClaimValue(Constants.CLIENT_ID));
                    }

                    // is there a scope token
                    String scopeHeader = headerMap.getFirst(Constants.SCOPE_TOKEN);
                    String scopeJwt = JwtHelper.getJwtFromAuthorization(scopeHeader);
                    List<String> secondaryScopes = null;
                    if(scopeJwt != null) {
                        try {
                            JwtClaims scopeClaims = JwtHelper.verifyJwt(scopeJwt);
                            secondaryScopes = scopeClaims.getStringListClaimValue("scope");
                            headerMap.add(new HttpString(Constants.SCOPE_CLIENT_ID), scopeClaims.getStringClaimValue(Constants.CLIENT_ID));
                        } catch (InvalidJwtException | MalformedClaimException e) {
                            logger.error("InvalidJwtException", e);
                            Status status = new Status(STATUS_INVALID_SCOPE_TOKEN);
                            exchange.setStatusCode(status.getStatusCode());
                            exchange.getResponseSender().send(status.toString());
                            return;
                        } catch (ExpiredTokenException e) {
                            Status status = new Status(STATUS_SCOPE_TOKEN_EXPIRED);
                            exchange.setStatusCode(status.getStatusCode());
                            exchange.getResponseSender().send(status.toString());
                            return;
                        }
                    }

                    // get scope defined in swagger spec for this endpoint.
                    List<String> specScopes = null;
                    List<Map<String, List<String>>> security = operation.getSecurity();
                    if(security != null) {
                        for(Map<String, List<String>> requirement: security) {
                            specScopes = requirement.get(SwaggerHelper.oauth2Name);
                            if(specScopes != null) break;
                        }
                    }

                    // validate scope
                    if (scopeHeader != null) {
                        if (secondaryScopes == null || !matchedScopes(secondaryScopes, specScopes)) {
                            if(logger.isDebugEnabled()) {
                                logger.debug("Scopes are not matched in scope token" + Encode.forJava(scopeHeader));
                            }
                            Status status = new Status(STATUS_SCOPE_TOKEN_SCOPE_MISMATCH);
                            exchange.setStatusCode(status.getStatusCode());
                            exchange.getResponseSender().send(status.toString());
                            return;
                        }
                    } else {
                        // no scope token, verify scope from auth token.
                        List<String> primaryScopes;
                        try {
                            primaryScopes = claims.getStringListClaimValue("scope");
                        } catch (MalformedClaimException e) {
                            logger.error("MalformedClaimException", e);
                            Status status = new Status(STATUS_INVALID_AUTH_TOKEN);
                            exchange.setStatusCode(status.getStatusCode());
                            exchange.getResponseSender().send(status.toString());
                            return;
                        }
                        if (!matchedScopes(primaryScopes, specScopes)) {
                            if(logger.isDebugEnabled()) {
                                logger.debug("Authorization jwt token scope is not matched " + Encode.forJava(jwt));
                            }
                            Status status = new Status(STATUS_AUTH_TOKEN_SCOPE_MISMATCH);
                            exchange.setStatusCode(status.getStatusCode());
                            exchange.getResponseSender().send(status.toString());
                            return;
                        }
                    }
                }
                next.handleRequest(exchange);
            } catch (InvalidJwtException e) {
                // only log it and unauthorized is returned.
                logger.error("Exception: ", e);
                Status status = new Status(STATUS_INVALID_AUTH_TOKEN);
                exchange.setStatusCode(status.getStatusCode());
                exchange.getResponseSender().send(status.toString());
            } catch (ExpiredTokenException e) {
                Status status = new Status(STATUS_AUTH_TOKEN_EXPIRED);
                exchange.setStatusCode(status.getStatusCode());
                exchange.getResponseSender().send(status.toString());
            }
        } else {
            Status status = new Status(STATUS_MISSING_AUTH_TOKEN);
            exchange.setStatusCode(status.getStatusCode());
            exchange.getResponseSender().send(status.toString());
        }
    }

    protected boolean matchedScopes(List<String> jwtScopes, List<String> specScopes) {
        boolean matched = false;
        if(specScopes != null && specScopes.size() > 0) {
            if(jwtScopes != null && jwtScopes.size() > 0) {
                for(String scope: specScopes) {
                    if(jwtScopes.contains(scope)) {
                        matched = true;
                        break;
                    }
                }
            }
        } else {
            matched = true;
        }
        return matched;
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
        Object object = config.get(JwtHelper.ENABLE_VERIFY_JWT);
        return object != null && (Boolean) object;
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(JwtVerifyHandler.class.getName(), config, null);
    }

}
