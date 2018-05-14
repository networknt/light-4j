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

package com.networknt.csrf;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.utility.Constants;
import com.networknt.utility.Util;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.lang.JoseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.net.URI;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Created by steve on 23/09/16.
 */
public class StatelessCsrfHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(StatelessCsrfHandlerTest.class);

    static Undertow apiServer = null;
    static Undertow tokenServer = null;
    // this is to simulate the randomly generated csrfToken for testing only to ensure
    // both header csrf and jwt csrf tokens are the same.
    static String csrfToken = "UniqueCsrfToken";

    @BeforeClass
    public static void setUp() {
        if(apiServer == null) {
            logger.info("starting API server");
            HttpHandler handler = getTestHandler();
            StatelessCsrfHandler statelessCsrfHandler = new StatelessCsrfHandler();
            statelessCsrfHandler.setNext(handler);
            handler = statelessCsrfHandler;
            SimulateJwtVerifierHandler jwtVerifierHandler = new SimulateJwtVerifierHandler();
            jwtVerifierHandler.setNext(handler);
            handler = jwtVerifierHandler;
            apiServer = Undertow.builder()
                    .addHttpListener(8080, "localhost")
                    .setHandler(handler)
                    .build();
            apiServer.start();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if(apiServer != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {

            }
            apiServer.stop();
            logger.info("The API server is stopped.");
        }
    }

    static RoutingHandler getTestHandler() {
        return Handlers.routing()
                .add(Methods.GET, "/api", exchange -> {
                    exchange.getResponseSender().send("OK");
                });
    }

    @Test
    public void testStatelessCsrf() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI("http://localhost:8080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/api").setMethod(Methods.GET);
            // csrf header needs to pass in
            request.getRequestHeaders().put(Constants.CSRF_TOKEN, csrfToken);
            // need to pass in the Authorization header with Bearer jwt token
            String token = getJwt(600, csrfToken);
            request.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer " + token);
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        Assert.assertEquals(200, statusCode);
        Assert.assertEquals("OK", body);
    }

    @Test
    public void testWithoutCsrfHeader() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI("http://localhost:8080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/api").setMethod(Methods.GET);
            // need to pass in the Authorization header with Bearer jwt token
            String token = getJwt(600, csrfToken);
            request.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer " + token);
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        logger.debug("statusCode = " + statusCode);
        logger.debug("body = " + body);
        Assert.assertEquals(400, statusCode);
        Assert.assertNotNull(body);
        Assert.assertTrue(body.contains("CSRF_HEADER_MISSING"));
    }

    @Test
    public void testCsrfMissingInJwt() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI("http://localhost:8080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/api").setMethod(Methods.GET);
            // csrf header needs to pass in
            request.getRequestHeaders().put(Constants.CSRF_TOKEN, csrfToken);
            // need to pass in the Authorization header with Bearer jwt token
            String token = getJwt(600, null);
            request.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer " + token);
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        logger.debug("statusCode = " + statusCode);
        logger.debug("body = " + body);
        Assert.assertEquals(400, statusCode);
        Assert.assertNotNull(body);
        Assert.assertTrue(body.contains("CSRF_TOKEN_MISSING_IN_JWT"));
    }


    @Test
    public void testUnmatchedCsrf() throws Exception {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI("http://localhost:8080"), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/api").setMethod(Methods.GET);
            // csrf header needs to pass in
            request.getRequestHeaders().put(Constants.CSRF_TOKEN, csrfToken);
            // need to pass in the Authorization header with Bearer jwt token
            String token = getJwt(600, "AnotherCsrf");
            request.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer " + token);
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        logger.debug("statusCode = " + statusCode);
        logger.debug("body = " + body);
        Assert.assertEquals(400, statusCode);
        Assert.assertNotNull(body);
        Assert.assertTrue(body.contains("HEADER_CSRF_JWT_CSRF_NOT_MATCH"));
    }

    private static String getJwt(int expiredInSeconds, String csrfToken) throws Exception {
        JwtClaims claims = getTestClaims(csrfToken);
        claims.setExpirationTime(NumericDate.fromMilliseconds(System.currentTimeMillis() + expiredInSeconds * 1000));
        return getJwt(claims);
    }

    private static JwtClaims getTestClaims(String csrfToken) {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("urn:com:networknt:oauth2:v1");
        claims.setAudience("urn:com.networknt");
        claims.setExpirationTimeMinutesInTheFuture(10);
        claims.setGeneratedJwtId(); // a unique identifier for the token
        claims.setIssuedAtToNow();  // when the token was issued/created (now)
        claims.setNotBeforeMinutesInThePast(2); // time before which the token is not yet valid (2 minutes ago)
        claims.setClaim("version", "1.0");

        claims.setClaim("user_id", "steve");
        claims.setClaim("user_type", "EMPLOYEE");
        claims.setClaim("client_id", "aaaaaaaa-1234-1234-1234-bbbbbbbb");
        if(csrfToken != null) claims.setClaim("csrf", csrfToken);
        List<String> scope = Arrays.asList("api.r", "api.w");
        claims.setStringListClaim("scope", scope); // multi-valued claims work too and will end up as a JSON array
        return claims;
    }

    private static String getJwt(JwtClaims claims) throws JoseException {
        String jwt;

        RSAPrivateKey privateKey = (RSAPrivateKey) getPrivateKey(
                "/config/oauth/primary.jks", "password", "selfsigned");

        // A JWT is a JWS and/or a JWE with JSON claims as the payload.
        // In this example it is a JWS nested inside a JWE
        // So we first create a JsonWebSignature object.
        JsonWebSignature jws = new JsonWebSignature();

        // The payload of the JWS is JSON content of the JWT Claims
        jws.setPayload(claims.toJson());

        // The JWT is signed using the sender's private key
        jws.setKey(privateKey);
        jws.setKeyIdHeaderValue("100");

        // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        // Sign the JWS and produce the compact serialization, which will be the inner JWT/JWS
        // representation, which is a string consisting of three dot ('.') separated
        // base64url-encoded parts in the form Header.Payload.Signature
        jwt = jws.getCompactSerialization();
        return jwt;
    }

    private static PrivateKey getPrivateKey(String filename, String password, String key) {
        PrivateKey privateKey = null;

        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(Http2Client.class.getResourceAsStream(filename),
                    password.toCharArray());

            privateKey = (PrivateKey) keystore.getKey(key,
                    password.toCharArray());
        } catch (Exception e) {
            logger.error("Exception:", e);
        }

        if (privateKey == null) {
            logger.error("Failed to retrieve private key from keystore");
        }

        return privateKey;
    }

}
