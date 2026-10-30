package com.networknt.client;

import com.networknt.client.http.Http2ClientCompletableFutureNoRequest;
import com.networknt.client.http.Http2ClientConnectionPool;
import com.networknt.client.http.IoThreadClientConnection;
import com.networknt.client.simplepool.SimpleConnectionState;
import io.undertow.Undertow;
import io.undertow.client.*;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.xnio.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** The borrowed/returned connection itself protects existing direct send/close callers. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Http2ClientConnectionBoundaryTest {
    private Undertow server;
    private XnioWorker worker;
    private Http2Client client;
    private URI uri;
    private final List<ClientConnection> connections = new CopyOnWriteArrayList<>();

    @BeforeAll
    void createClient() { client = new Http2Client(); }

    @AfterAll
    void closeClient() { client.shutdown(); }

    @BeforeEach
    void setUp() throws Exception {
        server = Undertow.builder().addHttpListener(0, "localhost").setIoThreads(1).setWorkerThreads(2)
                .setHandler(exchange -> exchange.getResponseSender().send("boundary")).build();
        server.start();
        uri = URI.create("http://localhost:" + ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort());
        worker = Xnio.getInstance().createWorker(OptionMap.create(Options.WORKER_IO_THREADS, 2));
        Http2ClientConnectionPool.getInstance().clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        connections.forEach(connection -> { try { connection.close(); } catch (IOException _) { /* Continue closing the remaining fixture connections. */ } });
        connections.clear();
        Http2ClientConnectionPool.getInstance().clear();
        worker.shutdown();
        assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        server.stop();
    }

    @ParameterizedTest
    @ValueSource(strings = {"async", "legacy-worker", "legacy-io", "callback-worker", "callback-bind-worker",
                            "callback-io", "callback-bind-io", "simple"})
    void returnedConnectionsProtectDirectSendsAndExposeImmediateClose(String entry) throws Exception {
        SimpleConnectionState.ConnectionToken token = null;
        ClientConnection returned;
        if (entry.equals("simple")) {
            token = client.borrow(uri, worker, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
            returned = (ClientConnection) token.getRawConnection();
        } else returned = connect(entry);
        connections.add(returned);
        assertInstanceOf(IoThreadClientConnection.class, returned);
        Http2ClientCompletableFutureNoRequest body = new Http2ClientCompletableFutureNoRequest();
        CompletableFuture<Thread> sendThread = new CompletableFuture<>();
        CompletableFuture<ClientConnection> callbackConnection = new CompletableFuture<>();
        ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath("/");
        request.getRequestHeaders().put(Headers.HOST, "localhost");
        returned.sendRequest(request, new ClientCallback<>() {
            @Override public void completed(ClientExchange exchange) {
                sendThread.complete(Thread.currentThread());
                callbackConnection.complete(exchange.getConnection());
                body.completed(exchange);
            }
            @Override public void failed(IOException error) { body.failed(error); }
        });
        assertEquals("boundary", body.get(5, TimeUnit.SECONDS).getAttachment(Http2Client.RESPONSE_BODY));
        assertSame(returned.getIoThread(), sendThread.get(5, TimeUnit.SECONDS));
        assertSame(returned, callbackConnection.get(5, TimeUnit.SECONDS));
        returned.close();
        assertFalse(returned.isOpen());
        if (token != null) {
            assertFalse(token.connection().isOpen());
            client.restore(token);
            var replacement = client.borrow(uri, worker, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
            ClientConnection next = (ClientConnection) replacement.getRawConnection();
            connections.add(next);
            assertNotSame(returned, next, "the next borrower must not get a connection scheduled for close");
            client.restore(replacement);
        }
    }

    @ParameterizedTest
    @CsvSource({"async,false", "async,true", "legacy-worker,false", "legacy-worker,true", "legacy-io,false", "legacy-io,true"})
    void cacheFailureFinishesConnectAndRetiresNewConnection(String entry, boolean error) throws Exception {
        Throwable original = error ? new AssertionError("cache failed") : new IllegalStateException("cache failed");
        Http2ClientConnectionPool brokenPool = mock(Http2ClientConnectionPool.class);
        AtomicReference<ClientConnection> retired = new AtomicReference<>();
        doAnswer(invocation -> {
            ClientConnection connection = invocation.getArgument(1);
            retired.set(connection);
            connections.add(connection);
            throw original;
        }).when(brokenPool).cacheConnection(any(), any());
        // Inject only the cache failure, without replacing providers or relying on static mocking.
        Field poolField = Http2Client.class.getDeclaredField("http2ClientConnectionPool");
        poolField.setAccessible(true);
        Object originalPool = poolField.get(client);
        poolField.set(client, brokenPool);
        try {
            Throwable failure = assertThrows(Exception.class, () -> connect(entry));
            assertSame(original, failure.getCause(), "cache errors must finish the future with the original cause");
            assertNotNull(retired.get());
            assertFalse(retired.get().isOpen());
        } finally { poolField.set(client, originalPool); }
    }

    private ClientConnection connect(String entry) throws Exception {
        if (entry.equals("async")) return client.connectAsync(null, uri, worker, null, Http2Client.BUFFER_POOL, OptionMap.EMPTY)
                .get(5, TimeUnit.SECONDS);
        if (entry.startsWith("legacy")) {
            IoFuture<ClientConnection> future = entry.equals("legacy-worker")
                    ? client.connect((InetSocketAddress) null, uri, worker, null, Http2Client.BUFFER_POOL, OptionMap.EMPTY)
                    : client.connect((InetSocketAddress) null, uri, worker.getIoThread(), null, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
            assertNotEquals(IoFuture.Status.WAITING, future.await(5, TimeUnit.SECONDS), "connect must finish even if cache insertion fails");
            return future.get();
        }
        CompletableFuture<ClientConnection> future = new CompletableFuture<>();
        ClientCallback<ClientConnection> callback = new ClientCallback<>() {
            @Override public void completed(ClientConnection connection) { future.complete(connection); }
            @Override public void failed(IOException error) { future.completeExceptionally(error); }
        };
        switch (entry) {
            case "callback-worker" -> client.connect(callback, uri, worker, null, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
            case "callback-bind-worker" -> client.connect(callback, null, uri, worker, null, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
            case "callback-io" -> client.connect(callback, uri, worker.getIoThread(), null, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
            case "callback-bind-io" -> client.connect(callback, null, uri, worker.getIoThread(), null, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
            default -> throw new IllegalArgumentException(entry);
        }
        return future.get(5, TimeUnit.SECONDS);
    }
}
