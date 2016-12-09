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
import com.networknt.security.JwtHelper;
import com.networknt.utility.Constants;
import com.networknt.exception.ExpiredTokenException;
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
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

public class ClientTest {
    static final Logger logger = LoggerFactory.getLogger(ClientTest.class);

    static Undertow server = null;

    @Before
    public void setUp() {
        if(server == null) {
            logger.info("starting server");
            server = Undertow.builder()
                    .addHttpListener(8887, "localhost")
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
            } catch (InterruptedException ie) {
            }
            server.stop();
            System.out.println("The server is stopped.");
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
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
            Map<String, Object> map = new HashMap<String, Object>();
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

        String url = "http://localhost:8887/api";
        CloseableHttpClient client = Client.getInstance().getSyncClient();
        HttpGet httpGet = new HttpGet(url);
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
            @Override
            public String handleResponse(
                    final HttpResponse response) throws ClientProtocolException, IOException {
                int status = response.getStatusLine().getStatusCode();
                Assert.assertEquals(200, status);
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }

        };
        String responseBody = null;
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
        String url = "http://localhost:8887/api";
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
        Callable<String> task = new Callable<String>() {
            @Override
            public String call() throws Exception {
                return callApiSync();
            }
        };
        List<Callable<String>> tasks = Collections.nCopies(threadCount, task);
        long start = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<String>> futures = executorService.invokeAll(tasks);
        List<String> resultList = new ArrayList<String>(futures.size());
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
            } catch (InterruptedException ie) {
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
            } catch (InterruptedException ie) {
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
            } catch (InterruptedException ie) {
            }
        }
    }

    private void callApiAsyncMultiThread(final int threadCount) throws InterruptedException, ExecutionException {
        Callable<String> task = new Callable<String>() {
            @Override
            public String call() throws Exception {
                return callApiAsync();
            }
        };
        List<Callable<String>> tasks = Collections.nCopies(threadCount, task);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<String>> futures = executorService.invokeAll(tasks);
        List<String> resultList = new ArrayList<String>(futures.size());
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
            } catch (InterruptedException ie) {
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
            } catch (InterruptedException ie) {
            }
        }
    }

    private static int randInt(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max-min) + 1) + min;
    }

    private boolean isTokenExpired(String authorization) {
        boolean expired = false;
        String jwt = JwtHelper.getJwtFromAuthorization(authorization);
        if(jwt != null) {
            try {
                JwtHelper.verifyJwt(jwt);
            } catch(InvalidJwtException e) {
                e.printStackTrace();
            } catch(ExpiredTokenException e) {
                expired = true;
            }
        }
        return expired;
    }

    private String getJwt(int expiredInSeconds) throws Exception {
        JwtClaims claims = getTestClaims();
        claims.setExpirationTime(NumericDate.fromMilliseconds(System.currentTimeMillis() + expiredInSeconds * 1000));
        return JwtHelper.getJwt(claims);
    }

    private JwtClaims getTestClaims() {
        JwtClaims claims = JwtHelper.getDefaultJwtClaims();
        claims.setClaim("user_id", "steve");
        claims.setClaim("user_type", "EMPLOYEE");
        claims.setClaim("client_id", "aaaaaaaa-1234-1234-1234-bbbbbbbb");
        List<String> scope = Arrays.asList("api.r", "api.w");
        claims.setStringListClaim("scope", scope); // multi-valued claims work too and will end up as a JSON array
        return claims;
    }

}
