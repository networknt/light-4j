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

package com.networknt.security;

import com.networknt.config.Config;
import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.jose4j.jwt.JwtClaims;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a jwt token provider for testing only. It should be injected into the server
 * after it is started. Do not use it on production runtime. If you need an external
 * OAuth2 server, please take a look at https://github.com/networknt/light-oauth2
 *
 * @author Steve Hu
 */
public class JwtMockHandler implements LightHttpHandler {

    public static final String ENABLE_MOCK_JWT = "enableMockJwt";

    public JwtMockHandler() {}

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(
                Headers.CONTENT_TYPE, "application/json");

        Map<String, Object> resMap = new HashMap<>();
        resMap.put("access_token", JwtIssuer.getJwt(mockClaims()));
        resMap.put("token_type", "bearer");
        resMap.put("expires_in", 600);
        exchange.getResponseSender().send(ByteBuffer.wrap(
                Config.getInstance().getMapper().writeValueAsBytes(
                        resMap)));
    }

    public JwtClaims mockClaims() {
        JwtClaims claims = JwtIssuer.getDefaultJwtClaims();
        claims.setClaim("user_id", "steve");
        claims.setClaim("user_type", "EMPLOYEE");
        claims.setClaim("client_id", "aaaaaaaa-1234-1234-1234-bbbbbbbb");
        List<String> scope = Arrays.asList("api.r", "api.w");
        claims.setStringListClaim("scope", scope); // multi-valued claims work too and will end up as a JSON array
        return claims;
    }
}
