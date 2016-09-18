package com.networknt.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.Config;
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
 * OAuth2 server, please take a look at https://github.com/networknt/undertow-server-oauth2
 *
 * Created by steve on 18/09/16.
 */
public class JwtMockHandler implements HttpHandler {

    public static final String ENABLE_MOCK_JWT = "enableMockJwt";

    public JwtMockHandler() {}

    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(
                Headers.CONTENT_TYPE, "application/json");

        Map<String, Object> resMap = new HashMap<String, Object>();
        resMap.put("access_token", JwtHelper.getJwt(mockClaims()));
        resMap.put("token_type", "bearer");
        resMap.put("expires_in", 600);
        exchange.getResponseSender().send(ByteBuffer.wrap(
                Config.getInstance().getMapper().writeValueAsBytes(
                        resMap)));
    }

    public JwtClaims mockClaims() {
        JwtClaims claims = JwtHelper.getDefaultJwtClaims();
        claims.setClaim("user_id", "steve");
        claims.setClaim("user_type", "EMPLOYEE");
        claims.setClaim("client_id", "aaaaaaaa-1234-1234-1234-bbbbbbbb");
        List<String> scope = Arrays.asList("api.r", "api.w");
        claims.setStringListClaim("scope", scope); // multi-valued claims work too and will end up as a JSON array
        return claims;
    }
}
