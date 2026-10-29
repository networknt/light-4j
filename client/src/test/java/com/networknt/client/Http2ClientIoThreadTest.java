package com.networknt.client;

import com.networknt.client.http.Http2ClientConnectionPool;
import com.networknt.client.http.ClientConnectionOperations;
import io.undertow.client.*;
import io.undertow.util.Methods;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xnio.*;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class Http2ClientIoThreadTest {
    private XnioWorker worker;
    private XnioIoThread owner;
    private ClientConnection connection;
    private TestClient client;
    private final Http2ClientConnectionPool pool = Http2ClientConnectionPool.getInstance();
    private final URI uri = URI.create("http://io-thread-test.invalid");

    @BeforeEach
    void setUp() throws IOException {
        worker = Xnio.getInstance().createWorker(OptionMap.create(Options.WORKER_IO_THREADS, 1));
        owner = worker.getIoThread();
        connection = mock(ClientConnection.class);
        when(connection.getIoThread()).thenReturn(owner);
        when(connection.isOpen()).thenReturn(true);
        pool.clear();
        client = new TestClient();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        client.shutdown();
        pool.clear();
        worker.shutdownNow();
        assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
    }

    @ParameterizedTest
    @CsvSource({"false,false,false", "false,false,true", "false,true,false", "false,true,true",
                "true,false,false", "true,false,true", "true,true,false", "true,true,true"})
    void sendsOnOwnerForWarmAndColdCalls(boolean warm, boolean body, boolean explicitHttp2) throws Exception {
        IOException failure = new IOException("expected send failure");
        AtomicReference<Thread> sendThread = new AtomicReference<>();
        doAnswer(invocation -> {
            sendThread.set(Thread.currentThread());
            invocation.<ClientCallback<ClientExchange>>getArgument(1).failed(failure);
            return null;
        }).when(connection).sendRequest(any(), any());
        if (warm) {
            pool.cacheConnection(uri, connection);
            pool.resetConnectionStatus(connection);
        }
        CompletableFuture<ClientResponse> response = call(client, body, explicitHttp2);
        if (!warm) client.connected.complete(connection); // complete from this non-I/O thread
        ExecutionException exception = assertThrows(ExecutionException.class, () -> response.get(5, TimeUnit.SECONDS));
        assertSame(failure, exception.getCause());
        assertSame(owner, sendThread.get(), "ClientConnection.sendRequest must run on its owning I/O thread");
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void cancellationBeforeDispatchSkipsSend(boolean warm, boolean body) throws Exception {
        CountDownLatch blocked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        owner.execute(() -> {
            blocked.countDown();
            try { release.await(5, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        assertTrue(blocked.await(5, TimeUnit.SECONDS));
        try {
            if (warm) {
                pool.cacheConnection(uri, connection);
                pool.resetConnectionStatus(connection);
            }
            CompletableFuture<ClientResponse> response = call(client, body, false);
            assertTrue(response.cancel(false));
            if (!warm) client.connected.complete(connection);
            release.countDown();
            CompletableFuture<Void> drained = new CompletableFuture<>();
            owner.execute(() -> drained.complete(null));
            drained.get(5, TimeUnit.SECONDS);
            verify(connection, never()).sendRequest(any(), any());
            assertTrue(response.isCancelled());
        } finally {
            release.countDown();
        }
    }

    @Test
    void connectionFailureRemainsExceptional() throws Exception {
        CompletableFuture<ClientResponse> response = call(client, false, false);
        IOException failure = new IOException("connect failed");
        client.connected.completeExceptionally(failure);
        assertSame(failure, assertThrows(ExecutionException.class,
                () -> response.get(5, TimeUnit.SECONDS)).getCause());
        verify(connection, never()).sendRequest(any(), any());
    }

    @Test
    void synchronousSendFailureIsReportedAndConnectionClosedOnOwner() throws Exception {
        IllegalStateException failure = new IllegalStateException("send failed");
        doThrow(failure).when(connection).sendRequest(any(), any());
        CompletableFuture<Thread> closed = new CompletableFuture<>();
        doAnswer(invocation -> { closed.complete(Thread.currentThread()); return null; }).when(connection).close();
        CompletableFuture<ClientResponse> response = call(client, false, false);
        client.connected.complete(connection);
        Throwable cause = assertThrows(ExecutionException.class, () -> response.get(5, TimeUnit.SECONDS)).getCause();
        assertInstanceOf(IOException.class, cause);
        assertSame(failure, cause.getCause());
        assertSame(owner, closed.get(5, TimeUnit.SECONDS));
    }

    @Test
    void dispatchRejectionCompletesFutureInsteadOfHanging() throws Exception {
        XnioIoThread stopped = mock(XnioIoThread.class);
        when(connection.getIoThread()).thenReturn(stopped);
        doThrow(new RejectedExecutionException("stopped")).when(stopped).execute(any(Runnable.class));
        CompletableFuture<ClientResponse> response = call(client, false, false);
        client.connected.complete(connection);
        assertInstanceOf(RejectedExecutionException.class,
                assertThrows(ExecutionException.class, () -> response.get(5, TimeUnit.SECONDS)).getCause());
        verify(connection, never()).sendRequest(any(), any());
    }

    @Test
    void directHelperRunsInlineOnlyOnTheOwningThread() throws Exception {
        CompletableFuture<Void> finished = new CompletableFuture<>();
        owner.execute(() -> {
            try {
                AtomicReference<Thread> operationThread = new AtomicReference<>();
                ClientConnectionOperations.execute(connection, () -> operationThread.set(Thread.currentThread()));
                assertSame(owner, operationThread.get(), "already on owner: execute inline");
                finished.complete(null);
            } catch (Throwable e) { finished.completeExceptionally(e); }
        });
        finished.get(5, TimeUnit.SECONDS);
    }

    @Test
    void anotherIoThreadStillDispatchesToTheConnectionOwner() throws Exception {
        XnioWorker other = Xnio.getInstance().createWorker(OptionMap.create(Options.WORKER_IO_THREADS, 1));
        try {
            CompletableFuture<Thread> operationThread = new CompletableFuture<>();
            other.getIoThread().execute(() -> ClientConnectionOperations.execute(connection,
                    () -> operationThread.complete(Thread.currentThread())));
            assertSame(owner, operationThread.get(5, TimeUnit.SECONDS));
        } finally {
            other.shutdownNow();
            assertTrue(other.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void poolCloseUsesOwnerThread() throws Exception {
        CompletableFuture<Thread> closed = new CompletableFuture<>();
        doAnswer(invocation -> { closed.complete(Thread.currentThread()); return null; }).when(connection).close();
        new com.networknt.client.simplepool.undertow.SimpleUndertowConnection(connection).safeClose();
        assertSame(owner, closed.get(5, TimeUnit.SECONDS));
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    void cancellationOrTimeoutAfterSendWaitsForTransportBeforeHttp1Reuse(boolean timeout) throws Exception {
        pool.cacheConnection(uri, connection);
        pool.resetConnectionStatus(connection);
        CompletableFuture<com.networknt.client.http.Http2ClientCompletableFutureNoRequest> sent = new CompletableFuture<>();
        doAnswer(invocation -> {
            sent.complete(invocation.getArgument(1));
            return null;
        }).when(connection).sendRequest(any(), any());
        CompletableFuture<ClientResponse> response = call(client, false, false);
        var transport = sent.get(5, TimeUnit.SECONDS);
        if (timeout) {
            response.orTimeout(1, TimeUnit.MILLISECONDS);
            assertInstanceOf(TimeoutException.class,
                    assertThrows(ExecutionException.class, () -> response.get(5, TimeUnit.SECONDS)).getCause());
        } else {
            assertTrue(response.cancel(false));
        }
        assertNull(pool.getConnection(uri), "in-flight HTTP/1 exchange must remain reserved");
        CompletableFuture<Void> drained = new CompletableFuture<>();
        owner.execute(() -> {
            transport.complete(new ClientResponse(200, "OK", io.undertow.util.Protocols.HTTP_1_1)); // simulate the body-reader reaching EOF
            drained.complete(null);
        });
        drained.get(5, TimeUnit.SECONDS);
        assertSame(connection, pool.getConnection(uri), "reuse only after transport completion");
        verify(connection, never()).close();
        assertTrue(response.isCompletedExceptionally(), "late transport success must not replace cancellation/timeout");
    }

    private CompletableFuture<ClientResponse> call(TestClient client, boolean body, boolean explicitHttp2) {
        ClientRequest request = new ClientRequest().setMethod(body ? Methods.POST : Methods.GET).setPath("/");
        Optional<String> content = body ? Optional.of("request body") : Optional.empty();
        return explicitHttp2 ? client.callService(uri, request, content, true) : client.callService(uri, request, content);
    }

    private static class TestClient extends Http2Client {
        final CompletableFuture<ClientConnection> connected = new CompletableFuture<>();
        @Override
        public CompletableFuture<ClientConnection> connectAsync(URI target) { return connected; }
        @Override
        public CompletableFuture<ClientConnection> connectAsync(URI target, boolean http2) { return connected; }
    }
}
