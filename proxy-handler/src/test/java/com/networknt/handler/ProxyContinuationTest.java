package com.networknt.handler;


import com.networknt.httpstring.AttachmentConstants;
import com.networknt.metrics.AbstractMetricsHandler;
import com.networknt.metrics.MetricsConfig;
import com.networknt.metrics.TimeSeriesDbSender;
import io.undertow.Undertow;
import io.undertow.attribute.ExchangeAttribute;
import io.undertow.util.HttpString;
import io.undertow.util.HeaderMap;
import io.undertow.util.Methods;
import io.undertow.UndertowOptions;
import io.undertow.client.*;
import io.undertow.server.*;
import io.undertow.server.handlers.proxy.*;
import io.undertow.util.Headers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.xnio.channels.Channels;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(20)
class ProxyContinuationTest {
    enum Phase { ACQUIRE, RETRY, SEND_CALLBACK, RESPONSE_CALLBACK, TIMEOUT, BEFORE_SEND }
    private static final class ControlledFailure extends RuntimeException {
        final Phase phase;
        ControlledFailure(Phase phase) { super("Injected failure at " + phase); this.phase = phase; }
    }

    static Stream<Arguments> failureCases() {
        return Stream.of(Phase.values()).flatMap(phase -> Stream.of(false, true)
                .flatMap(enabled -> Stream.of(false, true).map(mdc -> Arguments.of(phase, enabled, mdc))));
    }

    @ParameterizedTest
    @MethodSource("failureCases")
    void asynchronousFailuresUseSelectedBoundaryAndCloseUpstream(Phase phase, boolean enabled, boolean hasMdc) throws Exception {
        var previousMdc = MDC.getCopyOfContextMap();
        AtomicInteger injected = new AtomicInteger();
        AtomicInteger selected = new AtomicInteger();
        ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);
        AtomicInteger maxDepth = new AtomicInteger();
        AtomicInteger acquisitions = new AtomicInteger();
        var upstream = new AtomicReference<ClientConnection>();
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Runnable> retry = new CompletableFuture<>();
        Supplier<ControlledFailure> failure = () -> {
            assertEquals(hasMdc ? "canonical-request" : null, MDC.get("request"),
                    "Callbacks must install canonical request MDC for this frame");
            injected.incrementAndGet();
            return new ControlledFailure(phase);
        };
        try (Fixture fixture = new Fixture(); HttpClient http = HttpClient.newHttpClient()) {
            URI backend = fixture.start(exchange -> exchange.dispatch(() -> {
                try {
                    if (phase == Phase.TIMEOUT && !release.await(5, TimeUnit.SECONDS)) return;
                    exchange.getResponseSender().send("accepted");
                } catch (InterruptedException _) { Thread.currentThread().interrupt(); }
            }));
            LoadBalancingProxyClient real = new LoadBalancingProxyClient().addHost(backend);
            ProxyClient client = new ProxyClient() {
                @Override public ProxyTarget findTarget(HttpServerExchange exchange) { return real.findTarget(exchange); }
                @Override public void getConnection(ProxyTarget target, HttpServerExchange exchange,
                        ProxyCallback<ProxyConnection> callback, long timeout, TimeUnit unit) {
                    int attempt = acquisitions.incrementAndGet();
                    if (phase == Phase.ACQUIRE || (phase == Phase.RETRY && attempt > 1)) throw failure.get();
                    if (phase == Phase.RETRY) {
                        retry.complete(() -> callback.failed(exchange));
                        return;
                    }
                    real.getConnection(target, exchange, new ProxyCallback<>() {
                        @Override public void completed(HttpServerExchange ex, ProxyConnection connection) {
                            upstream.set(connection.getConnection());
                            callback.completed(ex, new ProxyConnection(decorateConnection(connection.getConnection(), phase, failure),
                                    connection.getTargetPath()));
                        }
                        @Override public void failed(HttpServerExchange ex) { callback.failed(ex); }
                        @Override public void couldNotResolveBackend(HttpServerExchange ex) { callback.couldNotResolveBackend(ex); }
                        @Override public void queuedRequestFailed(HttpServerExchange ex) { callback.queuedRequestFailed(ex); }
                    }, timeout, unit);
                }
            };
            ProxyHandler proxy = ProxyHandler.builder().setProxyClient(client)
                    .setMaxConnectionRetries(phase == Phase.RETRY ? 1 : 0)
                    .setMaxRequestTime(phase == Phase.TIMEOUT ? 300 : 0).build();
            HttpHandler work = exchange -> {
                if (hasMdc) RequestMdcContext.initialize(exchange).putField("request", "canonical-request");
                if (phase == Phase.TIMEOUT) {
                    exchange.putAttachment(AttachmentConstants.METRICS_HANDLER, new ThrowingMetrics(failure));
                    exchange.putAttachment(AttachmentConstants.DOWNSTREAM_METRICS_NAME, "test");
                    exchange.putAttachment(AttachmentConstants.DOWNSTREAM_METRICS_START, System.nanoTime());
                }
                proxy.handleRequest(exchange);
            };
            URI front = fixture.start(enabled ? exchange -> HandlerContinuation.execute(exchange, (ex, task) -> {
                depth.set(depth.get() + 1);
                maxDepth.accumulateAndGet(depth.get(), Math::max);
                try { task.handleRequest(ex); }
                catch (ControlledFailure e) {
                    assertEquals(phase, e.phase);
                    selected.incrementAndGet();
                    ex.setStatusCode(409).getResponseSender().send("selected");
                } finally { if (depth.get() == 1) depth.remove(); else depth.set(depth.get() - 1); }
            }, work) : work);
            var pending = http.sendAsync(HttpRequest.newBuilder(front).timeout(Duration.ofSeconds(8)).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (phase == Phase.RETRY) retry.get(5, TimeUnit.SECONDS).run();
            var response = pending.get(10, TimeUnit.SECONDS);
            assertEquals(enabled ? 409 : 500, response.statusCode(), response.body());
            assertEquals(1, injected.get(), "The intentional failure must execute exactly once");
            assertEquals(enabled ? 1 : 0, selected.get(), "Do not accept an incidental exception");
            if (enabled) assertEquals(1, maxDepth.get(), "Internal calls must not nest boundary frames");
            assertEquals(phase == Phase.RETRY ? 2 : 1, acquisitions.get());
            if (phase != Phase.ACQUIRE && phase != Phase.RETRY)
                assertFalse(upstream.get().isOpen(), "Close upstream resources on callback failure");
            release.countDown();
        } finally {
            release.countDown();
            if (previousMdc == null) MDC.clear();
            else MDC.setContextMap(previousMdc);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void successfulProxyActionRemainsPendingUntilUpstreamCompletes(boolean enabled) throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch complete = new CountDownLatch(1);
        try (Fixture fixture = new Fixture(); HttpClient http = HttpClient.newHttpClient()) {
            URI backend = fixture.start(exchange -> exchange.dispatch(() -> {
                try {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/event-stream");
                    var channel = exchange.getResponseChannel();
                    Channels.writeBlocking(channel, ByteBuffer.wrap("first".getBytes(StandardCharsets.UTF_8)));
                    Channels.flushBlocking(channel);
                    assertTrue(release.await(5, TimeUnit.SECONDS));
                    Channels.writeBlocking(channel, ByteBuffer.wrap("second".getBytes(StandardCharsets.UTF_8)));
                    channel.shutdownWrites();
                    Channels.flushBlocking(channel);
                } catch (Exception e) { throw new RuntimeException(e); }
            }));
            ProxyHandler proxy = ProxyHandler.builder().setProxyClient(new LoadBalancingProxyClient().addHost(backend))
                    .setMaxRequestTime(0).build();
            HttpHandler work = exchange -> {
                exchange.addExchangeCompleteListener((ex, next) -> { complete.countDown(); next.proceed(); });
                proxy.handleRequest(exchange);
            };
            URI front = fixture.start(enabled ? exchange -> HandlerContinuation.execute(exchange,
                    (ex, task) -> task.handleRequest(ex), work) : work);
            var response = http.send(HttpRequest.newBuilder(front).timeout(Duration.ofSeconds(8)).build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            assertEquals(200, response.statusCode());
            try (var body = response.body()) {
                assertEquals("first", new String(body.readNBytes(5), StandardCharsets.UTF_8));
                assertEquals(1, complete.getCount(), "A successful action must leave streaming I/O pending");
                release.countDown();
                assertEquals("second", new String(body.readAllBytes(), StandardCharsets.UTF_8));
            }
            assertTrue(complete.await(3, TimeUnit.SECONDS));
        } finally { release.countDown(); }
    }

    @Test
    void unhandledTaskFailureAfterHeadersClosesStreamWithoutAppendingAnError() throws Exception {
        try (Fixture fixture = new Fixture()) {
            URI front = fixture.start(exchange -> exchange.dispatch(() -> {
                try {
                    var channel = exchange.getResponseChannel();
                    Channels.writeBlocking(channel, ByteBuffer.wrap("first".getBytes(StandardCharsets.UTF_8)));
                    Channels.flushBlocking(channel);
                    assertTrue(exchange.isResponseStarted());
                    HandlerContinuation.task(exchange, ex -> { throw new ControlledFailure(Phase.RESPONSE_CALLBACK); }).run();
                } catch (IOException e) { throw new RuntimeException(e); }
            }));
            try (Socket socket = new Socket(front.getHost(), front.getPort())) {
                socket.setSoTimeout(5000);
                socket.getOutputStream().write("GET / HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"
                        .getBytes(StandardCharsets.UTF_8));
                String raw = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(raw.startsWith("HTTP/1.1 200"), raw);
                assertTrue(raw.contains("first"), raw);
                assertFalse(raw.contains("500"), raw);
                assertFalse(raw.endsWith("0\r\n\r\n"), "Do not mark a failed stream as successfully complete");
            }
        }
    }

    @Test
    void lateFailureDoesNotCloseACompletedKeepAliveConnection() throws Exception {
        var completed = new AtomicReference<HttpServerExchange>();
        CountDownLatch done = new CountDownLatch(1);
        try (Fixture fixture = new Fixture(); HttpClient http = HttpClient.newHttpClient()) {
            URI front = fixture.start(exchange -> {
                exchange.addExchangeCompleteListener((ex, next) -> {
                    completed.set(ex);
                    done.countDown();
                    next.proceed();
                });
                exchange.getResponseSender().send("accepted");
            });
            var request = HttpRequest.newBuilder(front).timeout(Duration.ofSeconds(5)).build();
            assertEquals("accepted", http.send(request, HttpResponse.BodyHandlers.ofString()).body());
            assertTrue(done.await(3, TimeUnit.SECONDS));
            assertTrue(completed.get().isComplete());
            assertTrue(completed.get().getConnection().isOpen());
            HandlerContinuation.task(completed.get(), ex -> {
                throw new ControlledFailure(Phase.RESPONSE_CALLBACK);
            }).run();
            assertTrue(completed.get().getConnection().isOpen(), "A late callback must not close a reusable connection");
            assertEquals("accepted", http.send(request, HttpResponse.BodyHandlers.ofString()).body());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"response", "timeout", "before-send", "late"})
    void upstreamHttp2FailureResetsOnlyTheFailedStream(String mode) throws Exception {
        CompletableFuture<Runnable> late = new CompletableFuture<>();
        AtomicInteger failedAcquisitions = new AtomicInteger();
        CountDownLatch healthyEntered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Set<Integer> backendPeers = ConcurrentHashMap.newKeySet();
        Set<Integer> frontendPeers = ConcurrentHashMap.newKeySet();
        var shared = new AtomicReference<ClientConnection>();
        AtomicInteger injected = new AtomicInteger();
        try (Fixture fixture = new Fixture(); HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build()) {
            URI backend = fixture.start(exchange -> {
                assertEquals("HTTP/2.0", exchange.getProtocol().toString());
                backendPeers.add(exchange.getSourceAddress().getPort());
                if (exchange.getRequestPath().equals("/healthy")
                        || (mode.equals("timeout") && exchange.getRequestPath().equals("/fail"))) exchange.dispatch(() -> {
                    if (exchange.getRequestPath().equals("/healthy")) healthyEntered.countDown();
                    try {
                        assertTrue(release.await(8, TimeUnit.SECONDS));
                        exchange.getResponseSender().send("healthy");
                    } catch (InterruptedException _) { Thread.currentThread().interrupt(); }
                });
                else exchange.getResponseSender().send("accepted");
            }, true);
            URI upstream = URI.create("h2c-prior://" + backend.getHost() + ":" + backend.getPort());
            ProxyClient client = new ProxyClient() {
                @Override public ProxyTarget findTarget(HttpServerExchange exchange) { return new ProxyTarget() {}; }
                @Override public void getConnection(ProxyTarget target, HttpServerExchange exchange,
                        ProxyCallback<ProxyConnection> callback, long timeout, TimeUnit unit) {
                    if (exchange.getRequestPath().equals("/fail")) failedAcquisitions.incrementAndGet();
                    Consumer<ClientConnection> connected = connection -> {
                        ClientConnection selected = connection;
                        if (exchange.getRequestPath().equals("/fail") && !mode.equals("timeout")) selected = decorateConnection(connection,
                                mode.equals("before-send") || mode.equals("late") ? Phase.BEFORE_SEND : Phase.RESPONSE_CALLBACK, () -> {
                                    injected.incrementAndGet();
                                    return new ControlledFailure(Phase.RESPONSE_CALLBACK);
                                });
                        callback.completed(exchange, new ProxyConnection(selected, "/"));
                    };
                    if (mode.equals("late") && exchange.getRequestPath().equals("/fail")) {
                        late.complete(() -> {
                            connected.accept(shared.get());
                            callback.failed(exchange);
                            callback.queuedRequestFailed(exchange);
                        });
                        return;
                    }
                    if (shared.get() != null) connected.accept(shared.get());
                    else UndertowClient.getInstance().connect(new ClientCallback<>() {
                        @Override public void completed(ClientConnection connection) {
                            shared.set(connection);
                            connected.accept(connection);
                        }
                        @Override public void failed(IOException e) { callback.failed(exchange); }
                    }, upstream, exchange.getIoThread(), exchange.getConnection().getByteBufferPool(), org.xnio.OptionMap.EMPTY);
                }
            };
            ProxyHandler proxy = ProxyHandler.builder().setProxyClient(client).setMaxRequestTime(0)
                    .setPathPrefixMaxRequestTime(Map.of("/fail", mode.equals("timeout") || mode.equals("late") ? 300 : 0)).build();
            URI front = fixture.start(exchange -> {
                frontendPeers.add(exchange.getSourceAddress().getPort());
                HandlerContinuation.execute(exchange, (ex, task) -> {
                    try { task.handleRequest(ex); }
                    catch (ControlledFailure _) { ex.setStatusCode(409).getResponseSender().send("selected"); }
                }, proxy);
            }, true);
            var warm = http.send(HttpRequest.newBuilder(front.resolve("/warm")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(HttpClient.Version.HTTP_2, warm.version());
            assertEquals("accepted", warm.body());
            assertTrue(shared.get().isMultiplexingSupported());
            var healthy = http.sendAsync(HttpRequest.newBuilder(front.resolve("/healthy")).timeout(Duration.ofSeconds(10)).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertTrue(healthyEntered.await(5, TimeUnit.SECONDS));
            var failed = http.send(HttpRequest.newBuilder(front.resolve("/fail")).timeout(Duration.ofSeconds(5)).build(),
                    HttpResponse.BodyHandlers.ofString());
            boolean timeout = mode.equals("timeout") || mode.equals("late");
            assertEquals(timeout ? 504 : 409, failed.statusCode(), failed.body());
            if (mode.equals("late")) late.get(3, TimeUnit.SECONDS).run();
            assertEquals(timeout ? 0 : 1, injected.get());
            assertEquals(1, failedAcquisitions.get(), "Late failure callbacks must not retry");
            assertTrue(shared.get().isOpen(), "Keep the shared upstream HTTP/2 connection open");
            assertFalse(healthy.isDone(), "Do not cancel the healthy sibling stream");
            release.countDown();
            assertEquals("healthy", healthy.get(5, TimeUnit.SECONDS).body());
            assertEquals("accepted", http.send(HttpRequest.newBuilder(front.resolve("/after")).build(),
                    HttpResponse.BodyHandlers.ofString()).body());
            assertEquals(1, backendPeers.size(), "All upstream requests must share one physical connection");
            assertEquals(1, frontendPeers.size(), "All downstream requests must share one physical connection");
        } finally {
            release.countDown();
            if (shared.get() != null) shared.get().close();
        }
    }

    @ParameterizedTest
    @CsvSource({"false,true", "true,true", "true,false"})
    void downstreamHttp2FailureDoesNotCancelSiblingStream(boolean fatal, boolean committed) throws Exception {
        CompletableFuture<Error> propagated = new CompletableFuture<>();
        Error sentinel = new StackOverflowError("injected fatal error");
        CountDownLatch healthyEntered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Set<Integer> peers = ConcurrentHashMap.newKeySet();
        try (Fixture fixture = new Fixture(); HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build()) {
            URI front = fixture.start(exchange -> {
                peers.add(exchange.getSourceAddress().getPort());
                if (exchange.getRequestPath().equals("/healthy")) exchange.dispatch(() -> {
                    healthyEntered.countDown();
                    try {
                        assertTrue(release.await(8, TimeUnit.SECONDS));
                        exchange.getResponseSender().send("healthy");
                    } catch (InterruptedException _) { Thread.currentThread().interrupt(); }
                });
                else if (exchange.getRequestPath().equals("/fail")) exchange.dispatch(() -> {
                    try {
                        if (committed) {
                            var channel = exchange.getResponseChannel();
                            Channels.writeBlocking(channel, ByteBuffer.wrap("first".getBytes(StandardCharsets.UTF_8)));
                            Channels.flushBlocking(channel);
                        }
                        assertEquals(committed, exchange.isResponseStarted());
                        try {
                            HandlerContinuation.task(exchange, ex -> {
                                if (fatal) throw sentinel;
                                throw new ControlledFailure(Phase.RESPONSE_CALLBACK);
                            }).run();
                        } catch (Error error) { propagated.complete(error); }
                    } catch (IOException e) { throw new RuntimeException(e); }
                });
                else exchange.getResponseSender().send("accepted");
            }, true);
            assertEquals(HttpClient.Version.HTTP_2, http.send(HttpRequest.newBuilder(front.resolve("/warm")).build(),
                    HttpResponse.BodyHandlers.ofString()).version());
            var healthy = http.sendAsync(HttpRequest.newBuilder(front.resolve("/healthy")).timeout(Duration.ofSeconds(10)).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertTrue(healthyEntered.await(5, TimeUnit.SECONDS));
            if (fatal && !committed) {
                var failed = http.send(HttpRequest.newBuilder(front.resolve("/fail")).timeout(Duration.ofSeconds(5)).build(),
                        HttpResponse.BodyHandlers.ofString());
                assertEquals(500, failed.statusCode());
            } else assertThrows(IOException.class, () -> http.send(HttpRequest.newBuilder(front.resolve("/fail"))
                    .timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString()));
            if (fatal) assertSame(sentinel, propagated.get(3, TimeUnit.SECONDS));
            assertFalse(healthy.isDone());
            release.countDown();
            assertEquals("healthy", healthy.get(5, TimeUnit.SECONDS).body());
            assertEquals("accepted", http.send(HttpRequest.newBuilder(front.resolve("/after")).build(),
                    HttpResponse.BodyHandlers.ofString()).body());
            assertEquals(1, peers.size(), "RST_STREAM must not kill the parent HTTP/2 connection");
        } finally { release.countDown(); }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void fatalHttp11FailureTerminatesRequestAndPropagatesOriginal(boolean committed) throws Exception {
        Error fatal = new StackOverflowError("HTTP/1.1 fatal sentinel");
        CompletableFuture<Error> propagated = new CompletableFuture<>();
        try (Fixture fixture = new Fixture(); HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
            URI front = fixture.start(exchange -> {
                if (exchange.getRequestPath().equals("/after")) {
                    exchange.getResponseSender().send("accepted");
                    return;
                }
                exchange.dispatch(() -> {
                    try {
                        if (committed) {
                            var channel = exchange.getResponseChannel();
                            Channels.writeBlocking(channel, ByteBuffer.wrap("first".getBytes(StandardCharsets.UTF_8)));
                            Channels.flushBlocking(channel);
                        }
                        HandlerContinuation.task(exchange, ex -> { throw fatal; }).run();
                    } catch (Error error) { propagated.complete(error); }
                    catch (IOException e) { propagated.completeExceptionally(e); }
                });
            });
            var request = HttpRequest.newBuilder(front).timeout(Duration.ofSeconds(5)).build();
            if (committed) assertThrows(IOException.class, () -> http.send(request, HttpResponse.BodyHandlers.ofString()));
            else assertEquals(500, http.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
            assertSame(fatal, propagated.get(3, TimeUnit.SECONDS));
            assertEquals("accepted", http.send(HttpRequest.newBuilder(front.resolve("/after")).build(),
                    HttpResponse.BodyHandlers.ofString()).body());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"completed", "failed", "queued"})
    void callbackAfterCommitTerminatesIncompleteStreamWithoutForwarding(String kind) throws Exception {
        CompletableFuture<Runnable> late = new CompletableFuture<>();
        CountDownLatch committed = new CountDownLatch(1);
        CountDownLatch delivered = new CountDownLatch(1);
        AtomicInteger acquisitions = new AtomicInteger();
        var closed = new AtomicBoolean();
        ClientConnection unusedConnection = (ClientConnection) Proxy.newProxyInstance(ClientConnection.class.getClassLoader(),
                new Class<?>[]{ClientConnection.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isMultiplexingSupported" -> false;
                    case "close" -> { closed.set(true); yield null; }
                    case "isOpen" -> !closed.get();
                    case "toString" -> "unused late connection";
                    default -> throw new AssertionError("A late callback must not start a ProxyAction: " + method.getName());
                });
        ProxyClient client = new ProxyClient() {
            @Override public ProxyTarget findTarget(HttpServerExchange exchange) { return new ProxyTarget() {}; }
            @Override public void getConnection(ProxyTarget target, HttpServerExchange exchange,
                    ProxyCallback<ProxyConnection> callback, long timeout, TimeUnit unit) {
                acquisitions.incrementAndGet();
                late.complete(() -> {
                    try {
                        assertTrue(committed.await(5, TimeUnit.SECONDS));
                        assertTrue(exchange.isResponseStarted());
                        assertFalse(exchange.isComplete());
                        switch (kind) {
                            case "completed" -> callback.completed(exchange, new ProxyConnection(unusedConnection, "/"));
                            case "failed" -> callback.failed(exchange);
                            case "queued" -> callback.queuedRequestFailed(exchange);
                            default -> throw new AssertionError(kind);
                        }
                        awaitIoCallbacks(exchange);
                    } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
                    finally { delivered.countDown(); }
                });
                exchange.dispatch(worker -> {
                    worker.startBlocking();
                    worker.getOutputStream().write("first".getBytes(StandardCharsets.UTF_8));
                    worker.getOutputStream().flush();
                    committed.countDown();
                    assertTrue(delivered.await(5, TimeUnit.SECONDS));
                });
            }
        };
        ProxyHandler proxy = ProxyHandler.builder().setProxyClient(client).setMaxRequestTime(0).build();
        try (Fixture fixture = new Fixture()) {
            URI front = fixture.start(proxy);
            try (Socket socket = new Socket(front.getHost(), front.getPort())) {
                socket.setSoTimeout(5000);
                socket.getOutputStream().write("GET / HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"
                        .getBytes(StandardCharsets.UTF_8));
                late.get(3, TimeUnit.SECONDS).run();
                String raw = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(raw.startsWith("HTTP/1.1 200"), raw);
                assertTrue(raw.contains("first"), raw);
                assertEquals(1, acquisitions.get());
                if (kind.equals("completed")) assertTrue(closed.get());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"completed", "failed", "queued"})
    void unexpectedCommittedHttp2CallbackResetsOnlyItsStream(String kind) throws Exception {
        CompletableFuture<Runnable> late = new CompletableFuture<>();
        CountDownLatch committed = new CountDownLatch(1);
        CountDownLatch delivered = new CountDownLatch(1);
        AtomicInteger acquisitions = new AtomicInteger();
        CountDownLatch healthyEntered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Set<Integer> peers = ConcurrentHashMap.newKeySet();
        ClientConnection reusable = (ClientConnection) Proxy.newProxyInstance(ClientConnection.class.getClassLoader(),
                new Class<?>[]{ClientConnection.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isMultiplexingSupported", "isOpen" -> true;
                    case "toString" -> "shared late HTTP/2 connection";
                    default -> throw new AssertionError("Do not use or close the late shared connection: " + method.getName());
                });
        ProxyClient client = new ProxyClient() {
            @Override public ProxyTarget findTarget(HttpServerExchange exchange) { return new ProxyTarget() {}; }
            @Override public void getConnection(ProxyTarget target, HttpServerExchange exchange,
                    ProxyCallback<ProxyConnection> callback, long timeout, TimeUnit unit) {
                acquisitions.incrementAndGet();
                late.complete(() -> {
                    try {
                        assertTrue(committed.await(5, TimeUnit.SECONDS));
                        assertTrue(exchange.isResponseStarted());
                        assertFalse(exchange.isComplete());
                        switch (kind) {
                            case "completed" -> callback.completed(exchange, new ProxyConnection(reusable, "/"));
                            case "failed" -> callback.failed(exchange);
                            case "queued" -> callback.queuedRequestFailed(exchange);
                            default -> throw new AssertionError(kind);
                        }
                        awaitIoCallbacks(exchange);
                    } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
                    finally { delivered.countDown(); }
                });
                exchange.dispatch(worker -> {
                    worker.startBlocking();
                    worker.getOutputStream().write("first".getBytes(StandardCharsets.UTF_8));
                    worker.getOutputStream().flush();
                    committed.countDown();
                    assertTrue(delivered.await(5, TimeUnit.SECONDS));
                });
            }
        };
        ProxyHandler proxy = ProxyHandler.builder().setProxyClient(client).setMaxRequestTime(0).build();
        try (Fixture fixture = new Fixture(); HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build()) {
            try {
            URI front = fixture.start(exchange -> {
                peers.add(exchange.getSourceAddress().getPort());
                if (exchange.getRequestPath().equals("/healthy")) exchange.dispatch(() -> {
                    healthyEntered.countDown();
                    try {
                        assertTrue(release.await(8, TimeUnit.SECONDS));
                        exchange.getResponseSender().send("healthy");
                    } catch (InterruptedException _) { Thread.currentThread().interrupt(); }
                });
                else if (exchange.getRequestPath().equals("/fail")) proxy.handleRequest(exchange);
                else exchange.getResponseSender().send("accepted");
            }, true);
            assertEquals(HttpClient.Version.HTTP_2, http.send(HttpRequest.newBuilder(front.resolve("/warm")).build(),
                    HttpResponse.BodyHandlers.ofString()).version());
            var healthy = http.sendAsync(HttpRequest.newBuilder(front.resolve("/healthy")).timeout(Duration.ofSeconds(10)).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertTrue(healthyEntered.await(5, TimeUnit.SECONDS));
            var failed = http.send(HttpRequest.newBuilder(front.resolve("/fail")).timeout(Duration.ofSeconds(5)).build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            assertEquals(200, failed.statusCode());
            try (var body = failed.body()) {
                assertEquals("first", new String(body.readNBytes(5), StandardCharsets.UTF_8));
                late.get(3, TimeUnit.SECONDS).run();
                assertThrows(IOException.class, body::readAllBytes);
            }
            assertEquals(1, acquisitions.get());
            assertFalse(healthy.isDone());
            release.countDown();
            assertEquals("healthy", healthy.get(5, TimeUnit.SECONDS).body());
            assertEquals("accepted", http.send(HttpRequest.newBuilder(front.resolve("/after")).build(),
                    HttpResponse.BodyHandlers.ofString()).body());
            assertEquals(1, peers.size(), "Only the failed HTTP/2 stream may be reset");
            } finally { release.countDown(); http.shutdownNow(); }
        } finally { release.countDown(); }
    }

    @Test
    void lateCallbacksAfterProxy503LeaveDrainingHttp2ResponseAlone() throws Exception {
        CompletableFuture<Void> callbacksDone = new CompletableFuture<>();
        CountDownLatch releaseBody = new CountDownLatch(1);
        CountDownLatch healthyEntered = new CountDownLatch(1);
        CountDownLatch releaseHealthy = new CountDownLatch(1);
        Set<Integer> peers = ConcurrentHashMap.newKeySet();
        ClientConnection reusable = (ClientConnection) Proxy.newProxyInstance(ClientConnection.class.getClassLoader(),
                new Class<?>[]{ClientConnection.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isMultiplexingSupported", "isOpen" -> true;
                    default -> throw new AssertionError("Late callbacks must not use or close the shared upstream: " + method.getName());
                });
        ProxyClient client = new ProxyClient() {
            @Override public ProxyTarget findTarget(HttpServerExchange exchange) { return new ProxyTarget() {}; }
            @Override public void getConnection(ProxyTarget target, HttpServerExchange exchange,
                    ProxyCallback<ProxyConnection> callback, long timeout, TimeUnit unit) {
                try {
                    assertFalse(exchange.isRequestComplete());
                    callback.couldNotResolveBackend(exchange);
                    var responseChannel = exchange.getResponseChannel();
                    if (responseChannel != null) { responseChannel.shutdownWrites(); responseChannel.flush(); }
                    assertTrue(exchange.isResponseStarted());
                    assertFalse(exchange.isComplete(), "The 503 is still draining the request body");
                    callback.completed(exchange, new ProxyConnection(reusable, "/"));
                    callback.failed(exchange);
                    callback.queuedRequestFailed(exchange);
                    assertTrue(exchange.getConnection().isOpen(), "Keep the draining 503 stream open");
                    callbacksDone.complete(null);
                } catch (Throwable failure) { callbacksDone.completeExceptionally(failure); throw new AssertionError(failure); }
            }
        };
        ProxyHandler proxy = ProxyHandler.builder().setProxyClient(client).setMaxRequestTime(0).build();
        try (Fixture fixture = new Fixture(); HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build()) {
            URI front = fixture.start(exchange -> {
                peers.add(exchange.getSourceAddress().getPort());
                if (exchange.getRequestPath().equals("/fail")) proxy.handleRequest(exchange);
                else if (exchange.getRequestPath().equals("/healthy")) exchange.dispatch(() -> {
                    healthyEntered.countDown();
                    try {
                        assertTrue(releaseHealthy.await(8, TimeUnit.SECONDS));
                        exchange.getResponseSender().send("healthy");
                    } catch (InterruptedException _) { Thread.currentThread().interrupt(); }
                });
                else exchange.getResponseSender().send("accepted");
            }, true);
            assertEquals(HttpClient.Version.HTTP_2, http.send(HttpRequest.newBuilder(front.resolve("/warm")).build(),
                    HttpResponse.BodyHandlers.ofString()).version());
            var healthy = http.sendAsync(HttpRequest.newBuilder(front.resolve("/healthy")).build(), HttpResponse.BodyHandlers.ofString());
            assertTrue(healthyEntered.await(5, TimeUnit.SECONDS));
            var body = HttpRequest.BodyPublishers.ofInputStream(() -> new InputStream() {
                @Override public int read() throws IOException {
                    try { if (!releaseBody.await(8, TimeUnit.SECONDS)) throw new IOException("Body was not released"); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
                    return -1;
                }
            });
            var failed = http.sendAsync(HttpRequest.newBuilder(front.resolve("/fail")).timeout(Duration.ofSeconds(10)).POST(body).build(),
                    HttpResponse.BodyHandlers.ofString());
            try { callbacksDone.get(5, TimeUnit.SECONDS); }
            catch (Exception failure) { releaseBody.countDown(); releaseHealthy.countDown(); throw failure; }
            assertFalse(healthy.isDone());
            releaseBody.countDown();
            assertEquals(503, failed.get(5, TimeUnit.SECONDS).statusCode());
            releaseHealthy.countDown();
            assertEquals("healthy", healthy.get(5, TimeUnit.SECONDS).body());
            assertEquals("accepted", http.send(HttpRequest.newBuilder(front.resolve("/after")).build(),
                    HttpResponse.BodyHandlers.ofString()).body());
            assertEquals(1, peers.size());
        } finally { releaseBody.countDown(); releaseHealthy.countDown(); }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void http2PushUsesItsOwnExchangeAndSelectedBoundary(boolean rejectPush) throws Exception {
        var pushedResponses = new ConcurrentHashMap<HttpRequest, CompletableFuture<HttpResponse<String>>>();
        var pushObserved = new CountDownLatch(1);
        HttpResponse.PushPromiseHandler<String> acceptPush = (request, pushed, acceptor) -> {
            pushedResponses.put(pushed, acceptor.apply(HttpResponse.BodyHandlers.ofString()));
            pushObserved.countDown();
        };
        var parentExchange = new AtomicReference<HttpServerExchange>();
        var childExchange = new AtomicReference<HttpServerExchange>();
        var failures = new CompletableFuture<Throwable>();
        Set<Integer> peers = ConcurrentHashMap.newKeySet();
        try (Fixture fixture = new Fixture(); HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build()) {
            try {
                URI backend = fixture.start(exchange -> {
                    if (exchange.getRequestPath().equals("/parent")) {
                        assertTrue(exchange.getConnection().pushResource("/pushed", Methods.GET,
                                new HeaderMap(), child -> child.getResponseSender().send("pushed")));
                        exchange.getResponseSender().send("parent");
                    } else exchange.getResponseSender().send("pushed");
                }, true);
                URI h2 = URI.create("h2c-prior://" + backend.getHost() + ":" + backend.getPort());
                ProxyHandler proxy = ProxyHandler.builder().setProxyClient(new LoadBalancingProxyClient().addHost(h2))
                        .setMaxRequestTime(0).addRequestHeader(new HttpString("X-Push-Test"), new ExchangeAttribute() {
                            @Override public String readAttribute(HttpServerExchange ex) {
                                if (rejectPush && ex.getRequestPath().equals("/pushed"))
                                    throw new ControlledFailure(Phase.BEFORE_SEND);
                                return "value";
                            }
                            @Override public void writeAttribute(HttpServerExchange ex, String value) {
                                throw new UnsupportedOperationException("Read-only test attribute");
                            }
                        }).build();
                URI front = fixture.start(exchange -> {
                    peers.add(exchange.getSourceAddress().getPort());
                    if (exchange.getRequestPath().equals("/warm")) exchange.getResponseSender().send("warm");
                    else HandlerContinuation.execute(exchange, (ex, next) -> {
                        if (ex.getRequestPath().equals("/pushed")) childExchange.set(ex);
                        else parentExchange.set(ex);
                        try { next.handleRequest(ex); }
                        catch (ControlledFailure failure) {
                            assertEquals("/pushed", ex.getRequestPath());
                            assertEquals(Phase.BEFORE_SEND, failure.phase);
                            ex.setStatusCode(409).getResponseSender().send("selected child");
                        } catch (Exception | Error failure) { failures.complete(failure); throw failure; }
                    }, proxy);
                }, true);
                assertEquals(HttpClient.Version.HTTP_2, http.sendAsync(HttpRequest.newBuilder(front.resolve("/warm")).build(),
                        HttpResponse.BodyHandlers.ofString(), acceptPush).get(5, TimeUnit.SECONDS).version());
                var parent = http.sendAsync(HttpRequest.newBuilder(front.resolve("/parent")).timeout(Duration.ofSeconds(6)).build(),
                        HttpResponse.BodyHandlers.ofString(), acceptPush);
                assertEquals("parent", parent.get(8, TimeUnit.SECONDS).body());
                assertTrue(pushObserved.await(5, TimeUnit.SECONDS), "Must receive a real HTTP/2 push promise");
                assertEquals(1, pushedResponses.size(), "Must exercise actual HTTP/2 push");
                var pushed = pushedResponses.values().iterator().next().get(5, TimeUnit.SECONDS);
                assertEquals(rejectPush ? 409 : 200, pushed.statusCode(), pushed.body());
                assertEquals(rejectPush ? "selected child" : "pushed", pushed.body());
                assertNotNull(parentExchange.get());
                assertNotNull(childExchange.get());
                assertNotSame(parentExchange.get(), childExchange.get());
                assertFalse(failures.isDone(), "Push must not corrupt the parent exchange");
                assertEquals("pushed", http.sendAsync(HttpRequest.newBuilder(front.resolve("/after")).build(),
                        HttpResponse.BodyHandlers.ofString(), acceptPush).get(5, TimeUnit.SECONDS).body());
                assertEquals(1, peers.size(), "The parent connection must survive its pushed response");
            } finally { http.shutdownNow(); }
        }
    }

    @Test
    void deferredBoundaryHasNoCallbackSlotAndPreservesPublicConnectionAttachment() throws Exception {
        var frontendExchange = new AtomicReference<HttpServerExchange>();
        var acquired = new AtomicReference<ProxyConnection>();
        var boundaryCalls = new AtomicInteger();
        var callbackFailure = new CompletableFuture<Throwable>();
        try (Fixture fixture = new Fixture(); HttpClient http = HttpClient.newHttpClient()) {
            URI backend = fixture.start(exchange -> exchange.getResponseSender().send("accepted"));
            var real = new LoadBalancingProxyClient().addHost(backend);
            ProxyClient client = new ProxyClient() {
                @Override public ProxyTarget findTarget(HttpServerExchange ex) { return real.findTarget(ex); }
                @Override public void getConnection(ProxyTarget target, HttpServerExchange ex,
                        ProxyCallback<ProxyConnection> callback, long timeout, TimeUnit unit) {
                    assertTrue(ex.isInIoThread(), "No callback-wide monitor: transport work belongs to the I/O thread");
                    real.getConnection(target, ex, new ProxyCallback<>() {
                        @Override public void completed(HttpServerExchange exchange, ProxyConnection connection) {
                            acquired.set(connection);
                            callback.completed(exchange, connection);
                        }
                        @Override public void failed(HttpServerExchange exchange) { callback.failed(exchange); }
                        @Override public void couldNotResolveBackend(HttpServerExchange exchange) { callback.couldNotResolveBackend(exchange); }
                        @Override public void queuedRequestFailed(HttpServerExchange exchange) { callback.queuedRequestFailed(exchange); }
                    }, timeout, unit);
                }
            };
            var proxy = ProxyHandler.builder().setProxyClient(client).setMaxRequestTime(0).build();
            URI front = fixture.start(exchange -> {
                frontendExchange.set(exchange);
                HandlerContinuation.execute(exchange, (ex, next) -> {
                    if (boundaryCalls.getAndIncrement() == 0) next.handleRequest(ex);
                    else ex.dispatch(ex.getConnection().getWorker(), () -> {
                        try { next.handleRequest(ex); }
                        catch (Throwable failure) { callbackFailure.complete(failure); }
                    });
                }, proxy);
            });
            var response = http.send(HttpRequest.newBuilder(front).timeout(Duration.ofSeconds(8)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals("accepted", response.body());
            assertTrue(boundaryCalls.get() > 1);
            assertFalse(callbackFailure.isDone());
            awaitIoCallbacks(frontendExchange.get());
            assertSame(acquired.get(), frontendExchange.get().getAttachment(ProxyHandler.CONNECTION));
        }
    }

    @Test
    void retryTimeoutReleasesCurrentConnectionAndIgnoresOldAcquisitionFailure() throws Exception {
        var firstResponseFailure = new CompletableFuture<Runnable>();
        var secondSend = new CountDownLatch(1);
        var connections = new CopyOnWriteArrayList<ClientConnection>();
        var acquired = new CopyOnWriteArrayList<ProxyConnection>();
        var callbacks = new CopyOnWriteArrayList<ProxyCallback<ProxyConnection>>();
        var frontend = new AtomicReference<HttpServerExchange>();
        var attempts = new AtomicInteger();
        try (Fixture fixture = new Fixture(); HttpClient http = HttpClient.newHttpClient()) {
            URI backend = fixture.start(HttpServerExchange::dispatch);
            ProxyClient client = new ProxyClient() {
                @Override public ProxyTarget findTarget(HttpServerExchange ex) { return new ProxyTarget() {}; }
                @Override public void getConnection(ProxyTarget target, HttpServerExchange exchange,
                        ProxyCallback<ProxyConnection> callback, long timeout, TimeUnit unit) {
                    assertTrue(exchange.isInIoThread());
                    callbacks.add(callback);
                    int attempt = attempts.incrementAndGet();
                    UndertowClient.getInstance().connect(new ClientCallback<>() {
                        @Override public void completed(ClientConnection connection) {
                            connections.add(connection);
                            ClientConnection decorated = (ClientConnection) Proxy.newProxyInstance(ClientConnection.class.getClassLoader(),
                                    new Class<?>[]{ClientConnection.class}, (proxy, method, args) -> {
                                if (method.getName().equals("sendRequest")) {
                                    if (attempt == 2) { secondSend.countDown(); return null; }
                                    @SuppressWarnings("unchecked") var send = (ClientCallback<ClientExchange>) args[1];
                                    connection.sendRequest((ClientRequest) args[0], new ClientCallback<>() {
                                        @Override public void completed(ClientExchange request) {
                                            ClientExchange decoratedRequest = (ClientExchange) Proxy.newProxyInstance(ClientExchange.class.getClassLoader(),
                                                    new Class<?>[]{ClientExchange.class}, (proxy, method, args) -> {
                                                if (method.getName().equals("setResponseListener")) {
                                                    @SuppressWarnings("unchecked") var listener = (ClientCallback<ClientExchange>) args[0];
                                                    firstResponseFailure.complete(() -> listener.failed(new IOException("first response failed")));
                                                    return null;
                                                }
                                                try { return method.invoke(request, args); }
                                                catch (InvocationTargetException e) { throw e.getCause(); }
                                            });
                                            send.completed(decoratedRequest);
                                        }
                                        @Override public void failed(IOException e) { send.failed(e); }
                                    });
                                    return null;
                                }
                                try { return method.invoke(connection, args); }
                                catch (InvocationTargetException e) { throw e.getCause(); }
                            });
                            var selected = new ProxyConnection(decorated, "/");
                            acquired.add(selected);
                            callback.completed(exchange, selected);
                        }
                        @Override public void failed(IOException e) { callback.failed(exchange); }
                    }, backend, exchange.getIoThread(), exchange.getConnection().getByteBufferPool(), org.xnio.OptionMap.EMPTY);
                }
            };
            var proxy = ProxyHandler.builder().setProxyClient(client).setMaxConnectionRetries(1).setMaxRequestTime(1500).build();
            URI front = fixture.start(exchange -> { frontend.set(exchange); proxy.handleRequest(exchange); });
            var response = http.sendAsync(HttpRequest.newBuilder(front).timeout(Duration.ofSeconds(6)).build(), HttpResponse.BodyHandlers.ofString());
            firstResponseFailure.get(5, TimeUnit.SECONDS).run();
            assertTrue(secondSend.await(5, TimeUnit.SECONDS));
            callbacks.getFirst().failed(frontend.get());
            callbacks.getFirst().queuedRequestFailed(frontend.get());
            callbacks.getFirst().couldNotResolveBackend(frontend.get());
            callbacks.getFirst().completed(frontend.get(), acquired.getFirst());
            awaitIoCallbacks(frontend.get());
            assertSame(acquired.getLast(), frontend.get().getAttachment(ProxyHandler.CONNECTION));
            assertEquals(2, attempts.get(), "An old acquisition callback cannot retry the current attempt");
            assertEquals(504, response.get(5, TimeUnit.SECONDS).statusCode());
            assertEquals(2, connections.size());
            assertFalse(connections.getFirst().isOpen());
            assertFalse(connections.getLast().isOpen(), "Timeout must release the current attempt's connection");
        }
    }

    private static void awaitIoCallbacks(HttpServerExchange exchange) {
        assertFalse(exchange.isInIoThread(), "Only the test thread waits for I/O ownership");
        var drained = new CompletableFuture<Void>();
        exchange.getIoThread().execute(() -> drained.complete(null));
        try { drained.get(5, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
        catch (ExecutionException | TimeoutException e) { throw new AssertionError(e); }
    }

    private static ClientConnection decorateConnection(ClientConnection real, Phase phase,
            Supplier<ControlledFailure> failure) {
        return (ClientConnection) Proxy.newProxyInstance(ClientConnection.class.getClassLoader(),
                new Class<?>[]{ClientConnection.class}, (proxy, method, args) -> {
            if (method.getName().equals("sendRequest")) {
                if (phase == Phase.BEFORE_SEND) throw failure.get();
                @SuppressWarnings("unchecked") var callback = (ClientCallback<ClientExchange>) args[1];
                real.sendRequest((ClientRequest) args[0], new ClientCallback<>() {
                    @Override public void completed(ClientExchange result) { callback.completed(decorateExchange(result, phase, failure)); }
                    @Override public void failed(IOException e) { callback.failed(e); }
                });
                return null;
            }
            try { return method.invoke(real, args); }
            catch (InvocationTargetException e) { throw e.getCause(); }
        });
    }

    private static ClientExchange decorateExchange(ClientExchange real, Phase phase,
            Supplier<ControlledFailure> failure) {
        return (ClientExchange) Proxy.newProxyInstance(ClientExchange.class.getClassLoader(),
                new Class<?>[]{ClientExchange.class}, (proxy, method, args) -> {
            if (method.getName().equals("setResponseListener")) {
                if (phase == Phase.SEND_CALLBACK) throw failure.get();
                @SuppressWarnings("unchecked") var callback = (ClientCallback<ClientExchange>) args[0];
                real.setResponseListener(new ClientCallback<>() {
                    @Override public void completed(ClientExchange result) { callback.completed(decorateExchange(result, phase, failure)); }
                    @Override public void failed(IOException e) { callback.failed(e); }
                });
                return null;
            }
            if (method.getName().equals("getResponse") && phase == Phase.RESPONSE_CALLBACK) throw failure.get();
            try { return method.invoke(real, args); }
            catch (InvocationTargetException e) { throw e.getCause(); }
        });
    }

    private static final class ThrowingMetrics extends AbstractMetricsHandler {
        private final Supplier<ControlledFailure> failure;
        ThrowingMetrics(Supplier<ControlledFailure> failure) { this.failure = failure; }
        @Override public void injectMetrics(HttpServerExchange exchange, long startTime, String name, String endpoint) { throw failure.get(); }
        @Override protected void createMetricsReporter(TimeSeriesDbSender sender, MetricsConfig config) { /* No reporter in this fixture. */ }
        @Override public void handleRequest(HttpServerExchange exchange) { /* Lookup-only metrics fixture. */ }
        @Override public HttpHandler getNext() { return null; }
        @Override public MiddlewareHandler setNext(HttpHandler next) { return this; }
    }

    private static final class Fixture implements AutoCloseable {
        private final List<Undertow> servers = new ArrayList<>();
        URI start(HttpHandler handler) { return start(handler, false); }
        URI start(HttpHandler handler, boolean http2) {
            Undertow server = Undertow.builder().addHttpListener(0, "127.0.0.1").setIoThreads(1)
                    .setServerOption(UndertowOptions.ENABLE_HTTP2, http2).setHandler(handler).build();
            server.start();
            servers.add(server);
            return URI.create("http://127.0.0.1:" + ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort());
        }
        @Override public void close() {
            for (int i = servers.size() - 1; i >= 0; i--) servers.get(i).stop();
        }
    }
}
