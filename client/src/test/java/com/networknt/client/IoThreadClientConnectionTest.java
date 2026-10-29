package com.networknt.client;

import com.networknt.client.http.*;
import io.undertow.client.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xnio.*;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IoThreadClientConnectionTest {
    private XnioWorker worker;
    private XnioIoThread owner;
    private ClientConnection nativeConnection;
    private IoThreadClientConnection connection;
    private final AtomicBoolean open = new AtomicBoolean(true);

    @BeforeEach void setUp() throws Exception {
        worker = Xnio.getInstance().createWorker(OptionMap.create(Options.WORKER_IO_THREADS, 1));
        owner = worker.getIoThread();
        nativeConnection = mock(ClientConnection.class);
        when(nativeConnection.getIoThread()).thenReturn(owner);
        when(nativeConnection.isOpen()).thenAnswer(ignored -> open.get());
        doAnswer(ignored -> { open.set(false); return null; }).when(nativeConnection).close();
        connection = IoThreadClientConnection.wrap(nativeConnection);
    }

    @AfterEach void tearDown() throws Exception {
        connection.close();
        Http2ClientConnectionPool.getInstance().clear();
        worker.shutdownNow();
        assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test void repeatedWrappingPreservesIdentityAndCloseState() {
        ClientConnection second = IoThreadClientConnection.wrap(nativeConnection);
        assertSame(connection, second);
        assertEquals(1, new HashSet<>(List.of(connection, second)).size());
        Map<ClientConnection, String> tokens = new HashMap<>();
        tokens.put(connection, "lease");
        assertEquals("lease", tokens.get(second));
        connection.close();
        assertFalse(second.isOpen());
        assertSame(second, IoThreadClientConnection.wrap(nativeConnection));
    }

    @Test void closeListenersSupportRemovalLateRegistrationAndReentry() throws Exception {
        AtomicInteger removed = new AtomicInteger();
        AtomicInteger notified = new AtomicInteger();
        connection.getCloseSetter().set(ignored -> removed.incrementAndGet());
        connection.getCloseSetter().set(null);
        connection.addCloseListener(closed -> {
            assertSame(connection, closed);
            try {
                CompletableFuture.runAsync(() -> connection.getCloseSetter().set(null)).get(2, TimeUnit.SECONDS);
                notified.incrementAndGet();
            } catch (Exception error) { throw new AssertionError(error); }
        });
        connection.close();
        drainOwner();
        assertEquals(0, removed.get());
        assertEquals(1, notified.get());
        connection.addCloseListener(ignored -> notified.incrementAndGet());
        connection.getCloseSetter().set(ignored -> notified.incrementAndGet());
        assertEquals(3, notified.get(), "late listeners observe an already-closed connection");
        connection.close();
        assertEquals(3, notified.get());
    }

    @Test void closeListenerRegistrationSurvivesStoppedOwner() {
        XnioIoThread stopped = mock(XnioIoThread.class);
        doThrow(new RejectedExecutionException("stopped")).when(stopped).execute(any(Runnable.class));
        when(nativeConnection.getIoThread()).thenReturn(stopped);
        AtomicInteger notifications = new AtomicInteger();
        assertDoesNotThrow(() -> connection.addCloseListener(ignored -> notifications.incrementAndGet()));
        assertDoesNotThrow(() -> connection.getCloseSetter().set(ignored -> notifications.incrementAndGet()));
        assertDoesNotThrow(connection::close);
        assertEquals(2, notifications.get());
    }

    @Test void pingUsesTheOwnerAndForwardsAcknowledgement() throws Exception {
        CompletableFuture<Thread> pingThread = new CompletableFuture<>();
        doAnswer(invocation -> {
            pingThread.complete(Thread.currentThread());
            invocation.<ClientConnection.PingListener>getArgument(0).acknowledged(); return null;
        }).when(nativeConnection).sendPing(any(), anyLong(), any());
        CompletableFuture<Void> acknowledged = new CompletableFuture<>();
        connection.sendPing(new ClientConnection.PingListener() {
            public void acknowledged() { acknowledged.complete(null); }
            public void failed(IOException error) { acknowledged.completeExceptionally(error); }
        }, 1, TimeUnit.SECONDS);
        acknowledged.get(5, TimeUnit.SECONDS);
        assertSame(owner, pingThread.get(5, TimeUnit.SECONDS));
    }

    @ParameterizedTest @ValueSource(strings = {"supports", "get", "set", "upgrade", "request", "response"})
    void synchronousIoOperationsRejectNonOwnerCalls(String operation) throws Exception {
        ClientExchange exchange = mock(ClientExchange.class);
        CompletableFuture<ClientExchange> exchangeFuture = new CompletableFuture<>();
        doAnswer(invocation -> {
            invocation.<ClientCallback<ClientExchange>>getArgument(1).completed(exchange); return null;
        }).when(nativeConnection).sendRequest(any(), any());
        connection.sendRequest(new ClientRequest(), new ClientCallback<>() {
            public void completed(ClientExchange result) { exchangeFuture.complete(result); }
            public void failed(IOException error) { exchangeFuture.completeExceptionally(error); }
        });
        ClientExchange owned = exchangeFuture.get(5, TimeUnit.SECONDS);
        assertThrows(IllegalStateException.class, () -> {
            switch (operation) {
                case "supports" -> connection.supportsOption(Options.READ_TIMEOUT);
                case "get" -> connection.getOption(Options.READ_TIMEOUT);
                case "set" -> connection.setOption(Options.READ_TIMEOUT, 1);
                case "upgrade" -> connection.performUpgrade();
                case "request" -> owned.getRequestChannel();
                default -> owned.getResponseChannel();
            }
        });
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void customCallbackExceptionsRemainObservableWithoutSecondNotification(boolean error) throws Exception {
        Throwable original = error ? new AssertionError("custom callback") : new IllegalStateException("custom callback");
        AtomicInteger failed = new AtomicInteger();
        doAnswer(invocation -> {
            invocation.<ClientCallback<ClientExchange>>getArgument(1).completed(mock(ClientExchange.class)); return null;
        }).when(nativeConnection).sendRequest(any(), any());
        CompletableFuture<Throwable> propagated = new CompletableFuture<>();
        owner.execute(() -> {
            try {
                connection.sendRequest(new ClientRequest(), new ClientCallback<>() {
                    public void completed(ClientExchange result) {
                        if (original instanceof Error fatal) throw fatal;
                        throw (RuntimeException) original;
                    }
                    public void failed(IOException ignored) { failed.incrementAndGet(); }
                });
                propagated.complete(null);
            } catch (Throwable thrown) { propagated.complete(thrown); }
        });
        assertSame(original, propagated.get(5, TimeUnit.SECONDS));
        assertEquals(0, failed.get());
        assertFalse(connection.isOpen());
    }

    @Test void frameworkSetupFailureQuarantinesStreamBeforeReleasingWaiter() throws Exception {
        when(nativeConnection.isMultiplexingSupported()).thenReturn(true);
        ClientExchange raw = mock(ClientExchange.class);
        StreamSinkChannel request = mock(StreamSinkChannel.class);
        StreamSourceChannel response = mock(StreamSourceChannel.class);
        when(raw.getRequestChannel()).thenReturn(request);
        when(raw.getResponseChannel()).thenReturn(response);
        AtomicBoolean requestClosed = new AtomicBoolean();
        AtomicBoolean responseClosed = new AtomicBoolean();
        doAnswer(ignored -> { requestClosed.set(true); return null; }).when(request).close();
        doAnswer(ignored -> { responseClosed.set(true); return null; }).when(response).close();
        AtomicReference<ClientCallback<ClientExchange>> late = new AtomicReference<>();
        doAnswer(invocation -> { late.set(invocation.getArgument(0)); return null; }).when(raw).setResponseListener(any());
        AtomicInteger successes = new AtomicInteger();
        CompletableFuture<Boolean> failed = new CompletableFuture<>();
        doAnswer(invocation -> {
            invocation.<ClientCallback<ClientExchange>>getArgument(1).completed(raw); return null;
        }).when(nativeConnection).sendRequest(any(), any());
        connection.sendRequest(new ClientRequest(), new SafeClientCallback() {
            protected void completedSafely(ClientExchange exchange) {
                exchange.setResponseListener(new ClientCallback<>() {
                    public void completed(ClientExchange result) { successes.incrementAndGet(); }
                    public void failed(IOException error) { }
                });
                throw new IllegalStateException("partial setup");
            }
            protected void failedSafely(IOException error) { failed.complete(requestClosed.get() && responseClosed.get()); }
        });
        assertTrue(failed.get(5, TimeUnit.SECONDS), "cleanup must precede application completion");
        owner.execute(() -> late.get().completed(raw));
        drainOwner();
        assertEquals(0, successes.get());
        assertTrue(connection.isOpen(), "only the failed HTTP/2 stream should be retired");
    }

    @Test void everyNewMultiplexedConnectionIsTrackedAndDuplicateInsertionIsIgnored() throws Exception {
        when(nativeConnection.isMultiplexingSupported()).thenReturn(true);
        ClientConnection secondRaw = mock(ClientConnection.class);
        when(secondRaw.getIoThread()).thenReturn(owner);
        when(secondRaw.isOpen()).thenReturn(true);
        when(secondRaw.isMultiplexingSupported()).thenReturn(true);
        ClientConnection second = IoThreadClientConnection.wrap(secondRaw);
        var pool = Http2ClientConnectionPool.getInstance();
        URI uri = URI.create("http://multiplex-tracking.invalid");
        pool.cacheConnection(uri, connection);
        pool.cacheConnection(uri, second);
        pool.cacheConnection(uri, nativeConnection);
        assertEquals(2, pool.numberOfConnections());
        connection.close();
        assertSame(second, pool.getConnection(uri));
        second.close();
        drainOwner();
        assertNull(pool.getConnection(uri));
        verify(nativeConnection).close();
        verify(secondRaw).close();
    }

    @Test void nativeCloseEventUsesCanonicalIdentityAndNotifiesOnce() throws Exception {
        CompletableFuture<ChannelListener<ClientConnection>> bridge = new CompletableFuture<>();
        doAnswer(invocation -> { bridge.complete(invocation.getArgument(0)); return null; })
                .when(nativeConnection).addCloseListener(any());
        AtomicInteger count = new AtomicInteger();
        connection.addCloseListener(closed -> { assertSame(connection, closed); count.incrementAndGet(); });
        ChannelListener<ClientConnection> listener = bridge.get(5, TimeUnit.SECONDS);
        onOwner(() -> { open.set(false); listener.handleEvent(nativeConnection); listener.handleEvent(nativeConnection); return null; });
        assertFalse(connection.isOpen());
        assertEquals(1, count.get());
        connection.getCloseSetter().set(closed -> count.incrementAndGet());
        assertEquals(2, count.get());
    }

    @Test void registrationBeforeOwnerObservesNativeCloseIsNotLost() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        owner.execute(() -> {
            entered.countDown();
            try { release.await(5, TimeUnit.SECONDS); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); }
        });
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        AtomicInteger count = new AtomicInteger();
        try {
            connection.addCloseListener(closed -> count.incrementAndGet());
            open.set(false); // transport shutdown before queued native-listener installation
        } finally { release.countDown(); }
        drainOwner();
        assertEquals(1, count.get());
    }

    @Test void rejectedPingNotifiesFailureExactlyOnce() throws Exception {
        XnioIoThread stopped = mock(XnioIoThread.class);
        doThrow(new RejectedExecutionException("stopped")).when(stopped).execute(any(Runnable.class));
        when(nativeConnection.getIoThread()).thenReturn(stopped);
        AtomicInteger calls = new AtomicInteger();
        CompletableFuture<IOException> failure = new CompletableFuture<>();
        connection.sendPing(new ClientConnection.PingListener() {
            public void acknowledged() { fail("ping must not be acknowledged"); }
            public void failed(IOException error) { calls.incrementAndGet(); failure.complete(error); }
        }, 1, TimeUnit.SECONDS);
        assertInstanceOf(RejectedExecutionException.class, failure.get(5, TimeUnit.SECONDS).getCause());
        assertEquals(1, calls.get());
        verify(nativeConnection, never()).sendPing(any(), anyLong(), any());
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void fatalErrorsAreSignalledThenRethrown(boolean inCallback) throws Exception {
        OutOfMemoryError original = new OutOfMemoryError("synthetic fatal error");
        FatalCallback callback = new FatalCallback(inCallback ? original : null);
        if (inCallback) doAnswer(invocation -> {
            invocation.<ClientCallback<ClientExchange>>getArgument(1).completed(mock(ClientExchange.class)); return null;
        }).when(nativeConnection).sendRequest(any(), any());
        else doThrow(original).when(nativeConnection).sendRequest(any(), any());
        Throwable propagated = onOwner(() -> {
            try { connection.sendRequest(new ClientRequest(), callback); return null; }
            catch (Throwable error) { return error; }
        });
        assertSame(original, propagated);
        Throwable signalled = assertThrows(ExecutionException.class, () -> callback.get(5, TimeUnit.SECONDS)).getCause();
        assertSame(original, inCallback ? signalled : signalled.getCause());
        assertEquals(inCallback ? 0 : 1, callback.failed.get());
    }

    @Test void ownerOperationsAndContinuePushHandlersKeepExchangeIdentity() throws Exception {
        ClientExchange raw = mock(ClientExchange.class);
        ClientExchange pushed = mock(ClientExchange.class);
        ClientRequest initialRequest = new ClientRequest();
        ClientRequest pushedRequest = new ClientRequest();
        when(raw.getRequest()).thenReturn(initialRequest);
        when(pushed.getRequest()).thenReturn(pushedRequest);
        StreamSinkChannel requestChannel = mock(StreamSinkChannel.class);
        StreamSourceChannel responseChannel = mock(StreamSourceChannel.class);
        when(raw.getRequestChannel()).thenReturn(requestChannel);
        when(raw.getResponseChannel()).thenReturn(responseChannel);
        AtomicReference<ContinueNotification> continued = new AtomicReference<>();
        AtomicReference<PushCallback> push = new AtomicReference<>();
        doAnswer(invocation -> { continued.set(invocation.getArgument(0)); return null; }).when(raw).setContinueHandler(any());
        doAnswer(invocation -> { push.set(invocation.getArgument(0)); return null; }).when(raw).setPushHandler(any());
        CompletableFuture<ClientExchange> published = new CompletableFuture<>();
        doAnswer(invocation -> {
            invocation.<ClientCallback<ClientExchange>>getArgument(1).completed(raw); return null;
        }).when(nativeConnection).sendRequest(any(), any());
        connection.sendRequest(initialRequest, new ClientCallback<>() {
            public void completed(ClientExchange exchange) { published.complete(exchange); }
            public void failed(IOException error) { published.completeExceptionally(error); }
        });
        ClientExchange owned = published.get(5, TimeUnit.SECONDS);
        when(nativeConnection.supportsOption(Options.READ_TIMEOUT)).thenReturn(true);
        when(nativeConnection.getOption(Options.READ_TIMEOUT)).thenReturn(7);
        when(nativeConnection.setOption(Options.READ_TIMEOUT, 8)).thenReturn(7);
        StreamConnection upgraded = mock(StreamConnection.class);
        when(nativeConnection.performUpgrade()).thenReturn(upgraded);
        AtomicInteger callbacks = new AtomicInteger();
        onOwner(() -> {
            assertTrue(connection.supportsOption(Options.READ_TIMEOUT));
            assertEquals(7, connection.getOption(Options.READ_TIMEOUT));
            assertEquals(7, connection.setOption(Options.READ_TIMEOUT, 8));
            assertSame(upgraded, connection.performUpgrade());
            assertSame(requestChannel, owned.getRequestChannel());
            assertSame(responseChannel, owned.getResponseChannel());
            owned.setContinueHandler(exchange -> {
                assertSame(connection, exchange.getConnection()); assertSame(initialRequest, exchange.getRequest()); callbacks.incrementAndGet();
            });
            owned.setPushHandler((original, promised) -> {
                assertSame(connection, original.getConnection()); assertSame(connection, promised.getConnection());
                assertSame(initialRequest, original.getRequest()); assertSame(pushedRequest, promised.getRequest());
                callbacks.incrementAndGet(); return true;
            });
            continued.get().handleContinue(raw);
            assertTrue(push.get().handlePush(raw, pushed));
            owned.setContinueHandler(null); owned.setPushHandler(null);
            assertNull(continued.get()); assertNull(push.get());
            return null;
        });
        assertEquals(2, callbacks.get());
    }

    private <T> T onOwner(Callable<T> operation) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        owner.execute(() -> {
            try { result.complete(operation.call()); }
            catch (Throwable error) { result.completeExceptionally(error); }
        });
        return result.get(5, TimeUnit.SECONDS);
    }

    private static final class FatalCallback extends CompletableFuture<ClientExchange> implements ClientCallback<ClientExchange> {
        final Error error;
        final AtomicInteger failed = new AtomicInteger();
        FatalCallback(Error error) { this.error = error; }
        public void completed(ClientExchange exchange) { if (error != null) throw error; complete(exchange); }
        public void failed(IOException error) { failed.incrementAndGet(); completeExceptionally(error); }
    }

    private void drainOwner() throws Exception {
        CompletableFuture<Void> drained = new CompletableFuture<>();
        owner.execute(() -> drained.complete(null));
        drained.get(5, TimeUnit.SECONDS);
    }
}
