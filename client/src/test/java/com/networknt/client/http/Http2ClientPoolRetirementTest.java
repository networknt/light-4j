package com.networknt.client.http;

import com.networknt.client.ClientConfig;
import com.networknt.client.RequestConfig;
import io.undertow.client.ClientConnection;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.networknt.config.Config;
import java.lang.reflect.Field;
import org.xnio.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Isolated("Observes pool logging and creates concurrent pool operations")
class Http2ClientPoolRetirementTest {
    private XnioWorker worker;
    private XnioIoThread owner;
    private Http2ClientConnectionPool pool;
    private RequestConfig request;
    private final AtomicLong clock = new AtomicLong();
    private final List<ClientConnection> transports = new ArrayList<>();

    @BeforeEach void setUp() throws Exception {
        worker = Xnio.getInstance().createWorker(OptionMap.create(Options.WORKER_IO_THREADS, 1));
        owner = worker.getIoThread();
        request = Config.getInstance().getMapper().convertValue(Map.of(
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
    @Test void expiryDoesNotRefreshUriRecency() throws Exception {
        URI first = URI.create("https://first.invalid");
        URI second = URI.create("https://second.invalid");
        URI third = URI.create("https://third.invalid");
        ClientConnection expired = transport();
        pool.cacheConnection(first, expired);
        pool.resetConnectionStatus(expired);
        clock.set(10000);
        ClientConnection other = transport();
        pool.cacheConnection(second, other);
        pool.resetConnectionStatus(other);
        clock.set(20000);
        ClientConnection newer = transport();
        pool.cacheConnection(first, newer);
        pool.resetConnectionStatus(newer);
        ClientConnection borrowed = pool.getConnection(second); // second is the most recently used URI.
        pool.resetConnectionStatus(borrowed);
        clock.set(30000);
        pool.reapExpiredConnections();
        pool.cacheConnection(third, transport());
        drainOwner();
        assertNull(pool.getConnection(first), "expiry cleanup must not refresh the older URI");
        assertSame(IoThreadClientConnection.wrap(other), pool.getConnection(second));
        verify(newer).close();
    }

    @Test void nativeCloseRemovesUriMetadataWithoutWaitingForTheReaper() throws Exception {
        ClientConnection raw = transport();
        pool.cacheConnection(URI.create("https://closed.invalid"), raw);
        IoThreadClientConnection.wrap(raw).close();
        drainOwner();
        // The public count alone would miss a closed transport still rooted by its URI list.
        Field cache = Http2ClientConnectionPool.class.getDeclaredField("connectionPool");
        cache.setAccessible(true);
        assertTrue(((Map<?, ?>) cache.get(pool)).isEmpty());
        assertEquals(0, pool.numberOfConnections());
        assertEquals(0, pool.reapExpiredConnections());
    }

    @Test void untrackedReturnDoesNotTouchTheRawTransport() {
        ClientConnection raw = mock(ClientConnection.class);
        pool.resetConnectionStatus(raw);
        verifyNoInteractions(raw);
        assertEquals(0, pool.numberOfConnections());
    }

    @Test void unexpiredDeadlinesDoNotScanConnectionsOrTakeThePoolMonitor() throws Exception {
        for (int i = 0; i < 16; i++) {
            pool.cacheConnection(URI.create("https://fresh" + i + ".invalid"), transport());
        }
        drainOwner();
        transports.forEach(org.mockito.Mockito::clearInvocations);
        synchronized (pool) {
            assertEquals(0, CompletableFuture.supplyAsync(pool::reapExpiredConnections).get(5, TimeUnit.SECONDS));
        }
        for (ClientConnection raw : transports) verifyNoInteractions(raw);
    }

    @Test void busyHttp1CandidatesDoNotProduceWarnings() throws Exception {
        ClientConnection raw = transport();
        when(raw.isMultiplexingSupported()).thenReturn(false);
        URI uri = URI.create("https://busy.invalid");
        pool.cacheConnection(uri, raw);
        Logger logger = (Logger) LoggerFactory.getLogger(Http2ClientConnectionPool.class);
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.start();
        logger.addAppender(events);
        try {
            for (int i = 0; i < 20; i++) assertNull(pool.getConnection(uri));
            assertTrue(events.list.stream().noneMatch(event -> event.getLevel().isGreaterOrEqual(Level.WARN)));
        } finally {
            logger.detachAppender(events);
            events.stop();
        }
    }

    @Test void lastReturnRacingLruRetirementAlwaysClosesExactlyOnce() throws Exception {
        URI first = URI.create("https://first.invalid");
        URI second = URI.create("https://second.invalid");
        URI third = URI.create("https://third.invalid");
        for (int i = 0; i < 100; i++) {
            ClientConnection raw = transport();
            ClientConnection next = transport();
            ClientConnection last = transport();
            pool.cacheConnection(first, raw);
            pool.cacheConnection(second, next);
            CompletableFuture<Void> gate = new CompletableFuture<>();
            CompletableFuture<Void> returning = gate.thenRunAsync(() -> pool.resetConnectionStatus(raw));
            CompletableFuture<Void> evicting = gate.thenRunAsync(() -> pool.cacheConnection(third, last));
            gate.complete(null);
            CompletableFuture.allOf(returning, evicting).get(5, TimeUnit.SECONDS);
            drainOwner();
            verify(raw).close();
            assertNull(pool.getConnection(first));
            assertEquals(2, pool.numberOfConnections());
            pool.clear();
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void initializationFailureCanBeRetried(boolean fatal) {
        AtomicInteger attempts = new AtomicInteger();
        Http2ClientConnectionPool.RetryablePool retryable = new Http2ClientConnectionPool.RetryablePool(() -> {
            if (attempts.getAndIncrement() == 0) {
                if (fatal) throw new LinkageError("initialization failure");
                throw new IllegalStateException("configuration not ready");
            }
            return pool;
        });
        if (fatal) assertThrows(LinkageError.class, retryable::get);
        else assertThrows(IllegalStateException.class, retryable::get);
        assertSame(pool, retryable.get());
        assertSame(pool, retryable.get());
        assertEquals(2, attempts.get());
    }

    @ParameterizedTest @CsvSource({"false,1", "false,2", "true,1", "true,2"})
    void deadlineErrorReschedulesAndRetriesTheAffectedConnection(boolean fatal, int failingRead) throws Exception {
        AtomicInteger failAt = new AtomicInteger();
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        Queue<Runnable> ticks = new ConcurrentLinkedQueue<>();
        doAnswer(invocation -> { ticks.add(invocation.getArgument(0)); return null; })
                .when(scheduler).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS));
        pool = new Http2ClientConnectionPool(request, () -> {
            if (failAt.getAndUpdate(count -> Math.max(0, count - 1)) == 1) {
                if (fatal) throw new LinkageError("deadline failure");
                throw new AssertionError("deadline failure");
            }
            return clock.get();
        }, scheduler);
        ClientConnection raw = transport();
        pool.cacheConnection(URI.create("https://retry.invalid"), raw);
        pool.startReaper();
        clock.set(30000);
        failAt.set(failingRead);
        Runnable first = ticks.remove();
        if (fatal) assertThrows(LinkageError.class, first::run);
        else assertDoesNotThrow(first::run);
        assertEquals(1, ticks.size(), "a failed run must schedule its successor");
        clock.set(31000);
        ticks.remove().run();
        verify(raw, never()).close();
        clock.set(131000);
        ticks.remove().run();
        drainOwner();
        verify(raw).close();
        assertEquals(0, pool.numberOfConnections());
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
