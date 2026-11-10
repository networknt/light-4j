package com.networknt.cors;

import com.networknt.config.Config;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorsRequestIsolationTest {
    @Test
    void rejectionCannotChangeAnAlreadyMatchedRequestsDispatchDecision() throws Exception {
        String name = "cors-request-isolation";
        Config.getInstance().putInConfigCache(name, Map.of("allowedOrigins", List.of("https://app.example.com"),
                "allowedMethods", List.of("GET")));
        CountDownLatch accepted = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        CompletableFuture<Void> pending = null;
        try {
            CorsHttpHandler handler = new CorsHttpHandler(name);
            AtomicInteger dispatched = new AtomicInteger();
            handler.setNext(exchange -> dispatched.incrementAndGet());
            HeaderMap response = new HeaderMap();
            HttpServerExchange allowed = spy(exchange("https://app.example.com", response));
            AtomicBoolean paused = new AtomicBoolean();
            doAnswer(invocation -> {
                // Pause after origin acceptance, before the handler makes its downstream decision.
                if (response.contains(new HttpString(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                        && paused.compareAndSet(false, true)) {
                    accepted.countDown();
                    assertTrue(resume.await(10, TimeUnit.SECONDS), "Timed out waiting to resume accepted request");
                }
                return response;
            }).when(allowed).getResponseHeaders();
            pending = CompletableFuture.runAsync(() -> {
                try { handler.handleRequest(allowed); }
                catch (Exception e) { throw new RuntimeException(e); }
            });
            assertTrue(accepted.await(10, TimeUnit.SECONDS), "Accepted request did not reach the interleaving point");
            HttpServerExchange denied = exchange("https://evil.net", new HeaderMap());
            handler.handleRequest(denied);
            assertEquals(403, denied.getStatusCode());
            assertEquals(0, dispatched.get());
            resume.countDown();
            pending.get(10, TimeUnit.SECONDS);
            assertEquals(1, dispatched.get(), "An unrelated rejection must not suppress an accepted request");
        } finally {
            resume.countDown();
            if (pending != null) pending.get(10, TimeUnit.SECONDS);
            Config.getInstance().clearConfigCache(name);
        }
    }

    private static HttpServerExchange exchange(String origin, HeaderMap response) {
        HeaderMap request = new HeaderMap();
        request.put(Headers.HOST, "localhost");
        request.put(Headers.ORIGIN, origin);
        return new HttpServerExchange(null, request, response, -1)
                .setRequestScheme("http").setRequestMethod(Methods.GET).setRequestPath("/");
    }
}
