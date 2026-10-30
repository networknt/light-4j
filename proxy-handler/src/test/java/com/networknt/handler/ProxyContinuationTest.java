package com.networknt.handler;

import com.networknt.httpstring.AttachmentConstants;
import com.networknt.metrics.AbstractMetricsHandler;
import com.networknt.metrics.MetricsConfig;
import com.networknt.metrics.TimeSeriesDbSender;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.client.*;
import io.undertow.server.*;
import io.undertow.server.handlers.proxy.*;
import io.undertow.util.Headers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.xnio.channels.Channels;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
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
        var previousMdc = org.slf4j.MDC.getCopyOfContextMap();
        AtomicInteger injected = new AtomicInteger();
        AtomicInteger selected = new AtomicInteger();
        AtomicInteger depth = new AtomicInteger();
        AtomicInteger maxDepth = new AtomicInteger();
        AtomicInteger acquisitions = new AtomicInteger();
        var upstream = new java.util.concurrent.atomic.AtomicReference<ClientConnection>();
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Runnable> retry = new CompletableFuture<>();
        java.util.function.Supplier<ControlledFailure> failure = () -> {
            assertEquals(hasMdc ? "canonical-request" : null, org.slf4j.MDC.get("request"),
                    "Callbacks must install canonical request MDC for this frame");
            injected.incrementAndGet();
            return new ControlledFailure(phase);
        };
        try (Fixture fixture = new Fixture(); HttpClient http = HttpClient.newHttpClient()) {
            URI backend = fixture.start(exchange -> exchange.dispatch(() -> {
                try {
                    if (phase == Phase.TIMEOUT && !release.await(5, TimeUnit.SECONDS)) return;
                    exchange.getResponseSender().send("accepted");
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
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
                if (hasMdc) exchange.putAttachment(AttachmentConstants.MDC_CONTEXT, Map.of("request", "canonical-request"));
                if (phase == Phase.TIMEOUT) {
                    exchange.putAttachment(AttachmentConstants.METRICS_HANDLER, new ThrowingMetrics(failure));
                    exchange.putAttachment(AttachmentConstants.DOWNSTREAM_METRICS_NAME, "test");
                    exchange.putAttachment(AttachmentConstants.DOWNSTREAM_METRICS_START, System.nanoTime());
                }
                proxy.handleRequest(exchange);
            };
            URI front = fixture.start(enabled ? exchange -> HandlerContinuation.execute(exchange, (ex, task) -> {
                maxDepth.accumulateAndGet(depth.incrementAndGet(), Math::max);
                try { task.handleRequest(ex); }
                catch (ControlledFailure e) {
                    assertEquals(phase, e.phase);
                    selected.incrementAndGet();
                    ex.setStatusCode(409).getResponseSender().send("selected");
                } finally { depth.decrementAndGet(); }
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
            if (previousMdc == null) org.slf4j.MDC.clear();
            else org.slf4j.MDC.setContextMap(previousMdc);
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
        var completed = new java.util.concurrent.atomic.AtomicReference<HttpServerExchange>();
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
        var shared = new java.util.concurrent.atomic.AtomicReference<ClientConnection>();
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
                    } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                });
                else exchange.getResponseSender().send("accepted");
            }, true);
            URI upstream = URI.create("h2c-prior://" + backend.getHost() + ":" + backend.getPort());
            ProxyClient client = new ProxyClient() {
                @Override public ProxyTarget findTarget(HttpServerExchange exchange) { return new ProxyTarget() {}; }
                @Override public void getConnection(ProxyTarget target, HttpServerExchange exchange,
                        ProxyCallback<ProxyConnection> callback, long timeout, TimeUnit unit) {
                    if (exchange.getRequestPath().equals("/fail")) failedAcquisitions.incrementAndGet();
                    java.util.function.Consumer<ClientConnection> connected = connection -> {
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
                    catch (ControlledFailure e) { ex.setStatusCode(409).getResponseSender().send("selected"); }
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
    @org.junit.jupiter.params.provider.CsvSource({"false,true", "true,true", "true,false"})
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
                    } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
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
            assertThrows(IOException.class, () -> http.send(HttpRequest.newBuilder(front.resolve("/fail"))
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

    private static ClientConnection decorateConnection(ClientConnection real, Phase phase,
            java.util.function.Supplier<ControlledFailure> failure) {
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
            java.util.function.Supplier<ControlledFailure> failure) {
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
        private final java.util.function.Supplier<ControlledFailure> failure;
        ThrowingMetrics(java.util.function.Supplier<ControlledFailure> failure) { this.failure = failure; }
        @Override public void injectMetrics(HttpServerExchange exchange, long startTime, String name, String endpoint) { throw failure.get(); }
        @Override protected void createMetricsReporter(TimeSeriesDbSender sender, MetricsConfig config) { }
        @Override public void handleRequest(HttpServerExchange exchange) { }
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
