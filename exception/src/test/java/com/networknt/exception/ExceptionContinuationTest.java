package com.networknt.exception;


import com.networknt.config.Config;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.networknt.handler.HandlerContinuation;
import com.networknt.status.Status;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(20)
class ExceptionContinuationTest {
    static Stream<Arguments> cases() {
        return Stream.of("exception", "continuation-named", "continuation-disabled")
                .flatMap(name -> Stream.of(false, true).flatMap(async ->
                        Stream.of(false, true).flatMap(runtime -> Stream.of(false, true)
                                .map(mdc -> Arguments.of(name, async, runtime, mdc)))));
    }

    @ParameterizedTest
    @MethodSource("cases")
    void selectedPolicyHandlesInitialAndResumedFailures(String name, boolean async, boolean runtime, boolean hasMdc) throws Exception {
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
            if (async) {
                assertEquals(hasMdc ? "canonical" : null, MDC.get("request"));
                assertNull(MDC.get("executor"));
            }
            if (runtime) throw new IllegalStateException("runtime failure");
            throw new IOException("checked failure");
        };
        var resumedMdc = new AtomicReference<String>();
        var resumedDone = new CountDownLatch(1);
        HttpHandler work = exchange -> {
            if (hasMdc) selected.addHandlerMDCContext(exchange, "request", "canonical");
            if (async) {
                HttpHandler resumed = HandlerContinuation.capture(exchange, failing);
                exchange.dispatch(() -> {
                    MDC.put("executor", "owns-context");
                    try {
                        HandlerContinuation.task(exchange, resumed).run();
                        resumedMdc.set(MDC.get("executor"));
                    } finally {
                        MDC.clear();
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
                assertTrue(resumedDone.await(3, TimeUnit.SECONDS));
                assertEquals("owns-context", resumedMdc.get(), "Resumed exception handling must leave executor MDC alone");
            }
        } finally {
            server.stop();
            if (name.equals("exception") && previous != null) Config.getInstance().putInConfigCache(name, previous);
            else Config.getInstance().clearConfigCache(name);
        }
    }
    static Stream<Arguments> typedFailureCases() {
        return Stream.of("framework", "api", "client", "client-without-status")
                .flatMap(kind -> Stream.of(false, true).map(resumed -> Arguments.of(kind, resumed)));
    }

    @ParameterizedTest
    @MethodSource("typedFailureCases")
    void typedFailuresRetainResponseStatusAndBody(String kind, boolean resumed) throws Exception {
        Status previousClientStatus = ClientException.getStatus();
        Status status = new Status(429, "TEST_TYPED_FAILURE", "Typed failure", "Preserve the supplied status");
        Exception failure = switch (kind) {
            case "framework" -> new FrameworkException(status);
            case "api" -> new ApiException(status);
            case "client" -> new ClientException(status);
            case "client-without-status" -> new ClientException(new Status());
            default -> throw new IllegalArgumentException(kind);
        };
        var events = new ListAppender<ILoggingEvent>();
        var log = (Logger) ExceptionHandler.logger;
        events.start();
        log.addAppender(events);
        ExceptionHandler selected = new ExceptionHandler();
        HttpHandler failing = exchange -> { throw failure; };
        selected.setNext(exchange -> {
            if (resumed) exchange.dispatch(HandlerContinuation.capture(exchange, failing));
            else failing.handleRequest(exchange);
        });
        Undertow server = Undertow.builder().addHttpListener(0, "127.0.0.1").setHandler(selected).build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            server.start();
            int port = ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort();
            var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port))
                    .timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
            boolean missingStatus = kind.equals("client-without-status");
            assertEquals(missingStatus ? 400 : 429, response.statusCode(), response.body());
            assertTrue(response.body().contains(missingStatus ? "ERR10011" : "TEST_TYPED_FAILURE"), response.body());
            assertEquals("application/json", response.headers().firstValue("Content-Type").orElse(""));
            if (kind.equals("framework") || kind.equals("api"))
                assertTrue(events.list.stream().anyMatch(event -> status.toString().equals(event.getMessage())
                        && event.getThrowableProxy() != null
                        && failure.getClass().getName().equals(event.getThrowableProxy().getClassName())),
                        "Keep the existing raw JSON log message and throwable");
        } finally {
            server.stop();
            ClientException.setStatus(previousClientStatus);
            log.detachAppender(events);
            events.stop();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void committedResponseIsNotTruncatedByInitialOrResumedBoundary(boolean resumed) throws Exception {
        var release = new CountDownLatch(1);
        ExceptionHandler selected = new ExceptionHandler();
        HttpHandler streaming = exchange -> {
            exchange.startBlocking();
            var output = exchange.getOutputStream();
            output.write("first".getBytes(StandardCharsets.UTF_8));
            output.flush();
            exchange.dispatch(() -> {
                try {
                    assertTrue(release.await(5, TimeUnit.SECONDS));
                    output.write("second".getBytes(StandardCharsets.UTF_8));
                    output.close();
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            throw new IOException("failure after sending response headers");
        };
        selected.setNext(exchange -> {
            if (resumed) exchange.dispatch(HandlerContinuation.capture(exchange, streaming));
            else streaming.handleRequest(exchange);
        });
        Undertow server = Undertow.builder().addHttpListener(0, "127.0.0.1").setHandler(selected).build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            server.start();
            int port = ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort();
            var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port))
                    .timeout(Duration.ofSeconds(8)).build(), HttpResponse.BodyHandlers.ofInputStream());
            assertEquals(200, response.statusCode());
            try (var body = response.body()) {
                assertEquals("first", new String(body.readNBytes(5), StandardCharsets.UTF_8));
                release.countDown();
                assertEquals("second", new String(body.readAllBytes(), StandardCharsets.UTF_8));
            }
        } finally {
            release.countDown();
            server.stop();
        }
    }

    @Test
    void lateResumedFailureDoesNotRewriteCompletedResponseOrCloseKeepAliveConnection() throws Exception {
        var saved = new AtomicReference<HttpHandler>();
        var completed = new AtomicReference<HttpServerExchange>();
        var done = new CountDownLatch(1);
        ExceptionHandler selected = new ExceptionHandler();
        selected.setNext(exchange -> {
            saved.set(HandlerContinuation.capture(exchange, ex -> { throw new IOException("late failure"); }));
            exchange.addExchangeCompleteListener((ex, next) -> {
                completed.set(ex);
                done.countDown();
                next.proceed();
            });
            exchange.getResponseSender().send("accepted");
        });
        Undertow server = Undertow.builder().addHttpListener(0, "127.0.0.1").setHandler(selected).build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            server.start();
            int port = ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort();
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port)).timeout(Duration.ofSeconds(5)).build();
            assertEquals("accepted", client.send(request, HttpResponse.BodyHandlers.ofString()).body());
            assertTrue(done.await(3, TimeUnit.SECONDS));
            assertTrue(completed.get().isComplete());
            saved.get().handleRequest(completed.get());
            assertTrue(completed.get().getConnection().isOpen());
            assertEquals("accepted", client.send(request, HttpResponse.BodyHandlers.ofString()).body());
        } finally { server.stop(); }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void installedBoundaryRetainsExistingFatalErrorMappingOnHttp11(boolean resumed) throws Exception {
        var calls = new AtomicInteger();
        Error sentinel = new StackOverflowError("installed boundary sentinel");
        ExceptionHandler selected = new ExceptionHandler();
        HttpHandler failing = exchange -> { calls.incrementAndGet(); throw sentinel; };
        selected.setNext(exchange -> {
            if (exchange.getRequestPath().equals("/after")) exchange.getResponseSender().send("accepted");
            else if (resumed) exchange.dispatch(HandlerContinuation.capture(exchange, failing));
            else failing.handleRequest(exchange);
        });
        Undertow server = Undertow.builder().addHttpListener(0, "127.0.0.1").setHandler(selected).build();
        try (HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
            server.start();
            int port = ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort();
            URI endpoint = URI.create("http://127.0.0.1:" + port);
            var response = client.send(HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(5)).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(400, response.statusCode(), response.body());
            assertTrue(response.body().contains("ERR10011"), response.body());
            assertEquals(1, calls.get());
            assertEquals("accepted", client.send(HttpRequest.newBuilder(endpoint.resolve("/after")).build(),
                    HttpResponse.BodyHandlers.ofString()).body());
        } finally { server.stop(); }
    }

}
