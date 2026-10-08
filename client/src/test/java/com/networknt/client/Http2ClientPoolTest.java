/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.networknt.client;


import com.networknt.config.Config;
import com.networknt.client.oauth.Jwt;
import com.networknt.client.oauth.OauthHelper;
import com.networknt.client.oauth.TokenManager;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.monad.Result;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import com.networknt.client.simplepool.SimpleConnectionState;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.io.Receiver;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.*;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;


public class Http2ClientPoolTest {
    static final Logger logger = LoggerFactory.getLogger(Http2ClientPoolTest.class);
    public static final String SLOW = "/slow";
    static Undertow server = null;
    static SSLContext sslContext;
    private static final String message = "Hello World!";
    public static final String MESSAGE = "/message";
    public static final String SLOW_MESSAGE = "/slowMessage";
    public static final String POST = "/post";
    public static final String FORM = "/form";
    public static final String TOKEN = "/oauth2/token";
    public static final String API = "/api";
    public static final String KEY = "/oauth2/key";

    private static final String SERVER_KEY_STORE = "server.keystore";
    private static final String SERVER_TRUST_STORE = "server.truststore";
    private static final String CLIENT_KEY_STORE = "client.keystore";
    private static final String CLIENT_TRUST_STORE = "client.truststore";
    private static final char[] STORE_PASSWORD = "password".toCharArray();

    private static XnioWorker worker;

    private static final URI ADDRESS;

    public static final String CONFIG_NAME = "client";
    static ClientConfig config;

    static {
        try {
            ADDRESS = new URI("http://localhost:7777");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static int slowCount;
    private static final Map<String, TokenScenario> tokenScenarios = new ConcurrentHashMap<>();
    private TokenScenario tokenScenario;
    private long originalRenewWindow;

    private static final class TokenScenario {
        final String serviceId = "pool-test-" + UUID.randomUUID();
        final AtomicInteger tokenRequests = new AtomicInteger();
        final AtomicInteger activeRequests = new AtomicInteger();
        final Queue<String> receivedTokens = new ConcurrentLinkedQueue<>();
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final AuthServerConfig authServer = new AuthServerConfig();
        final Queue<Thread> requestThreads = new ConcurrentLinkedQueue<>();
        final CountDownLatch bodyReaderReady = new CountDownLatch(1);
        final CountDownLatch renewalStarted = new CountDownLatch(1);
        final CountDownLatch releaseRenewal = new CountDownLatch(1);
        volatile int heldAttempt;
        volatile int failureAttempt;
        volatile int initialLifetimeSeconds;
        volatile Jwt cachedJwt;
    }

    static void sendMessage(final HttpServerExchange exchange) {
        exchange.setStatusCode(StatusCodes.OK);
        exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, message.length() + "");
        final Sender sender = exchange.getResponseSender();
        sender.send(message);
    }


    @BeforeEach
    public void setUp() {
        slowCount = 0;
        // Each scenario has its own supported service/cache key, including across test classes.
        tokenScenario = new TokenScenario();
        // Initialize the fixture's configured timings before calculating token lifetimes.
        new Jwt();
        originalRenewWindow = Jwt.getTokenRenewBeforeExpired();
        tokenScenario.initialLifetimeSeconds = tokenLifetimeSeconds(originalRenewWindow);
        tokenScenario.authServer.setServerUrl(ADDRESS.toString());
        tokenScenario.authServer.setUri(TOKEN + "/" + tokenScenario.serviceId);
        tokenScenario.authServer.setClientId(config.getOAuth().getToken().getClientCredentials().getClientId());
        tokenScenario.authServer.setClientSecret(config.getOAuth().getToken().getClientCredentials().getClientSecret());
        tokenScenario.authServer.setScope(Arrays.asList("api.r", "api.w"));
        tokenScenarios.put(tokenScenario.serviceId, tokenScenario);
    }

    @AfterEach
    public void cleanUpTokenScenario() throws InterruptedException {
        try {
            tokenScenario.releaseRenewal.countDown();
            awaitCondition(() -> tokenScenario.activeRequests.get() == 0
                            && (tokenScenario.cachedJwt == null || !tokenScenario.cachedJwt.isRenewing()),
                    10000, "Token refresh did not finish before fixture cleanup");
        } finally {
            tokenScenarios.remove(tokenScenario.serviceId);
            Jwt.setTokenRenewBeforeExpired(originalRenewWindow);
        }
    }

    @BeforeAll
    public static void beforeClass() throws IOException {
        config = ClientConfig.get(CONFIG_NAME);
        // Use the client's shared worker. Http2Client caches one pool per URI for the whole JVM and a
        // pool keeps the worker it was created with, so a per-class worker shut down in afterClass
        // leaves a stale pool behind for whichever test class runs next against the same URI.
        Http2Client.getInstance();
        worker = Http2Client.WORKER;

        if(server == null) {
            System.out.println("starting server");
            Undertow.Builder builder = Undertow.builder();

            sslContext = createSSLContext(loadKeyStore(SERVER_KEY_STORE), loadKeyStore(SERVER_TRUST_STORE), false);
            builder.addHttpsListener(7778, "localhost", sslContext);
            builder.addHttpListener(7777, "localhost");

            builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);


            server = builder
                    .setBufferSize(1024 * 16)
                    .setIoThreads(Runtime.getRuntime().availableProcessors() * 2) //this seems slightly faster in some configurations
                    .setSocketOption(Options.BACKLOG, 10000)
                    .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false) //don't send a keep-alive header for HTTP/1.1 requests, as it is not required
                    .setServerOption(UndertowOptions.ALWAYS_SET_DATE, true)
                    .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
                    .setHandler(new PathHandler()
                            .addExactPath(MESSAGE, exchange -> {
                                sendMessage(exchange);
                            })
                            .addExactPath(SLOW_MESSAGE, exchange -> {
                                Thread.sleep(20);
                                sendMessage(exchange);
                            })
                            .addExactPath(KEY, exchange -> sendMessage(exchange))
                            .addExactPath(API, exchange -> {
                                TokenScenario scenario = tokenScenarios.get(
                                        exchange.getRequestHeaders().getFirst(ClientConfig.SERVICE_ID));
                                try {
                                    Assertions.assertNotNull(scenario, "Unknown token scenario");
                                    String scopeToken = exchange.getRequestHeaders().getFirst(HttpStringConstants.SCOPE_TOKEN);
                                    Assertions.assertNotNull(scopeToken, "Missing scope-token header");
                                    Assertions.assertFalse(isTokenExpired(scopeToken), "Expired scope token");
                                    scenario.receivedTokens.add(getJwtFromAuthorization(scopeToken));
                                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                                    exchange.getResponseSender().send(ByteBuffer.wrap(
                                            Config.getInstance().getMapper().writeValueAsBytes(
                                                    Collections.singletonMap("message", "OK!"))));
                                } catch (Throwable failure) {
                                    if (scenario != null) scenario.failure.compareAndSet(null, failure);
                                    exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                                    exchange.getResponseSender().send("Invalid scope token");
                                }
                            })
                            .addExactPath(FORM, exchange -> exchange.getRequestReceiver().receiveFullString(new Receiver.FullStringCallback() {
                                @Override
                                public void handle(HttpServerExchange exchange, String message) {
                                    exchange.getResponseSender().send(message);
                                }
                            }))
                            .addPrefixPath(TOKEN + "/", exchange -> {
                                // Controlled waits belong on a worker, never the Undertow I/O thread.
                                if (exchange.isInIoThread()) {
                                    exchange.dispatch(Http2ClientPoolTest::handleTokenRequest);
                                } else {
                                    handleTokenRequest(exchange);
                                }
                            })
                            .addExactPath(POST, exchange -> exchange.getRequestReceiver().receiveFullString(new Receiver.FullStringCallback() {
                                @Override
                                public void handle(HttpServerExchange exchange, String message) {
                                    exchange.getResponseSender().send(message);
                                }
                            }))
                            .addExactPath(SLOW, exchange -> exchange.getRequestReceiver().receiveFullString((exchange2, message) -> {
                                try {
                                    if (slowCount < 2) {
                                        Thread.sleep(4000);
                                    }
                                } catch (InterruptedException e) {
                                }
                                exchange2.getResponseSender().send(message);
                            })))
                    .setWorkerThreads(200)
                    .build();

            server.start();
        }
    }

    @AfterAll
    public static void afterClass() {
        if(server != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
            server.stop();
            server = null;
            System.out.println("The server is stopped.");
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

    }

    static Http2Client createClient() {
        return createClient(OptionMap.EMPTY);
    }

    static Http2Client createClient(final OptionMap options) {
        return Http2Client.getInstance();
    }

    @Test
    public void testConnectionClose() throws Exception {
        //
        final Http2Client client = createClient();

        final CountDownLatch latch = new CountDownLatch(1);
        SimpleConnectionState.ConnectionToken token = client.borrow(ADDRESS, worker, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        try {
            ClientRequest request = new ClientRequest().setPath(MESSAGE).setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            final AtomicReference<ClientResponse> reference = new AtomicReference<>();
            request.getRequestHeaders().add(Headers.CONNECTION, Headers.CLOSE.toString());
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
            final ClientResponse response = reference.get();
            Assertions.assertEquals(message, response.getAttachment(Http2Client.RESPONSE_BODY));
            Assertions.assertEquals(false, connection.isOpen());
        } finally {
            client.restore(token);
        }

    }

    @Test
    public void testResponseTime() throws Exception {
        //
        final Http2Client client = createClient();

        final CountDownLatch latch = new CountDownLatch(1);
        SimpleConnectionState.ConnectionToken token = client.borrow(ADDRESS, worker, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        try {
            ClientRequest request = new ClientRequest().setPath(MESSAGE).setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            final AtomicReference<AsyncResult<AsyncResponse>> reference = new AtomicReference<>();
            request.getRequestHeaders().add(Headers.CONNECTION, Headers.CLOSE.toString());
            connection.sendRequest(request, client.createFullCallback(reference, latch));
            latch.await();
            final AsyncResult<AsyncResponse> ar = reference.get();
            if(ar.succeeded()) {
                Assertions.assertEquals(message, ar.result().getResponseBody());
                System.out.println("responseBody = " + ar.result().getResponseBody() + " responseTime = " + ar.result().getResponseTime());
                // we used to check the response time greater than 0, but it is not always true on a faster machine.
                Assertions.assertNotNull(ar.result().getResponseBody());
            } else {
                ar.cause().printStackTrace();
            }
            Assertions.assertEquals(false, connection.isOpen());
        } finally {
            client.restore(token);
        }
    }


    @Test
    @Disabled
    public void testSingleAsych() throws Exception {
        callApiAsync();
    }

    public String callApiAsync() throws Exception {
        final Http2Client client = createClient();
        final CountDownLatch latch = new CountDownLatch(1);
        SimpleConnectionState.ConnectionToken token = client.borrow(ADDRESS, worker, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
        final ClientConnection connection = (ClientConnection) token.getRawConnection();
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath(API).setMethod(Methods.GET);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            request.getRequestHeaders().put(new HttpString(ClientConfig.SERVICE_ID), tokenScenario.serviceId);
            if (tokenScenario.cachedJwt == null) acquireScenarioToken();
            tokenScenario.requestThreads.add(Thread.currentThread());
            Result<?> headerResult = client.populateHeader(request, "Bearer token", "cid", "tid");
            Assertions.assertFalse(headerResult.isFailure(),
                    () -> "Failed to populate request headers: " + headerResult.getError());
            tokenScenario.cachedJwt = (Jwt) headerResult.getResult();
            request.getRequestHeaders().add(Headers.CONNECTION, Headers.CLOSE.toString());
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS), "API response timed out");
            final ClientResponse response = reference.get();
            Assertions.assertNotNull(response, "API callback returned no response");
            Assertions.assertNull(tokenScenario.failure.get(),
                    () -> "Mock server failure: " + tokenScenario.failure.get());
            Assertions.assertEquals(StatusCodes.OK, response.getResponseCode());
            Assertions.assertEquals("{\"message\":\"OK!\"}", response.getAttachment(Http2Client.RESPONSE_BODY));
            Assertions.assertEquals(false, connection.isOpen());
        } finally {
            tokenScenario.requestThreads.remove(Thread.currentThread());
            client.restore(token);
        }
        return reference.get().getAttachment(Http2Client.RESPONSE_BODY);
    }

    @Test
    public void testAsyncAboutToExpire() throws Exception {
        tokenScenario.heldAttempt = 2;
        callApiAsync();
        Jwt cached = tokenScenario.cachedJwt;
        String initialToken = cached.getJwt();
        long initialExpiry = cached.getExpire();
        tokenScenario.receivedTokens.clear();

        // Enter the refresh window via configuration, leaving a full response budget
        // on the real signed token. This does not assume a 4000ms configured window.
        Jwt.setTokenRenewBeforeExpired(initialExpiry - System.currentTimeMillis() + 1);
        try {
            callApiAsyncMultiThread(4);
            Assertions.assertTrue(tokenScenario.renewalStarted.await(requestTimeoutMillis(), TimeUnit.MILLISECONDS),
                    "Async refresh did not start");
            Assertions.assertEquals(2, tokenScenario.tokenRequests.get(), "Expected exactly one asynchronous refresh");
            Assertions.assertEquals(4, tokenScenario.receivedTokens.size());
            Assertions.assertTrue(tokenScenario.receivedTokens.stream().allMatch(initialToken::equals),
                    "Requests must use the valid cached token while refresh is in flight");
        } finally {
            tokenScenario.releaseRenewal.countDown();
        }
        awaitCondition(() -> !cached.isRenewing(), 10000, "Async refresh did not finish");
        Assertions.assertNull(tokenScenario.failure.get());
        Assertions.assertTrue(cached.getExpire() > initialExpiry, "Refresh did not extend expiry");
        Assertions.assertFalse(initialToken.equals(cached.getJwt()), "Refresh did not replace the token");
        tokenScenario.receivedTokens.clear();
        callApiAsyncMultiThread(4);
        Assertions.assertTrue(tokenScenario.receivedTokens.stream().allMatch(cached.getJwt()::equals),
                "Subsequent requests must use the refreshed token");
        Assertions.assertEquals(2, tokenScenario.tokenRequests.get(), "Fresh token should be reused");
    }

    @Test
    public void testAsyncExpired() throws Exception {
        tokenScenario.initialLifetimeSeconds = 1;
        acquireScenarioToken();
        Jwt cached = tokenScenario.cachedJwt;
        String initialToken = cached.getJwt();
        long initialExpiry = cached.getExpire();
        awaitCondition(() -> System.currentTimeMillis() >= initialExpiry,
                10000, "Token did not expire before the request batch");
        callApiAsyncMultiThread(4);
        Assertions.assertFalse(cached.isRenewing(), "Synchronous refresh must have finished");
        Assertions.assertEquals(2, tokenScenario.tokenRequests.get(), "Expected exactly one post-expiry refresh");
        Assertions.assertTrue(cached.getExpire() > initialExpiry, "Refresh did not extend expiry");
        Assertions.assertEquals(4, tokenScenario.receivedTokens.size());
        Assertions.assertTrue(tokenScenario.receivedTokens.stream().noneMatch(initialToken::equals),
                "Expired token must not reach the API");
    }

    @Test
    public void testExpiredDuringAsyncRefresh() throws Exception {
        tokenScenario.initialLifetimeSeconds = 1;
        tokenScenario.heldAttempt = 2;
        acquireScenarioToken();
        Jwt cached = tokenScenario.cachedJwt;
        long initialExpiry = cached.getExpire();
        Jwt.setTokenRenewBeforeExpired(initialExpiry - System.currentTimeMillis() + 1);
        callApiAsync();
        Assertions.assertTrue(tokenScenario.renewalStarted.await(requestTimeoutMillis(), TimeUnit.MILLISECONDS),
                "Async refresh did not start");
        awaitCondition(() -> System.currentTimeMillis() >= initialExpiry, 10000, "Initial token did not expire");
        tokenScenario.receivedTokens.clear();

        runDuringHeldTokenResponse(() -> {
            callApiAsyncMultiThread(4);
            return null;
        }, pending -> {
            awaitCondition(() -> pending.isDone() || (tokenScenario.requestThreads.size() == 4
                            && tokenScenario.requestThreads.stream().allMatch(thread -> thread.getState() == Thread.State.TIMED_WAITING)),
                    requestTimeoutMillis(), "Expired-token callers did not wait for the in-flight refresh");
            Assertions.assertFalse(pending.isDone(), "Expired-token calls completed before refresh was released");
            Assertions.assertTrue(cached.isRenewing(), "Waiters must not clear the refresh flag");
            Assertions.assertEquals(2, tokenScenario.tokenRequests.get(), "Must not start a parallel synchronous refresh");
        });
        Assertions.assertEquals(2, tokenScenario.tokenRequests.get());
        Assertions.assertEquals(4, tokenScenario.receivedTokens.size());
        Assertions.assertTrue(tokenScenario.receivedTokens.stream().allMatch(cached.getJwt()::equals));
        Assertions.assertFalse(cached.isRenewing());
    }

    @Test
    public void testSlowInitialAcquisition() throws Exception {
        tokenScenario.heldAttempt = 1;
        runDuringHeldTokenResponse(() -> {
            callApiAsyncMultiThread(4);
            return null;
        }, pending -> {
            Assertions.assertTrue(tokenScenario.renewalStarted.await(requestTimeoutMillis(), TimeUnit.MILLISECONDS),
                    "Initial acquisition did not start");
            Assertions.assertFalse(pending.isDone(), "Initial acquisition did not wait for its response");
            Assertions.assertEquals(1, tokenScenario.tokenRequests.get());
        });
        Assertions.assertEquals(1, tokenScenario.tokenRequests.get(), "Concurrent acquisition must be single flight");
        Assertions.assertEquals(4, tokenScenario.receivedTokens.size());
        Assertions.assertFalse(tokenScenario.cachedJwt.isRenewing());
    }

    @Test
    public void testFailedInitialAcquisition() {
        tokenScenario.failureAttempt = 1;
        AssertionError failure = Assertions.assertThrows(AssertionError.class, this::callApiAsync);
        Assertions.assertTrue(failure.getMessage().contains("GET_TOKEN_ERROR"), "Must report the acquisition error");
        Assertions.assertTrue(tokenScenario.receivedTokens.isEmpty(), "A failed acquisition must not reach the API");
        Assertions.assertEquals(1, tokenScenario.tokenRequests.get());
    }

    @Test
    public void testFailedAsyncRefresh() throws Exception {
        acquireScenarioToken();
        Jwt cached = tokenScenario.cachedJwt;
        String initialToken = cached.getJwt();
        long initialExpiry = cached.getExpire();
        tokenScenario.failureAttempt = 2;
        Jwt.setTokenRenewBeforeExpired(initialExpiry - System.currentTimeMillis() + 1);
        callApiAsync();
        awaitCondition(() -> tokenScenario.tokenRequests.get() == 2 && !cached.isRenewing(),
                10000, "Failed async refresh did not release its state");
        Assertions.assertTrue(initialToken.equals(cached.getJwt()), "Failure must preserve the valid token");
        Assertions.assertEquals(initialExpiry, cached.getExpire());
        Assertions.assertTrue(cached.getExpiredRetryTimeout() > System.currentTimeMillis());
        Assertions.assertTrue(cached.getEarlyRetryTimeout() > System.currentTimeMillis());
        callApiAsync();
        Assertions.assertEquals(2, tokenScenario.tokenRequests.get(), "Failed refresh must respect its retry delay");
        Assertions.assertEquals(2, tokenScenario.receivedTokens.size());
    }

    @Test
    public void testTokenCallbackWithDelayedRequestBody() throws Exception {
        String body = "grant_type=client_credentials";
        try (Socket socket = new Socket(ADDRESS.getHost(), ADDRESS.getPort())) {
            socket.setSoTimeout((int) requestTimeoutMillis());
            String headers = "POST " + tokenScenario.authServer.getUri() + " HTTP/1.1\r\n"
                    + "Host: localhost\r\nContent-Length: " + body.length() + "\r\nConnection: close\r\n\r\n";
            socket.getOutputStream().write(headers.getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            Assertions.assertTrue(tokenScenario.bodyReaderReady.await(requestTimeoutMillis(), TimeUnit.MILLISECONDS),
                    "Body receiver did not register before the body arrived");
            Assertions.assertEquals(0, tokenScenario.tokenRequests.get());
            socket.getOutputStream().write(body.getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            Assertions.assertTrue(reader.readLine().startsWith("HTTP/1.1 200"));
            Assertions.assertNull(tokenScenario.failure.get(), "Body callback must dispatch token work off the I/O thread");
            Assertions.assertEquals(1, tokenScenario.tokenRequests.get());
        }
    }

    @Test
    public void testMixed() throws Exception {
        callApiAsyncMultiThread(4);
        callApiAsyncMultiThread(4);
        Assertions.assertFalse(tokenScenario.cachedJwt.isRenewing(), "Fresh token must not trigger another refresh");
        Assertions.assertEquals(8, tokenScenario.receivedTokens.size());
        Assertions.assertTrue(tokenScenario.receivedTokens.stream().allMatch(tokenScenario.cachedJwt.getJwt()::equals),
                "Requests outside the refresh window must reuse the cached token");
        Assertions.assertEquals(1, tokenScenario.tokenRequests.get(), "Acquisition and cache reuse must make exactly one token request");
    }

    private void acquireScenarioToken() {
        Result<Jwt> result = TokenManager.getInstance().getJwt(new Jwt.Key(tokenScenario.serviceId), tokenScenario.authServer);
        Assertions.assertFalse(result.isFailure(), () -> "Failed to acquire scope token: " + result.getError());
        tokenScenario.cachedJwt = result.getResult();
    }

    private static long requestTimeoutMillis() {
        return config.getRequest().getTimeout();
    }

    private static int tokenLifetimeSeconds(long renewWindowMillis) {
        return (int) ((renewWindowMillis + 3 * requestTimeoutMillis() + 999) / 1000);
    }

    private interface HeldResponseCheck {
        void check(Future<?> pending) throws Exception;
    }

    private void runDuringHeldTokenResponse(Callable<Void> calls, HeldResponseCheck check) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Void> pending = executor.submit(calls);
        Throwable primaryFailure = null;
        try {
            check.check(pending);
            tokenScenario.releaseRenewal.countDown();
            pending.get(15, TimeUnit.SECONDS);
        } catch (Exception | Error failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            tokenScenario.releaseRenewal.countDown();
            pending.cancel(true);
            stopExecutor(executor, primaryFailure);
        }
    }

    private void callApiAsyncMultiThread(final int threadCount) throws InterruptedException, ExecutionException {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<String>> futures = new ArrayList<>();
        Throwable primaryFailure = null;
        try {
            for (int i = 0; i < threadCount; i++) {
                futures.add(executorService.submit(() -> {
                    ready.countDown();
                    Assertions.assertTrue(start.await(10, TimeUnit.SECONDS), "Concurrent request start timed out");
                    return callApiAsync();
                }));
            }
            Assertions.assertTrue(ready.await(10, TimeUnit.SECONDS), "Request workers did not become ready");
            start.countDown();
            for (Future<String> future : futures) {
                try {
                    future.get(15, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    throw new AssertionError("Concurrent API request timed out", e);
                }
            }
        } catch (InterruptedException | ExecutionException | RuntimeException | Error failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            start.countDown();
            futures.forEach(future -> future.cancel(true));
            stopExecutor(executorService, primaryFailure);
        }
    }

    private static void stopExecutor(ExecutorService executor, Throwable primaryFailure) throws InterruptedException {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Request executor did not stop");
            }
        } catch (InterruptedException | AssertionError cleanupFailure) {
            if (cleanupFailure instanceof InterruptedException) Thread.currentThread().interrupt();
            if (primaryFailure != null) primaryFailure.addSuppressed(cleanupFailure);
            else throw cleanupFailure;
        }
    }

    private static void awaitCondition(BooleanSupplier condition, long timeoutMillis, String message) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() >= deadline) Assertions.fail(message);
            Thread.sleep(10);
        }
    }

    private static void handleTokenRequest(HttpServerExchange exchange) {
        String id = exchange.getRequestPath().substring(TOKEN.length() + 1);
        TokenScenario scenario = tokenScenarios.get(id);
        if (scenario == null) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("Unknown token scenario");
            return;
        }
        exchange.getRequestReceiver().receiveFullString((request, body) -> {
            // The receiver callback can run on an I/O thread even when registered on a worker.
            Runnable response = () -> respondWithToken(request, scenario);
            if (request.isInIoThread()) request.dispatch(response);
            else response.run();
        });
        scenario.bodyReaderReady.countDown();
    }

    private static void respondWithToken(HttpServerExchange request, TokenScenario scenario) {
        scenario.activeRequests.incrementAndGet();
        try {
            Assertions.assertFalse(request.isInIoThread(), "Token processing must not block an I/O thread");
            int attempt = scenario.tokenRequests.incrementAndGet();
            if (attempt == scenario.heldAttempt) {
                scenario.renewalStarted.countDown();
                Assertions.assertTrue(scenario.releaseRenewal.await(requestTimeoutMillis(), TimeUnit.MILLISECONDS),
                        "Token response was not released");
            }
            Map<String, Object> response = new HashMap<>();
            if (attempt == scenario.failureAttempt) {
                response.put("statusCode", 401);
                response.put("code", "ERR10052");
                response.put("message", "GET_TOKEN_ERROR");
                response.put("description", "Controlled acquisition failure");
                response.put("severity", "ERROR");
            } else {
                int lifetime = attempt == 1 ? scenario.initialLifetimeSeconds : tokenLifetimeSeconds(Jwt.getTokenRenewBeforeExpired());
                response.put("access_token", getJwt(lifetime));
                response.put("token_type", "Bearer");
                response.put("expires_in", lifetime);
                response.put("scope", "api.r api.w");
            }
            request.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getResponseSender().send(ByteBuffer.wrap(Config.getInstance().getMapper().writeValueAsBytes(response)));
        } catch (Throwable failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            scenario.failure.compareAndSet(null, failure);
            request.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
            request.getResponseSender().send("Token fixture failed");
        } finally {
            scenario.activeRequests.decrementAndGet();
        }
    }

    private static KeyStore loadKeyStore(final String name) throws IOException {
        final InputStream stream = Config.getInstance().getInputStreamFromFile(name);
        if(stream == null) {
            throw new RuntimeException("Could not load keystore");
        }
        try {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(stream, STORE_PASSWORD);

            return loadedKeystore;
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new IOException(String.format("Unable to load KeyStore %s", name), e);
        } finally {
            IoUtils.safeClose(stream);
        }
    }

    private static SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore, boolean client) throws IOException {
        KeyManager[] keyManagers;
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, STORE_PASSWORD);
            keyManagers = keyManagerFactory.getKeyManagers();
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
            throw new IOException("Unable to initialise KeyManager[]", e);
        }

        TrustManager[] trustManagers = null;
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            trustManagers = trustManagerFactory.getTrustManagers();
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new IOException("Unable to initialise TrustManager[]", e);
        }

        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(keyManagers, trustManagers, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IOException("Unable to create and initialise the SSLContext", e);
        }

        return sslContext;
    }

    private static boolean isTokenExpired(String authorization) throws IOException {
        String jwt = getJwtFromAuthorization(authorization);
        Assertions.assertNotNull(jwt, "Malformed bearer token");
        return System.currentTimeMillis() >= OauthHelper.getJwtExp(jwt);
    }

    private static String getJwt(int expiredInSeconds) throws Exception {
        JwtClaims claims = getTestClaims();
        claims.setExpirationTime(NumericDate.fromMilliseconds(
                ((System.currentTimeMillis() + expiredInSeconds * 1000L + 999) / 1000) * 1000));
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
                "/config/primary.jks", "password", "selfsigned");

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
