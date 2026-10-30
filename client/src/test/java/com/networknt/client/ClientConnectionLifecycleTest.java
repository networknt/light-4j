package com.networknt.client;

import com.networknt.client.http.Http2ClientConnectionPool;
import com.networknt.client.simplepool.undertow.SimpleUndertowConnection;
import io.undertow.client.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.xnio.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientConnectionLifecycleTest {
    private XnioWorker worker;
    private XnioIoThread owner;
    private ClientConnection delegate;
    private ClientConnection guarded;
    private Http2Client client;

    @BeforeAll
    void createClient() { client = new Http2Client(); }

    @AfterAll
    void closeClient() { client.shutdown(); }

    @BeforeEach
    void setUp() throws Exception {
        worker = Xnio.getInstance().createWorker(OptionMap.create(Options.WORKER_IO_THREADS, 1));
        owner = worker.getIoThread();
        delegate = mock(ClientConnection.class);
        when(delegate.getIoThread()).thenReturn(owner);
        when(delegate.isOpen()).thenReturn(true);
        guarded = (ClientConnection) new SimpleUndertowConnection(delegate).getRawConnection();
        Http2ClientConnectionPool.getInstance().clear();
    }

    @AfterEach
    void tearDown() throws Exception {
        Http2ClientConnectionPool.getInstance().clear();
        worker.shutdownNow();
        assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void closeImmediatelyRetiresPooledConnectionWhilePhysicalCloseIsQueued() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        owner.execute(() -> {
            entered.countDown();
            try { release.await(5, TimeUnit.SECONDS); }
            catch (InterruptedException _) { Thread.currentThread().interrupt(); }
        });
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        try {
            SimpleUndertowConnection pooled = new SimpleUndertowConnection(delegate);
            ClientConnection returned = (ClientConnection) pooled.getRawConnection();
            pooled.safeClose();
            assertFalse(pooled.isOpen(), "pool must stop lending before owner processes close");
            assertFalse(returned.isOpen(), "borrowed connection must expose immediate retirement");
            verify(delegate, never()).close();
        } finally { release.countDown(); }
    }

    @Test
    void safeCloseDoesNotThrowWhenOwnerRejectsDispatch() {
        XnioIoThread stopped = mock(XnioIoThread.class);
        when(delegate.getIoThread()).thenReturn(stopped);
        doThrow(new RejectedExecutionException("stopped")).when(stopped).execute(any(Runnable.class));
        SimpleUndertowConnection pooled = new SimpleUndertowConnection(delegate);
        assertDoesNotThrow(pooled::safeClose);
        assertFalse(pooled.isOpen());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void throwingCompletedDoesNotCauseASecondTerminalCallback(boolean error) {
        Throwable problem = error ? new AssertionError("callback failed") : new IllegalStateException("callback failed");
        RecordingCallback callback = new RecordingCallback(problem);
        doAnswer(invocation -> {
            invocation.<ClientCallback<ClientExchange>>getArgument(1).completed(mock(ClientExchange.class));
            return null;
        }).when(delegate).sendRequest(any(), any());
        owner.execute(() -> guarded.sendRequest(new ClientRequest(), callback));
        ExecutionException failure = assertThrows(ExecutionException.class, () -> callback.get(2, TimeUnit.SECONDS));
        assertSame(problem, failure.getCause());
        assertEquals(1, callback.completed.get());
        assertEquals(0, callback.failed.get(), "completed must not be followed by failed on the same callback");
    }

    @Test
    void errorBeforeCallbackIsDeliveredAsOneFailure() {
        AssertionError problem = new AssertionError("send failed");
        doThrow(problem).when(delegate).sendRequest(any(), any());
        RecordingCallback callback = new RecordingCallback(null);
        guarded.sendRequest(new ClientRequest(), callback);
        Throwable failure = assertThrows(ExecutionException.class, () -> callback.get(2, TimeUnit.SECONDS)).getCause();
        assertSame(problem, failure.getCause());
        assertEquals(0, callback.completed.get());
        assertEquals(1, callback.failed.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void exchangeFailureRetiresHttp1ButKeepsMultiplexedConnection(boolean multiplexed) throws Exception {
        when(delegate.isMultiplexingSupported()).thenReturn(multiplexed);
        IOException original = new IOException("exchange failed");
        CompletableFuture<Void> delivered = new CompletableFuture<>();
        doAnswer(invocation -> {
            invocation.<ClientCallback<ClientExchange>>getArgument(1).failed(original);
            delivered.complete(null);
            return null;
        }).when(delegate).sendRequest(any(), any());
        URI uri = URI.create("http://review-failure.invalid");
        var pool = Http2ClientConnectionPool.getInstance();
        pool.cacheConnection(uri, delegate);
        pool.resetConnectionStatus(delegate);
        var response = client.callService(uri, new ClientRequest(), Optional.empty());
        assertSame(original, assertThrows(ExecutionException.class, () -> response.get(5, TimeUnit.SECONDS)).getCause());
        delivered.get(5, TimeUnit.SECONDS);
        if (multiplexed) {
            assertNotNull(pool.getConnection(uri));
            verify(delegate, never()).close();
        } else {
            assertNull(pool.getConnection(uri));
            verify(delegate).close();
        }
    }

    @ParameterizedTest
    @CsvSource({"string,false", "string,true", "stringBody,false", "stringBody,true",
                "bytes,false", "bytes,true", "bytesBody,false", "bytesBody,true",
                "full,false", "full,true", "fullBody,false", "fullBody,true"})
    void frameworkCallbacksReleaseWaitersWhenTheirSetupThrows(String kind, boolean responseStage) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ClientResponse> response = new AtomicReference<>();
        AtomicReference<AsyncResult<AsyncResponse>> full = new AtomicReference<>();
        ClientCallback<ClientExchange> callback = switch (kind) {
            case "string" -> client.createClientCallback(response, latch);
            case "stringBody" -> client.createClientCallback(response, latch, "body");
            case "bytes" -> client.byteBufferClientCallback(response, latch);
            case "bytesBody" -> client.byteBufferClientCallback(response, latch, ByteBuffer.wrap(new byte[]{1}));
            case "full" -> client.createFullCallback(full, latch);
            default -> client.createFullCallback(full, latch, "body");
        };
        ClientExchange exchange = mock(ClientExchange.class);
        AssertionError original = new AssertionError("channel setup failed");
        CompletableFuture<ClientCallback<ClientExchange>> responseListener = new CompletableFuture<>();
        doAnswer(invocation -> { responseListener.complete(invocation.getArgument(0)); return null; })
                .when(exchange).setResponseListener(any());
        if (responseStage) {
            StreamSinkChannel requestChannel = mock(StreamSinkChannel.class);
            when(requestChannel.flush()).thenReturn(true);
            when(requestChannel.write(any(ByteBuffer.class))).thenAnswer(invocation -> {
                ByteBuffer data = invocation.getArgument(0);
                int size = data.remaining(); data.position(data.limit()); return size;
            });
            when(exchange.getRequestChannel()).thenReturn(requestChannel);
            when(delegate.getBufferPool()).thenReturn(Http2Client.BUFFER_POOL);
            when(exchange.getResponse()).thenReturn(new ClientResponse(200, "OK", io.undertow.util.Protocols.HTTP_1_1));
            doThrow(original).when(exchange).getResponseChannel();
        } else doThrow(original).when(exchange).getRequestChannel();
        doAnswer(invocation -> {
            invocation.<ClientCallback<ClientExchange>>getArgument(1).completed(exchange);
            return null;
        }).when(delegate).sendRequest(any(), any());
        guarded.sendRequest(new ClientRequest(), callback);
        if (responseStage) {
            ClientCallback<ClientExchange> listener = responseListener.get(5, TimeUnit.SECONDS);
            owner.execute(() -> listener.completed(exchange));
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "callback errors must not strand latch-based callers");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void stoppedOwnerDoesNotReplaceAnExchangeOutcome(boolean failed) throws Exception {
        URI uri = URI.create("http://review-cleanup.invalid");
        var pool = Http2ClientConnectionPool.getInstance();
        pool.cacheConnection(uri, delegate);
        pool.resetConnectionStatus(delegate);
        ClientExchange exchange = mock(ClientExchange.class);
        StreamSinkChannel requestChannel = mock(StreamSinkChannel.class);
        when(requestChannel.flush()).thenReturn(true);
        when(exchange.getRequestChannel()).thenReturn(requestChannel);
        when(delegate.getBufferPool()).thenReturn(Http2Client.BUFFER_POOL);
        StreamSourceChannel channel = mock(StreamSourceChannel.class);
        XnioIoThread stopped = mock(XnioIoThread.class);
        doThrow(new RejectedExecutionException("stopped")).when(stopped).execute(any(Runnable.class));
        when(channel.read(any(ByteBuffer.class))).thenAnswer(invocation -> {
            when(delegate.getIoThread()).thenReturn(stopped);
            return -1;
        });
        when(exchange.getResponseChannel()).thenReturn(channel);
        ClientResponse expected = new ClientResponse(200, "OK", io.undertow.util.Protocols.HTTP_1_1);
        when(exchange.getResponse()).thenReturn(expected);
        CompletableFuture<ClientCallback<ClientExchange>> listenerFuture = new CompletableFuture<>();
        doAnswer(invocation -> { listenerFuture.complete(invocation.getArgument(0)); return null; })
                .when(exchange).setResponseListener(any());
        doAnswer(invocation -> {
            invocation.<ClientCallback<ClientExchange>>getArgument(1).completed(exchange); return null;
        }).when(delegate).sendRequest(any(), any());
        var response = client.callService(uri, new ClientRequest(), Optional.empty());
        ClientCallback<ClientExchange> listener = listenerFuture.get(5, TimeUnit.SECONDS);
        CompletableFuture<Void> setupDone = new CompletableFuture<>();
        owner.execute(() -> setupDone.complete(null));
        setupDone.get(5, TimeUnit.SECONDS);
        if (failed) {
            when(delegate.getIoThread()).thenReturn(stopped);
            IOException original = new IOException("response failed");
            listener.failed(original);
            assertSame(original, assertThrows(ExecutionException.class, () -> response.get(5, TimeUnit.SECONDS)).getCause());
            assertNull(pool.getConnection(uri));
        } else {
            owner.execute(() -> listener.completed(exchange));
            assertSame(expected, response.get(5, TimeUnit.SECONDS));
            assertNotNull(pool.getConnection(uri), "pool bookkeeping must still complete without owner dispatch");
        }
    }

    private static class RecordingCallback extends CompletableFuture<ClientExchange> implements ClientCallback<ClientExchange> {
        final AtomicInteger completed = new AtomicInteger();
        final AtomicInteger failed = new AtomicInteger();
        final Throwable problem;
        RecordingCallback(Throwable problem) { this.problem = problem; }
        public void completed(ClientExchange result) {
            completed.incrementAndGet();
            if (problem instanceof Error error) throw error;
            if (problem instanceof RuntimeException exception) throw exception;
            complete(result);
        }
        public void failed(IOException exception) { failed.incrementAndGet(); completeExceptionally(exception); }
    }
}
