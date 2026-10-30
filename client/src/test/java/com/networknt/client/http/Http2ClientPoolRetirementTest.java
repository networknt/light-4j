package com.networknt.client.http;

import com.networknt.client.ClientConfig;
import com.networknt.client.RequestConfig;
import io.undertow.client.ClientConnection;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.networknt.config.Config;
import java.lang.reflect.Field;
import org.xnio.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Http2ClientPoolRetirementTest {
    private XnioWorker worker;
    private XnioIoThread owner;
    private Http2ClientConnectionPool pool;
    private final AtomicLong clock = new AtomicLong();
    private final List<ClientConnection> transports = new ArrayList<>();

    @BeforeEach void setUp() throws Exception {
        worker = Xnio.getInstance().createWorker(OptionMap.create(Options.WORKER_IO_THREADS, 1));
        owner = worker.getIoThread();
        RequestConfig request = Config.getInstance().getMapper().convertValue(Map.of(
                ClientConfig.CONNECTION_POOL_SIZE, 2,
                ClientConfig.MAX_CONNECTION_NUM_PER_HOST, 4,
                ClientConfig.MIN_CONNECTION_NUM_PER_HOST, 1,
                ClientConfig.MAX_REQ_PER_CONN, -1,
                ClientConfig.CONNECTION_EXPIRE_TIME, 30000), RequestConfig.class);
        pool = new Http2ClientConnectionPool(request, clock::get);
    }
    @AfterEach void tearDown() throws Exception {
        if (pool != null) pool.clear();
        transports.forEach(connection -> IoThreadClientConnection.wrap(connection).close());
        drainOwner();
        worker.shutdownNow();
        assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
    }
    private ClientConnection transport() throws Exception {
        ClientConnection raw = mock(ClientConnection.class);
        transports.add(raw);
        AtomicBoolean open = new AtomicBoolean(true);
        when(raw.getIoThread()).thenReturn(owner);
        when(raw.isOpen()).thenAnswer(ignored -> open.get());
        when(raw.isMultiplexingSupported()).thenReturn(true);
        doAnswer(ignored -> { open.set(false); return null; }).when(raw).close();
        IoThreadClientConnection.wrap(raw);
        drainOwner();
        return raw;
    }
    private void drainOwner() throws Exception {
        CompletableFuture<Void> drained = new CompletableFuture<>();
        owner.execute(() -> drained.complete(null));
        drained.get(5, TimeUnit.SECONDS);
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void lruEvictionClosesIdleOrDrainsBorrowedTransport(boolean borrowed) throws Exception {
        URI first = URI.create("https://first.invalid");
        ClientConnection raw = transport();
        pool.cacheConnection(first, raw);
        if (!borrowed) pool.resetConnectionStatus(raw);
        for (String host : List.of("second", "third")) {
            ClientConnection next = transport();
            pool.cacheConnection(URI.create("https://" + host + ".invalid"), next);
            pool.resetConnectionStatus(next);
        }
        drainOwner();
        assertNull(pool.getConnection(first), "evicted transports cannot be selected again");
        if (borrowed) {
            verify(raw, never()).close();
            assertEquals(3, pool.numberOfConnections(), "outstanding eviction stays tracked");
            pool.resetConnectionStatus(raw);
            drainOwner();
        }
        verify(raw).close();
        assertEquals(2, pool.numberOfConnections());
    }
    @Test void perHostTrimDrainsEveryBorrowBeforeClosing() throws Exception {
        URI uri = URI.create("https://backend.invalid");
        ClientConnection first = transport();
        pool.cacheConnection(uri, first);
        pool.resetConnectionStatus(first);
        assertNotNull(pool.getConnection(uri));
        assertNotNull(pool.getConnection(uri));
        for (int i = 0; i < 3; i++) {
            ClientConnection next = transport();
            pool.cacheConnection(uri, next);
            pool.resetConnectionStatus(next);
        }
        ClientConnection kept = pool.getConnection(uri);
        assertNotNull(kept);
        drainOwner();
        verify(first, never()).close();
        assertEquals(2, pool.numberOfConnections());
        pool.resetConnectionStatus(first);
        drainOwner();
        verify(first, never()).close();
        pool.resetConnectionStatus(first);
        drainOwner();
        verify(first).close();
        assertEquals(1, pool.numberOfConnections());
        pool.resetConnectionStatus(kept);
    }
    @Test void evictionCallsCloseListenersOutsidePoolMonitor() throws Exception {
        URI first = URI.create("https://first.invalid");
        ClientConnection raw = transport();
        pool.cacheConnection(first, raw);
        pool.resetConnectionStatus(raw);
        CompletableFuture<Void> notified = new CompletableFuture<>();
        IoThreadClientConnection.wrap(raw).addCloseListener(ignored -> {
            try {
                CompletableFuture.supplyAsync(() -> pool.getConnection(URI.create("https://absent.invalid")))
                        .get(5, TimeUnit.SECONDS);
                notified.complete(null);
            } catch (Exception error) { notified.completeExceptionally(error); }
        });
        for (String host : List.of("second", "third")) {
            ClientConnection next = transport();
            pool.cacheConnection(URI.create("https://" + host + ".invalid"), next);
            pool.resetConnectionStatus(next);
        }
        notified.get(5, TimeUnit.SECONDS);
        verify(raw).close();
        assertEquals(2, pool.numberOfConnections());
    }
    @Test void clearLeavesDrainingAndSelectableTransportsOpen() throws Exception {
        List<ClientConnection> open = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ClientConnection raw = transport();
            open.add(raw);
            pool.cacheConnection(URI.create("https://backend" + i + ".invalid"), raw);
        }
        pool.clear();
        drainOwner();
        for (ClientConnection raw : open) verify(raw, never()).close();
        assertEquals(0, pool.numberOfConnections());
    }
    @Test void expiredBorrowClosesAfterGraceWithoutAReturnOrUriLookup() throws Exception {
        ClientConnection raw = transport();
        URI uri = URI.create("https://unreturned.invalid");
        pool.cacheConnection(uri, raw);
        clock.set(30000);
        pool.reapExpiredConnections();
        assertNull(pool.getConnection(uri));
        verify(raw, never()).close();
        clock.addAndGet(99999);
        pool.reapExpiredConnections();
        verify(raw, never()).close();
        clock.incrementAndGet();
        pool.reapExpiredConnections();
        drainOwner();
        verify(raw).close();
        assertEquals(0, pool.numberOfConnections());
    }
    @Test void evictedBorrowClosesAfterGraceWithoutAReturn() throws Exception {
        ClientConnection raw = transport();
        pool.cacheConnection(URI.create("https://first.invalid"), raw);
        for (String host : List.of("second", "third")) {
            pool.cacheConnection(URI.create("https://" + host + ".invalid"), transport());
        }
        clock.set(100000);
        pool.reapExpiredConnections();
        drainOwner();
        verify(raw).close();
    }
    @Test void normalReturnDoesNotWaitForPoolMonitor() throws Exception {
        ClientConnection raw = transport();
        URI uri = URI.create("https://return.invalid");
        pool.cacheConnection(uri, raw);
        synchronized (pool) {
            CompletableFuture.runAsync(() -> pool.resetConnectionStatus(raw)).get(5, TimeUnit.SECONDS);
        }
        assertSame(IoThreadClientConnection.wrap(raw), pool.getConnection(uri));
        verify(raw, never()).close();
    }
    @Test void failedInsertionRollsBackTracking() throws Exception {
        ClientConnection raw = transport();
        IllegalStateException failure = new IllegalStateException("cache insertion failed");
        Map<String, Object> broken = new HashMap<>() {
            @Override public Object computeIfAbsent(String key, java.util.function.Function<? super String, ?> factory) {
                throw failure;
            }
        };
        Field cache = Http2ClientConnectionPool.class.getDeclaredField("connectionPool");
        cache.setAccessible(true);
        cache.set(pool, broken);
        try {
            URI uri = URI.create("https://failure.invalid");
            assertSame(failure, assertThrows(IllegalStateException.class, () -> pool.cacheConnection(uri, raw)));
            assertEquals(0, pool.numberOfConnections());
            assertNull(pool.getConnection(uri));
        } finally { IoThreadClientConnection.wrap(raw).close(); }
    }

}
