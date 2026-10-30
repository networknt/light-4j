package com.networknt.client;

import com.networknt.client.http.Http2ClientConnectionPool;
import com.networknt.client.http.IoThreadClientConnection;
import com.networknt.client.http.RequestSubmission;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.client.*;
import io.undertow.io.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xnio.*;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** Native HTTP/2 cancellation must reset the peer stream and retain the shared transport. */
@Isolated("Uses the shared connection pool")
class Http2ClientCancellationLoopbackTest {
    private final CompletableFuture<HttpServerExchange> stalled = new CompletableFuture<>();
    private final CountDownLatch releaseBackend = new CountDownLatch(1);
    private final AtomicInteger connects = new AtomicInteger();
    private final List<ClientConnection> connections = new CopyOnWriteArrayList<>();
    private Undertow server;
    private XnioWorker worker;
    private Http2Client client;
    private URI target;

    @BeforeEach void setUp() throws Exception {
        server = Undertow.builder().addHttpListener(0, "localhost").setIoThreads(1).setWorkerThreads(2)
                .setServerOption(UndertowOptions.ENABLE_HTTP2, true).setHandler(exchange -> {
                    if (exchange.getRequestPath().equals("/stall")) {
                        exchange.dispatch(() -> {
                            stalled.complete(exchange);
                            try { releaseBackend.await(30, TimeUnit.SECONDS); }
                            catch (InterruptedException _) { Thread.currentThread().interrupt(); }
                        });
                    } else exchange.getResponseSender().send("healthy");
                }).build();
        server.start();
        int port = ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort();
        target = URI.create("h2c-prior://localhost:" + port);
        worker = Xnio.getInstance().createWorker(OptionMap.create(Options.WORKER_IO_THREADS, 1));
        client = new Http2Client() {
            @Override public CompletableFuture<ClientConnection> connectAsync(URI uri, boolean enabled) {
                connects.incrementAndGet();
                return super.connectAsync(null, uri, worker, null, BUFFER_POOL,
                        OptionMap.create(UndertowOptions.ENABLE_HTTP2, true))
                        .thenApply(connection -> { connections.add(connection); return connection; });
            }
        };
        Http2ClientConnectionPool.getInstance().clear();
    }
    @AfterEach void tearDown() throws Exception {
        releaseBackend.countDown();
        Http2ClientConnectionPool.getInstance().clear();
        connections.forEach(org.xnio.IoUtils::safeClose);
        if (client != null) client.shutdown();
        if (worker != null) {
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        }
        if (server != null) server.stop();
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void cancellationBeforeResponseHeadersResetsPeerStream(boolean timeout) throws Exception {
        CompletableFuture<ClientResponse> response = client.callService(target, request("/stall"), Optional.empty(), true);
        HttpServerExchange backend = stalled.get(5, TimeUnit.SECONDS);
        if (timeout) {
            ExecutionException error = assertThrows(ExecutionException.class,
                    () -> response.orTimeout(100, TimeUnit.MILLISECONDS).get(5, TimeUnit.SECONDS));
            assertInstanceOf(TimeoutException.class, error.getCause());
        } else assertTrue(response.cancel(false));
        assertHealthyTransport();
        CompletableFuture<Boolean> reset = new CompletableFuture<>();
        backend.getIoThread().execute(() -> backend.getResponseSender().send("late", new IoCallback() {
            @Override public void onComplete(HttpServerExchange exchange, Sender sender) {
                reset.complete(false);
                releaseBackend.countDown();
            }
            @Override public void onException(HttpServerExchange exchange, Sender sender, IOException error) {
                reset.complete(true);
                releaseBackend.countDown();
            }
        }));
        assertTrue(reset.get(5, TimeUnit.SECONDS), "the cancelled peer stream must reject late response writes");
        assertTrue(connections.getFirst().isOpen());
    }
    @Test void cancellationInsideNativeRequestCallbackKeepsTransportUsable() throws Exception {
        ClientConnection connection = client.connectAsync(target, true).get(5, TimeUnit.SECONDS);
        RequestSubmission submission = new RequestSubmission();
        CompletableFuture<Void> notified = new CompletableFuture<>();
        IoThreadClientConnection.wrap(connection).sendRequest(request("/stall"), new ClientCallback<>() {
            @Override public void completed(ClientExchange exchange) {
                submission.cancel();
                notified.complete(null);
            }
            @Override public void failed(IOException error) { notified.completeExceptionally(error); }
        }, submission);
        notified.get(5, TimeUnit.SECONDS);
        client.returnConnection(connection);
        assertHealthyTransport();
        assertTrue(connection.isOpen());
    }
    @Test void clearKeepsAnInflightResponseUsable() throws Exception {
        CompletableFuture<ClientResponse> response = client.callService(target, request("/stall"), Optional.empty(), true);
        HttpServerExchange backend = stalled.get(5, TimeUnit.SECONDS);
        Http2ClientConnectionPool.getInstance().clear();
        assertTrue(connections.getFirst().isOpen());
        backend.getIoThread().execute(() -> {
            backend.getResponseSender().send("released");
            releaseBackend.countDown();
        });
        assertEquals("released", response.get(5, TimeUnit.SECONDS).getAttachment(Http2Client.RESPONSE_BODY));
        assertEquals(0, Http2ClientConnectionPool.getInstance().numberOfConnections());
        assertEquals(1, connects.get());
    }
    private ClientRequest request(String path) { return new ClientRequest().setMethod(Methods.GET).setPath(path); }
    private void assertHealthyTransport() throws Exception {
        ClientResponse response = client.callService(target, request("/healthy"), Optional.empty(), true).get(5, TimeUnit.SECONDS);
        assertEquals("healthy", response.getAttachment(Http2Client.RESPONSE_BODY));
        assertEquals(1, connects.get(), "cancellation must not replace the multiplexed connection");
    }
}
