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

package com.networknt.client;

import com.networknt.config.Config;
import com.networknt.utility.Constants;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.util.EntityUtils;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.lang.JoseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@Deprecated
public class ClientTest {
    static final Logger logger = LoggerFactory.getLogger(ClientTest.class);

    static Undertow server = null;

    @Before
    public void setUp() {
        if(server == null) {
            logger.info("starting server");
            server = Undertow.builder()
                    .addHttpListener(7777, "localhost")
                    .setHandler(Handlers.header(Handlers.path()
                                    .addPrefixPath("/api", new ApiHandler())
                                    .addPrefixPath("/oauth2/token", new OAuthHandler()),
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

    final class ApiHandler implements HttpHandler {
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            boolean hasScopeToken = exchange.getRequestHeaders().contains(Constants.SCOPE_TOKEN);
            Assert.assertTrue(hasScopeToken);
            String scopeToken = exchange.getRequestHeaders().get(Constants.SCOPE_TOKEN, 0);
            boolean expired = isTokenExpired(scopeToken);
            Assert.assertFalse(expired);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(ByteBuffer.wrap(
                    Config.getInstance().getMapper().writeValueAsBytes(
                            Collections.singletonMap("message", "OK!"))));
        }
    }

    final class OAuthHandler implements HttpHandler {
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            try {
                int sleepTime = randInt(1, 3) * 1000;
                if(sleepTime >= 2000) {
                    sleepTime = 3000;
                } else {
                    sleepTime = 1000;
                }
                Thread.sleep(sleepTime);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // create a token that expired in 5 seconds.
            Map<String, Object> map = new HashMap<>();
            String token = getJwt(5);
            map.put("access_token", token);
            map.put("token_type", "Bearer");
            map.put("expires_in", 5);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(ByteBuffer.wrap(
                    Config.getInstance().getMapper().writeValueAsBytes(map)));
        }
    }

    @Test
    public void testSingleSychClient() throws Exception {
        callApiSync();
    }

    @Test
    public void testSingleAsychClient() throws Exception {
        callApiAsync();
    }

    public String callApiSync() throws Exception {

        String url = "http://localhost:7777/api";
        CloseableHttpClient client = Client.getInstance().getSyncClient();
        HttpGet httpGet = new HttpGet(url);
        ResponseHandler<String> responseHandler = response -> {
            int status = response.getStatusLine().getStatusCode();
            Assert.assertEquals(200, status);
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        };
        String responseBody;
        try {
            Client.getInstance().populateHeader(httpGet, "Bearer token", "cid", "tid");
            responseBody = client.execute(httpGet, responseHandler);
            Assert.assertEquals("{\"message\":\"OK!\"}", responseBody);
            logger.debug("message = " + responseBody);
        } catch (Exception e) {
            e.printStackTrace();
            responseBody = "{\"message\":\"Error!\"}";
        }
        return responseBody;
    }

    public String callApiAsync() throws Exception {
        String url = "http://localhost:7777/api";
        CloseableHttpAsyncClient client = Client.getInstance().getAsyncClient();
        HttpGet httpGet = new HttpGet(url);
        try {
            Client.getInstance().populateHeader(httpGet, "Bearer token", "cid", "tid");
            Future<HttpResponse> future = client.execute(httpGet, null);
            HttpResponse response = future.get();
            int status = response.getStatusLine().getStatusCode();
            Assert.assertEquals(200, status);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity);
            Assert.assertEquals("{\"message\":\"OK!\"}", result);
            logger.debug("message = " + result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"message\":\"Error!\"}";
        }
    }

    private void callApiSyncMultiThread(final int threadCount) throws InterruptedException, ExecutionException {
        Callable<String> task = this::callApiSync;
        List<Callable<String>> tasks = Collections.nCopies(threadCount, task);
        long start = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<String>> futures = executorService.invokeAll(tasks);
        List<String> resultList = new ArrayList<>(futures.size());
        // Check for exceptions
        for (Future<String> future : futures) {
            // Throws an exception if an exception was thrown by the task.
            resultList.add(future.get());
        }
        long last = (System.currentTimeMillis() - start);
        System.out.println("resultList = " + resultList + " response time = " + last);
    }

    //@Test
    public void testSyncAboutToExpire() throws InterruptedException, ExecutionException {
        for(int i = 0; i < 100; i++) {
            callApiSyncMultiThread(4);
            logger.info("called times: " + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    //@Test
    public void testSyncExpired() throws InterruptedException, ExecutionException {
        for(int i = 0; i < 100; i++) {
            callApiSyncMultiThread(4);
            logger.info("called times: " + i);
            try {
                Thread.sleep(6000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    //@Test
    public void testMixed() throws InterruptedException, ExecutionException {
        for(int i = 0; i < 100; i++) {
            callApiSyncMultiThread(4
            );
            logger.info("called times: " + i);
            try {
                int sleepTime = randInt(1, 6) * 1000;
                if (sleepTime > 3000) {
                    sleepTime = 6000;
                } else {
                    sleepTime = 1000;
                }
                Thread.sleep(sleepTime);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void callApiAsyncMultiThread(final int threadCount) throws InterruptedException, ExecutionException {
        Callable<String> task = this::callApiAsync;
        List<Callable<String>> tasks = Collections.nCopies(threadCount, task);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<String>> futures = executorService.invokeAll(tasks);
        List<String> resultList = new ArrayList<>(futures.size());
        for (Future<String> future : futures) {
            resultList.add(future.get());
        }
        System.out.println("resultList = " + resultList);
    }

    //@Test
    public void testAsyncAboutToExpire() throws InterruptedException, ExecutionException {
        for(int i = 0; i < 10; i++) {
            callApiAsyncMultiThread(4);
            logger.info("called times: " + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    //@Test
    public void testAsyncExpired() throws InterruptedException, ExecutionException {
        for(int i = 0; i < 10; i++) {
            callApiAsyncMultiThread(4);
            logger.info("called times: " + i);
            try {
                Thread.sleep(6000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static int randInt(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max-min) + 1) + min;
    }

    private static boolean isTokenExpired(String authorization) {
        boolean expired = false;
        String jwt = getJwtFromAuthorization(authorization);
        if(jwt != null) {
            try {
                JwtConsumer consumer = new JwtConsumerBuilder()
                        .setSkipAllValidators()
                        .setDisableRequireSignature()
                        .setSkipSignatureVerification()
                        .build();

                JwtContext jwtContext = consumer.process(jwt);
                JwtClaims jwtClaims = jwtContext.getJwtClaims();
                JsonWebStructure structure = jwtContext.getJoseObjects().get(0);

                try {
                    if ((NumericDate.now().getValue() - 60) >= jwtClaims.getExpirationTime().getValue()) {
                        expired = true;
                    }
                } catch (MalformedClaimException e) {
                    logger.error("MalformedClaimException:", e);
                    throw new InvalidJwtException("MalformedClaimException", e);
                }
            } catch(InvalidJwtException e) {
                e.printStackTrace();
            }
        }
        return expired;
    }

    private static String getJwt(int expiredInSeconds) throws Exception {
        JwtClaims claims = getTestClaims();
        claims.setExpirationTime(NumericDate.fromMilliseconds(System.currentTimeMillis() + expiredInSeconds * 1000));
        return getJwt(claims);
    }

    private static JwtClaims getTestClaims() {
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
        List<String> scope = Arrays.asList("api.r", "api.w");
        claims.setStringListClaim("scope", scope); // multi-valued claims work too and will end up as a JSON array
        return claims;
    }

    public static String getJwtFromAuthorization(String authorization) {
        String jwt = null;
        if(authorization != null) {
            String[] parts = authorization.split(" ");
            if (parts.length == 2) {
                String scheme = parts[0];
                String credentials = parts[1];
                Pattern pattern = Pattern.compile("^Bearer$", Pattern.CASE_INSENSITIVE);
                if (pattern.matcher(scheme).matches()) {
                    jwt = credentials;
                }
            }
        }
        return jwt;
    }

    public static String getJwt(JwtClaims claims) throws JoseException {
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
