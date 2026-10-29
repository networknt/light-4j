package com.networknt.exception;

import com.networknt.config.Config;
import com.networknt.handler.HandlerContinuation;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(20)
class ExceptionContinuationTest {
    static Stream<Arguments> cases() {
        return Stream.of("exception", "continuation-named", "continuation-disabled")
                .flatMap(name -> Stream.of(false, true).flatMap(async ->
                        Stream.of(false, true).map(runtime -> Arguments.of(name, async, runtime))));
    }

    @ParameterizedTest
    @MethodSource("cases")
    void selectedPolicyHandlesInitialAndResumedFailures(String name, boolean async, boolean runtime) throws Exception {
        boolean enabled = !name.endsWith("disabled");
        Map<String, Object> previous = Config.getInstance().getJsonMapConfig("exception");
        Config.getInstance().putInConfigCache(name, Map.of("enabled", enabled));
        ExceptionHandler selected = new ExceptionHandler(name) {
            @Override public void setExchangeStatus(HttpServerExchange exchange, String code, Object... args) {
                exchange.getResponseHeaders().put(new HttpString("X-Selected"), name);
                super.setExchangeStatus(exchange, code, args);
            }
        };
        HttpHandler failing = exchange -> {
            if (runtime) throw new IllegalStateException("runtime failure");
            throw new IOException("checked failure");
        };
        var resumedMdc = new java.util.concurrent.atomic.AtomicReference<String>();
        var resumedDone = new java.util.concurrent.CountDownLatch(1);
        HttpHandler work = exchange -> {
            if (async) {
                HttpHandler resumed = HandlerContinuation.capture(exchange, failing);
                exchange.dispatch(() -> {
                    org.slf4j.MDC.put("executor", "owns-context");
                    try {
                        HandlerContinuation.task(exchange, resumed).run();
                        resumedMdc.set(org.slf4j.MDC.get("executor"));
                    } finally {
                        org.slf4j.MDC.clear();
                        resumedDone.countDown();
                    }
                });
            } else failing.handleRequest(exchange);
        };
        selected.setNext(work);
        Undertow server = Undertow.builder().addHttpListener(0, "127.0.0.1")
                .setHandler(selected.isEnabled() ? selected : work).build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            server.start();
            int port = ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort();
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(
                    URI.create("http://127.0.0.1:" + port + "/failure")).timeout(Duration.ofSeconds(5)).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(enabled ? (runtime ? 500 : 400) : 500, response.statusCode(), response.body());
            assertEquals(enabled ? name : "", response.headers().firstValue("X-Selected").orElse(""));
            assertEquals(enabled, response.body().contains(runtime ? "ERR10010" : "ERR10011"), response.body());
            if (async) {
                assertTrue(resumedDone.await(3, java.util.concurrent.TimeUnit.SECONDS));
                assertEquals("owns-context", resumedMdc.get(), "Resumed exception handling must leave executor MDC alone");
            }
        } finally {
            server.stop();
            if (name.equals("exception") && previous != null) Config.getInstance().putInConfigCache(name, previous);
            else Config.getInstance().clearConfigCache(name);
        }
    }
}
