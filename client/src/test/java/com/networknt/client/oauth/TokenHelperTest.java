package com.networknt.client.oauth;

import com.networknt.client.ClientTest;
import com.networknt.config.Config;
import com.networknt.security.JwtHelper;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.util.Headers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * This is the tests for TokenHelper and it doesn't need live light-oauth2
 * server up and running.
 *
 */
public class TokenHelperTest {
    static final Logger logger = LoggerFactory.getLogger(ClientTest.class);

    static Undertow server = null;

    @BeforeClass
    public static void setUp() {
        if(server == null) {
            logger.info("starting server");
            server = Undertow.builder()
                    .addHttpListener(8887, "localhost")
                    .setHandler(Handlers.header(Handlers.path()
                                    .addPrefixPath("/oauth2/key", (exchange) -> {
                                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                                        exchange.getResponseSender().send("OK");
                                    })
                                    .addPrefixPath("/oauth2/token", (exchange) -> {
                                        // create a token that expired in 5 seconds.
                                        Map<String, Object> map = new HashMap<>();
                                        String token = getJwt(5);
                                        map.put("access_token", token);
                                        map.put("token_type", "Bearer");
                                        map.put("expires_in", 5);
                                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                                        exchange.getResponseSender().send(ByteBuffer.wrap(
                                                Config.getInstance().getMapper().writeValueAsBytes(map)));
                                    }),
                            Headers.SERVER_STRING, "U-tow"))
                    .build();
            server.start();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if(server != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
            server.stop();
            System.out.println("The server is stopped.");
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static String getJwt(int expiredInSeconds) throws Exception {
        JwtClaims claims = getTestClaims();
        claims.setExpirationTime(NumericDate.fromMilliseconds(System.currentTimeMillis() + expiredInSeconds * 1000));
        return JwtHelper.getJwt(claims);
    }

    private static JwtClaims getTestClaims() {
        JwtClaims claims = JwtHelper.getDefaultJwtClaims();
        claims.setClaim("user_id", "steve");
        claims.setClaim("user_type", "EMPLOYEE");
        claims.setClaim("client_id", "aaaaaaaa-1234-1234-1234-bbbbbbbb");
        List<String> scope = Arrays.asList("api.r", "api.w");
        claims.setStringListClaim("scope", scope); // multi-valued claims work too and will end up as a JSON array
        return claims;
    }

    @Test
    public void testGetToken() throws Exception {
        AuthorizationCodeRequest tokenRequest = new AuthorizationCodeRequest();
        tokenRequest.setClientId("test_client");
        tokenRequest.setClientSecret("test_secret");
        tokenRequest.setGrantType(TokenRequest.AUTHORIZATION_CODE);
        List<String> list = new ArrayList<>();
        list.add("test.r");
        list.add("test.w");
        tokenRequest.setScope(list);
        tokenRequest.setServerUrl("http://localhost:8887");
        tokenRequest.setUri("/oauth2/token");

        tokenRequest.setRedirectUri("https://localhost:8443/authorize");
        tokenRequest.setAuthCode("test_code");

        TokenResponse tokenResponse = TokenHelper.getToken(tokenRequest, true);
        System.out.println("tokenResponse = " + tokenResponse);
    }
}
