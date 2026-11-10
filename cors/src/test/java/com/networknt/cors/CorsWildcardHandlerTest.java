package com.networknt.cors;

import com.networknt.config.Config;
import io.undertow.Undertow;
import io.undertow.util.Headers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CorsWildcardHandlerTest {
    private static final String NAME = "wildcard-handler-cors";
    private static final AtomicInteger downstreamCalls = new AtomicInteger();
    private static Undertow server;
    private static URI base;
    private static HttpClient client;

    @BeforeAll
    static void start() {
        Map<String, Object> paths = new LinkedHashMap<>();
        paths.put("/private", Map.of("allowedOrigins", List.of("https://trusted.example.com"), "allowedMethods", List.of("GET")));
        paths.put("/private/admin", Map.of("allowedOrigins", List.of("https://admin.example.com"), "allowedMethods", List.of("POST")));
        paths.put("/empty", Map.of());
        Config.getInstance().putInConfigCache(NAME, Map.of("allowedOrigins", List.of("https://*.example.com", "https://example.com"),
                "allowedMethods", List.of("GET", "PUT"), "pathPrefixAllowed", paths));
        CorsHttpHandler cors = new CorsHttpHandler(NAME);
        cors.setNext(exchange -> {
            downstreamCalls.incrementAndGet();
            exchange.getResponseSender().send("OK");
        });
        server = Undertow.builder().addHttpListener(0, "127.0.0.1").setIoThreads(2).setWorkerThreads(8)
                .setHandler(exchange -> {
                    exchange.getResponseHeaders().put(Headers.VARY, "Accept-Encoding");
                    cors.handleRequest(exchange);
                }).build();
        server.start();
        int port = ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort();
        base = URI.create("http://127.0.0.1:" + port);
        client = HttpClient.newHttpClient();
    }

    @AfterAll
    static void stop() {
        if (client != null) client.close();
        if (server != null) server.stop();
        Config.getInstance().clearConfigCache(NAME);
    }

    private static HttpRequest request(String path, String method, String... origins) {
        HttpRequest.Builder request = HttpRequest.newBuilder(base.resolve(path)).method(method, HttpRequest.BodyPublishers.noBody());
        for (String origin : origins) request.header("Origin", origin);
        if (method.equals("OPTIONS")) {
            request.header("Access-Control-Request-Method", "PUT").header("Access-Control-Request-Headers", "X-Test");
        }
        return request.build();
    }

    private static HttpResponse<String> send(String path, String method, String... origins) throws Exception {
        return client.send(request(path, method, origins), HttpResponse.BodyHandlers.ofString());
    }

    private static void assertVary(HttpResponse<?> response) {
        String vary = String.join(",", response.headers().allValues("Vary"));
        assertTrue(vary.contains("Accept-Encoding"), vary);
        assertTrue(vary.contains("Origin"), vary);
    }

    @Test
    void echoesExactlyOneValidatedOriginForActualAndPreflightRequests() throws Exception {
        for (String method : List.of("GET", "OPTIONS")) {
            for (String origin : List.of("https://app.example.com", "https://team.app.example.com:443", "https://example.com")) {
                HttpResponse<String> response = send("/public", method, origin);
                assertEquals(200, response.statusCode());
                assertEquals(List.of(origin), response.headers().allValues("Access-Control-Allow-Origin"));
                assertEquals("true", response.headers().firstValue("Access-Control-Allow-Credentials").orElseThrow());
                assertVary(response);
                if (method.equals("OPTIONS")) {
                    assertTrue(String.join(",", response.headers().allValues("Access-Control-Allow-Headers")).contains("X-Test"));
                }
            }
        }
    }

    @Test
    void rejectsMalformedUntrustedAndDuplicateOriginsWithoutDispatch() throws Exception {
        int before = downstreamCalls.get();
        for (String method : List.of("GET", "OPTIONS")) {
            for (String origin : List.of("https://badexample.com", "https://example.com.attacker.net",
                    "http://app.example.com", "https://app.example.com:8443", "null", "https://app.example.com/",
                    "https://user@app.example.com", "https://app.example.com.", "https://app.example.com:65536",
                    "https://app.example.com, https://evil.net")) {
                HttpResponse<String> response = send("/public", method, origin);
                assertEquals(403, response.statusCode(), origin);
                assertTrue(response.headers().allValues("Access-Control-Allow-Origin").isEmpty());
                assertTrue(response.headers().allValues("Access-Control-Allow-Credentials").isEmpty());
                assertVary(response);
            }
            assertEquals(403, send("/public", method, "https://app.example.com", "https://evil.net").statusCode());
            assertEquals(403, send("/public", method, "https://app.example.com", "https://app.example.com").statusCode());
        }
        assertEquals(before, downstreamCalls.get());
    }

    @Test
    void preservesPathSelectionAndServerOriginFallback() throws Exception {
        for (String path : List.of("/private", "/private/admin", "/private-other")) {
            assertEquals(403, send(path, "GET", "https://arbitrary.example.com").statusCode());
            assertEquals(200, send(path, "GET", "https://trusted.example.com").statusCode());
        }
        assertEquals(200, send("/unmatched", "GET", "https://arbitrary.example.com").statusCode());
        assertEquals(403, send("/empty", "GET", "https://arbitrary.example.com").statusCode());
        // Existing server-origin fallback remains available after an explicit allowlist miss or an empty list.
        assertEquals(200, send("/private", "GET", base.toString()).statusCode());
        assertEquals(200, send("/empty", "GET", base.toString()).statusCode());
        HttpResponse<String> noOrigin = send("/public", "GET");
        assertEquals(200, noOrigin.statusCode());
        assertTrue(noOrigin.headers().allValues("Access-Control-Allow-Origin").isEmpty());
        assertVary(noOrigin);
        for (String method : List.of("GET", "POST", "OPTIONS")) {
            HttpRequest headersOnly = HttpRequest.newBuilder(base.resolve("/public"))
                    .method(method, HttpRequest.BodyPublishers.noBody())
                    .header("Access-Control-Request-Method", "PUT")
                    .header("Access-Control-Request-Headers", "X-Test").build();
            HttpResponse<String> response = client.send(headersOnly, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("OK", response.body());
            assertTrue(response.headers().allValues("Access-Control-Allow-Origin").isEmpty());
            assertVary(response);
        }
    }

    @Test
    void concurrentAllowedDeniedAndPreflightRequestsKeepIndependentDecisions() {
        int before = downstreamCalls.get();
        List<CompletableFuture<HttpResponse<String>>> pending = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            pending.add(client.sendAsync(request("/public", i % 3 == 2 ? "OPTIONS" : "GET",
                    i % 3 == 1 ? "https://evil.net" : "https://app.example.com"), HttpResponse.BodyHandlers.ofString()));
        }
        CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new)).join();
        for (int i = 0; i < pending.size(); i++) {
            HttpResponse<String> response = pending.get(i).join();
            assertEquals(i % 3 == 1 ? 403 : 200, response.statusCode());
            if (i % 3 == 0) assertEquals("OK", response.body());
        }
        assertEquals(before + 40, downstreamCalls.get());
    }
}
