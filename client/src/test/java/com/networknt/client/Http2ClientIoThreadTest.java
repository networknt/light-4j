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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Http2ClientIoThreadTest {
    private XnioWorker worker;
    private XnioIoThread owner;
    private ClientConnection connection;
    private TestClient client;
    private final Http2ClientConnectionPool pool = Http2ClientConnectionPool.getInstance();
    private final URI uri = URI.create("http://io-thread-test.invalid");

    @BeforeAll
    void createClient() { client = new TestClient(); }

    @AfterAll
    void closeClient() { client.shutdown(); }

    @BeforeEach
    void setUp() throws Exception {
        worker = Xnio.getInstance().createWorker(OptionMap.create(Options.WORKER_IO_THREADS, 1));
        owner = worker.getIoThread();
        connection = mock(ClientConnection.class);
        when(connection.getIoThread()).thenReturn(owner);
        when(connection.isOpen()).thenReturn(true);
        pool.clear();
        client.connected = new CompletableFuture<>();
        com.networknt.client.http.IoThreadClientConnection.wrap(connection);
        drainOwner();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        pool.clear();
        worker.shutdownNow();
        assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
    }

    @ParameterizedTest
    @CsvSource({"false,false,false", "false,false,true", "false,true,false", "false,true,true",
                "true,false,false", "true,false,true", "true,true,false", "true,true,true"})
    void sendsOnOwnerForWarmAndColdCalls(boolean warm, boolean body, boolean explicitHttp2) {
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
    @CsvSource({"false,false,false", "false,true,false", "true,false,false", "true,true,false",
                "false,false,true", "false,true,true", "true,false,true", "true,true,true"})
    void cancellationBeforeDispatchSkipsSend(boolean warm, boolean body, boolean timeout) throws Exception {
        CountDownLatch blocked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        owner.execute(() -> {
            blocked.countDown();
            try { release.await(5, TimeUnit.SECONDS); }
            catch (InterruptedException _) { Thread.currentThread().interrupt(); }
        });
        assertTrue(blocked.await(5, TimeUnit.SECONDS));
        try {
            if (warm) {
                pool.cacheConnection(uri, connection);
                pool.resetConnectionStatus(connection);
            }
            CompletableFuture<ClientResponse> response = call(client, body, false);
            if (timeout) response.completeExceptionally(new TimeoutException("before send"));
            else assertTrue(response.cancel(false));
            if (!warm) {
                pool.cacheConnection(uri, connection);
                client.connected.complete(connection);
            }
            release.countDown();
            CompletableFuture<Void> drained = new CompletableFuture<>();
            owner.execute(() -> drained.complete(null));
            drained.get(5, TimeUnit.SECONDS);
            verify(connection, never()).sendRequest(any(), any());
            assertTrue(response.isCompletedExceptionally());
            verify(connection, never()).close();
            assertSame(com.networknt.client.http.IoThreadClientConnection.wrap(connection), pool.getConnection(uri));
            pool.resetConnectionStatus(connection);
        } finally {
            release.countDown();
        }
    }

    @Test
    void connectionFailureRemainsExceptional() {
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
    void dispatchRejectionCompletesFutureInsteadOfHanging() {
        XnioIoThread stopped = mock(XnioIoThread.class);
        when(connection.getIoThread()).thenReturn(stopped);
        doThrow(new RejectedExecutionException("stopped")).when(stopped).execute(any(Runnable.class));
        CompletableFuture<ClientResponse> response = call(client, false, false);
        client.connected.complete(connection);
        Throwable failure = assertThrows(ExecutionException.class, () -> response.get(5, TimeUnit.SECONDS)).getCause();
        assertInstanceOf(IOException.class, failure);
        assertInstanceOf(RejectedExecutionException.class, failure.getCause());
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
    void cancellationOrTimeoutAfterSendRetiresHttp1WithoutWaitingForBackend(boolean timeout) throws Exception {
        pool.cacheConnection(uri, connection);
        pool.resetConnectionStatus(connection);
        CompletableFuture<ClientCallback<ClientExchange>> sent = new CompletableFuture<>();
        CompletableFuture<Thread> closed = new CompletableFuture<>();
        doAnswer(invocation -> { closed.complete(Thread.currentThread()); return null; }).when(connection).close();
        doAnswer(invocation -> { sent.complete(invocation.getArgument(1)); return null; })
                .when(connection).sendRequest(any(), any());
        CompletableFuture<ClientResponse> response = call(client, false, false);
        sent.get(5, TimeUnit.SECONDS);
        if (timeout) {
            response.orTimeout(1, TimeUnit.MILLISECONDS);
            assertInstanceOf(TimeoutException.class,
                    assertThrows(ExecutionException.class, () -> response.get(5, TimeUnit.SECONDS)).getCause());
        } else assertTrue(response.cancel(false));
        assertSame(owner, closed.get(5, TimeUnit.SECONDS));
        assertNull(pool.getConnection(uri), "a backend that never answers must not pin a cancelled HTTP/1 connection");
        assertTrue(response.isCompletedExceptionally());
    }

    private CompletableFuture<ClientResponse> call(TestClient client, boolean body, boolean explicitHttp2) {
        ClientRequest request = new ClientRequest().setMethod(body ? Methods.POST : Methods.GET).setPath("/");
        Optional<String> content = body ? Optional.of("request body") : Optional.empty();
        return explicitHttp2 ? client.callService(uri, request, content, true) : client.callService(uri, request, content);
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void multiplexedCancellationResetsOnlyItsStream(boolean lateExchange, boolean timeout) throws Exception {
        when(connection.isMultiplexingSupported()).thenReturn(true);
        when(connection.getBufferPool()).thenReturn(Http2Client.BUFFER_POOL);
        ClientExchange raw = mock(ClientExchange.class);
        org.xnio.channels.StreamSinkChannel request = mock(org.xnio.channels.StreamSinkChannel.class);
        org.xnio.channels.StreamSourceChannel body = mock(org.xnio.channels.StreamSourceChannel.class);
        CompletableFuture<Void> requestClosed = new CompletableFuture<>();
        CompletableFuture<Void> responseClosed = new CompletableFuture<>();
        doAnswer(ignored -> { requestClosed.complete(null); return null; }).when(request).close();
        doAnswer(ignored -> { responseClosed.complete(null); return null; }).when(body).close();
        when(request.flush()).thenReturn(true);
        when(raw.getRequestChannel()).thenReturn(request);
        when(raw.getResponseChannel()).thenReturn(body);
        when(raw.getResponse()).thenReturn(new ClientResponse(200, "OK", io.undertow.util.Protocols.HTTP_2_0));
        ChannelListener.SimpleSetter<org.xnio.channels.StreamSourceChannel> reads = new ChannelListener.SimpleSetter<>();
        doReturn(reads).when(body).getReadSetter();
        CompletableFuture<ClientCallback<ClientExchange>> sent = new CompletableFuture<>();
        CompletableFuture<ClientCallback<ClientExchange>> responseListener = new CompletableFuture<>();
        doAnswer(invocation -> { responseListener.complete(invocation.getArgument(0)); return null; }).when(raw).setResponseListener(any());
        doAnswer(invocation -> {
            ClientCallback<ClientExchange> callback = invocation.getArgument(1);
            sent.complete(callback);
            if (!lateExchange) callback.completed(raw);
            return null;
        }).when(connection).sendRequest(any(), any());
        pool.cacheConnection(uri, connection);
        pool.resetConnectionStatus(connection);
        CompletableFuture<ClientResponse> response = call(client, false, false);
        ClientCallback<ClientExchange> sendCallback = sent.get(5, TimeUnit.SECONDS);
        if (!lateExchange) {
            ClientCallback<ClientExchange> listener = responseListener.get(5, TimeUnit.SECONDS);
            owner.execute(() -> listener.completed(raw));
            drainOwner();
            assertNotNull(reads.get(), "the response reader must be installed before cancellation");
        }
        if (timeout) response.completeExceptionally(new TimeoutException("caller timeout"));
        else assertTrue(response.cancel(false));
        if (lateExchange) owner.execute(() -> sendCallback.completed(raw));
        CompletableFuture.allOf(requestClosed, responseClosed).get(5, TimeUnit.SECONDS);
        drainOwner();
        verify(request).close();
        verify(body).close();
        assertNull(reads.get(), "cancelled streams must release the body reader");
        verify(connection, never()).close();
        assertTrue(com.networknt.client.http.IoThreadClientConnection.wrap(connection).isOpen());
        assertNotNull(pool.getConnection(uri), "other requests can still borrow the HTTP/2 transport");
        pool.resetConnectionStatus(connection);
    }

    private void drainOwner() throws Exception {
        CompletableFuture<Void> drained = new CompletableFuture<>();
        owner.execute(() -> drained.complete(null));
        drained.get(5, TimeUnit.SECONDS);
    }

    private static class TestClient extends Http2Client {
        CompletableFuture<ClientConnection> connected = new CompletableFuture<>();
        @Override
        public CompletableFuture<ClientConnection> connectAsync(URI target) { return connected; }
        @Override
        public CompletableFuture<ClientConnection> connectAsync(URI target, boolean http2) { return connected; }
    }
}
