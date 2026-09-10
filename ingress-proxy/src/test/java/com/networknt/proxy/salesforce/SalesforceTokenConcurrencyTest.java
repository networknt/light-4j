package com.networknt.proxy.salesforce;

import com.networknt.config.Config;
import com.networknt.config.PathPrefixAuth;
import com.sun.net.httpserver.HttpServer;
import io.undertow.Undertow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SalesforceTokenConcurrencyTest {
    private HttpServer upstream;
    private Undertow gateway;
    private ExecutorService upstreamWorkers;
    private HttpClient http;
    private volatile SalesforceHandler handler;
    private SalesforceConfig config;
    private String upstreamUrl;
    private String gatewayUrl;
    private final AtomicInteger tokenRequests = new AtomicInteger();
    private final AtomicInteger apiRequests = new AtomicInteger();
    private final CountDownLatch tokenEntered = new CountDownLatch(1);
    private final CountDownLatch extraTokenEntered = new CountDownLatch(1);
    private final CountDownLatch apiEntered = new CountDownLatch(1);
    private volatile CountDownLatch releaseToken = new CountDownLatch(0);
    private volatile CountDownLatch requestsEntered = new CountDownLatch(0);
    private volatile boolean failFirstToken;
    private PausingAuth pausingAuth;

    @BeforeEach
    void startServers() throws Exception {
        http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        upstreamWorkers = Executors.newCachedThreadPool();
        upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        upstream.setExecutor(upstreamWorkers);
        upstream.createContext("/token", exchange -> {
            int request = tokenRequests.incrementAndGet();
            tokenEntered.countDown();
            if (request > 1) extraTokenEntered.countDown();
            try {
                await(releaseToken);
                exchange.getRequestBody().readAllBytes();
                boolean fail = failFirstToken && request == 1;
                byte[] body = (fail ? "{\"error\":\"unavailable\"}" :
                        "{\"access_token\":\"fresh-token-" + request + "\",\"token_type\":\"Bearer\"}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(fail ? 503 : 200, body.length);
                exchange.getResponseBody().write(body);
            } finally {
                exchange.close();
            }
        });
        upstream.createContext("/api", exchange -> {
            apiRequests.incrementAndGet();
            apiEntered.countDown();
            byte[] body = exchange.getRequestHeaders().getFirst("Authorization").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        upstream.start();
        upstreamUrl = "http://127.0.0.1:" + upstream.getAddress().getPort();

        Config.getInstance().clearConfigCache(SalesforceConfig.CONFIG_NAME);
        config = SalesforceConfig.load();
        config.getPathPrefixAuths().clear();
        config.getUrlRewriteRules().clear();
        config.metricsInjection = false;
        handler = new SalesforceHandler();
        // Use a local HTTP client so this test requires no Salesforce credentials or TLS stores.
        Field client = SalesforceHandler.class.getDeclaredField("client");
        client.setAccessible(true);
        client.set(handler, http);
        gateway = Undertow.builder().addHttpListener(0, "127.0.0.1")
                .setIoThreads(2).setWorkerThreads(16)
                .setHandler(exchange -> exchange.dispatch(() -> {
                    requestsEntered.countDown();
                    try {
                        handler.handleRequest(exchange);
                    } catch (Exception e) {
                        exchange.setStatusCode(500);
                        exchange.getResponseSender().send(e.toString());
                    }
                })).build();
        gateway.start();
        gatewayUrl = "http://127.0.0.1:" + ((InetSocketAddress) gateway.getListenerInfo().get(0).getAddress()).getPort();
    }

    @AfterEach
    void stopServers() {
        releaseToken.countDown();
        if (pausingAuth != null) pausingAuth.release.countDown();
        if (gateway != null) gateway.stop();
        if (upstream != null) upstream.stop(0);
        if (upstreamWorkers != null) upstreamWorkers.shutdownNow();
        Config.getInstance().clearConfigCache(SalesforceConfig.CONFIG_NAME);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void concurrentRequestsAcquireOneToken(boolean refreshing) throws Exception {
        PathPrefixAuth auth = auth(new PathPrefixAuth(), refreshing);
        config.getPathPrefixAuths().add(auth);
        assertSame(auth, SalesforceConfig.load().getPathPrefixAuths().get(0));
        releaseToken = new CountDownLatch(1);
        requestsEntered = new CountDownLatch(8);
        List<CompletableFuture<HttpResponse<String>>> requests = new ArrayList<>();
        for (int i = 0; i < 8; i++) requests.add(request());
        try {
            await(requestsEntered);
            await(tokenEntered);
            assertFalse(extraTokenEntered.await(300, TimeUnit.MILLISECONDS), "A refresh is already in progress");
        } finally {
            releaseToken.countDown();
        }
        for (CompletableFuture<HttpResponse<String>> request : requests) assertToken(request, "fresh-token-1");
        assertEquals(1, tokenRequests.get());
        assertEquals(8, apiRequests.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void requestsCannotObservePartiallyPublishedToken(boolean refreshing) throws Exception {
        pausingAuth = auth(new PausingAuth(), refreshing);
        pausingAuth.pause = true;
        config.getPathPrefixAuths().add(pausingAuth);
        CompletableFuture<HttpResponse<String>> first = request();
        await(pausingAuth.expirationWritten);
        requestsEntered = new CountDownLatch(1);
        CompletableFuture<HttpResponse<String>> second = request();
        try {
            await(requestsEntered);
            assertFalse(apiEntered.await(300, TimeUnit.MILLISECONDS), "Token publication must finish before API invocation");
        } finally {
            pausingAuth.release.countDown();
        }
        assertToken(first, "fresh-token-1");
        assertToken(second, "fresh-token-1");
        assertEquals(1, tokenRequests.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void anotherHandlerCanUseSharedTokenWithoutAcquiringAgain(boolean createdBeforeAcquisition) throws Exception {
        config.getPathPrefixAuths().add(auth(new PathPrefixAuth(), false));
        // Exercise normal client initialization for both handlers, without injecting either client.
        handler = new SalesforceHandler();
        SalesforceHandler other = createdBeforeAcquisition ? new SalesforceHandler() : null;
        assertToken(request(), "fresh-token-1");

        handler = other != null ? other : new SalesforceHandler();
        assertToken(request(), "fresh-token-1");
        assertEquals(1, tokenRequests.get());
        assertEquals(2, apiRequests.get());
    }

    @Test
    void failedRefreshKeepsExpiredStateAndCanBeRetried() throws Exception {
        PathPrefixAuth auth = auth(new PathPrefixAuth(), true);
        config.getPathPrefixAuths().add(auth);
        long expiration = auth.getExpiration();
        failFirstToken = true;
        assertNotEquals(200, request().get(10, TimeUnit.SECONDS).statusCode());
        assertEquals(0, apiRequests.get());
        synchronized (auth) {
            assertEquals("stale-token", auth.getAccessToken());
            assertEquals(expiration, auth.getExpiration());
        }
        assertToken(request(), "fresh-token-2");
        assertEquals(2, tokenRequests.get());
    }

    private <T extends PathPrefixAuth> T auth(T auth, boolean refreshing) {
        auth.setPathPrefix("/api");
        auth.setGrantType("password");
        auth.setUsername("test-user");
        auth.setPassword("test-password");
        auth.setClientId("test-client");
        auth.setClientSecret("test-secret");
        auth.setResponseType("token");
        auth.setTokenUrl(upstreamUrl + "/token");
        auth.setServiceHost(upstreamUrl);
        auth.setTokenTtl(60);
        auth.setWaitLength(5000);
        if (refreshing) {
            auth.setAccessToken("stale-token");
            auth.setExpiration(System.currentTimeMillis() - 1000);
        }
        return auth;
    }

    private CompletableFuture<HttpResponse<String>> request() {
        return http.sendAsync(HttpRequest.newBuilder(URI.create(gatewayUrl + "/api"))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json").GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static void assertToken(CompletableFuture<HttpResponse<String>> request, String token) throws Exception {
        HttpResponse<String> response = request.get(10, TimeUnit.SECONDS);
        assertEquals(200, response.statusCode(), response.body());
        assertEquals("Bearer " + token, response.body());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for concurrent request");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static class PausingAuth extends PathPrefixAuth {
        final CountDownLatch expirationWritten = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        volatile boolean pause;

        @Override
        public void setExpiration(long expiration) {
            super.setExpiration(expiration);
            if (pause) {
                expirationWritten.countDown();
                await(release);
            }
        }
    }
}
