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
        request = requestConfig(-1, 30000);
        pool = new Http2ClientConnectionPool(request, clock::get);
    }
    private RequestConfig requestConfig(int maximum, int lifetime) {
        return Config.getInstance().getMapper().convertValue(Map.of(
                ClientConfig.CONNECTION_POOL_SIZE, 2,
                ClientConfig.MAX_CONNECTION_NUM_PER_HOST, 4,
                ClientConfig.MIN_CONNECTION_NUM_PER_HOST, 1,
                ClientConfig.MAX_REQ_PER_CONN, maximum,
                ClientConfig.CONNECTION_EXPIRE_TIME, lifetime), RequestConfig.class);
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

    @Test void nativeCloseSignalsWithoutWaitingForThePoolMonitor() throws Exception {
        ClientConnection raw = transport();
        pool.cacheConnection(URI.create("https://closed.invalid"), raw);
        IoThreadClientConnection owned = IoThreadClientConnection.wrap(raw);
        CompletableFuture<Void> closed = new CompletableFuture<>();
        synchronized (pool) {
            owner.execute(() -> { owned.close(); closed.complete(null); });
            closed.get(5, TimeUnit.SECONDS);
            assertEquals(0, pool.numberOfConnections());
        }
        assertEquals(1, pool.reapExpiredConnections(), "metadata cleanup is performed by maintenance");
        Field cache = Http2ClientConnectionPool.class.getDeclaredField("connectionPool");
        cache.setAccessible(true);
        assertTrue(((Map<?, ?>) cache.get(pool)).isEmpty());
        verify(raw).close();
    }

    @Test void staleEmptyUriBucketCannotRemoveANewEmptyBucket() throws Exception {
        URI uri = URI.create("https://recreated.invalid");
        Field cacheField = Http2ClientConnectionPool.class.getDeclaredField("connectionPool");
        cacheField.setAccessible(true);
        Map<?, ?> cache = (Map<?, ?>) cacheField.get(pool);
        ClientConnection old = transport();
        pool.cacheConnection(uri, old);
        Object oldBucket = cache.get(uri.toString());
        IoThreadClientConnection.wrap(old).close();
        drainOwner();
        pool.reapExpiredConnections();
        pool.clear();
        pool.cacheConnection(uri, transport());
        Object newBucket = cache.get(uri.toString());
        clock.set(30000);
        assertNull(pool.getConnection(uri)); // Leaves an empty, newer URI bucket.
        assertNotSame(oldBucket, newBucket);
        assertFalse(cache.remove(uri.toString(), oldBucket), "value matching must use bucket identity");
        assertSame(newBucket, cache.get(uri.toString()));
    }

    @ParameterizedTest @CsvSource({"1,false", "1,true", "2,false", "2,true"})
    void requestLimitHasADeadlineWithoutFurtherUriTraffic(int maximum, boolean multiplexed) throws Exception {
        pool = new Http2ClientConnectionPool(requestConfig(maximum, 1_000_000), clock::get);
        ClientConnection raw = transport();
        when(raw.isMultiplexingSupported()).thenReturn(multiplexed);
        URI uri = URI.create("https://limited.invalid");
        pool.cacheConnection(uri, raw); // The connecting caller owns request number one.
        if (maximum == 2) {
            pool.resetConnectionStatus(raw);
            assertNotNull(pool.getConnection(uri));
        }
        verify(raw, never()).close();
        clock.set(99999);
        pool.reapExpiredConnections();
        verify(raw, never()).close();
        clock.set(100000);
        pool.reapExpiredConnections();
        drainOwner();
        verify(raw).close();
        assertEquals(0, pool.numberOfConnections());
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void finalPermittedBorrowRemainsUsableUntilItReturns(boolean multiplexed) throws Exception {
        pool = new Http2ClientConnectionPool(requestConfig(2, 1_000_000), clock::get);
        ClientConnection raw = transport();
        when(raw.isMultiplexingSupported()).thenReturn(multiplexed);
        URI uri = URI.create("https://last-request.invalid");
        pool.cacheConnection(uri, raw);
        pool.resetConnectionStatus(raw);
        ClientConnection last = pool.getConnection(uri);
        assertNotNull(last);
        assertTrue(last.isOpen());
        verify(raw, never()).close();
        assertNull(pool.getConnection(uri));
        pool.resetConnectionStatus(last);
        drainOwner();
        verify(raw).close();
    }

    @Test void closeNotificationBurstDoesNotStarveARetirementDeadline() throws Exception {
        pool = new Http2ClientConnectionPool(requestConfig(1, 1_000_000), clock::get);
        ClientConnection due = transport();
        pool.cacheConnection(URI.create("https://due.invalid"), due);
        clock.set(100000);
        for (int i = 0; i < 64; i++) {
            ClientConnection raw = transport();
            pool.cacheConnection(URI.create("https://closed.invalid"), raw);
            IoThreadClientConnection.wrap(raw).close();
        }
        drainOwner();
        pool.reapExpiredConnections();
        drainOwner();
        verify(due).close();
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 3})
    void permanentlyFailingRetirementDrainsBorrowersBeforeClosing(int borrowers) throws Exception {
        AtomicBoolean fail = new AtomicBoolean();
        pool = new Http2ClientConnectionPool(request, () -> {
            if (fail.get() && Thread.holdsLock(pool)) throw new AssertionError("permanent retirement failure");
            return clock.get();
        });
        ClientConnection raw = transport();
        URI uri = URI.create("https://failure.invalid");
        pool.cacheConnection(uri, raw);
        if (borrowers == 0) pool.resetConnectionStatus(raw);
        for (int i = 1; i < borrowers; i++) assertNotNull(pool.getConnection(uri));
        Logger logger = (Logger) LoggerFactory.getLogger(Http2ClientConnectionPool.class);
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.start();
        logger.setLevel(Level.WARN);
        logger.addAppender(events);
        try {
            clock.set(30000);
            fail.set(true);
            for (int attempt = 1; attempt <= 5; attempt++) {
                assertEquals(1, pool.reapExpiredConnections());
                if (attempt < 5) {
                    long delay = 1000L << (attempt - 1);
                    clock.addAndGet(delay - 1);
                    assertEquals(0, pool.reapExpiredConnections());
                    clock.incrementAndGet();
                }
            }
            fail.set(false);
            drainOwner();
            assertNull(pool.getConnection(uri));
            if (borrowers > 0) {
                assertTrue(IoThreadClientConnection.wrap(raw).isOpen());
                verify(raw, never()).close();
                for (int i = 0; i < borrowers; i++) {
                    pool.resetConnectionStatus(raw);
                    drainOwner();
                    if (i + 1 < borrowers) verify(raw, never()).close();
                }
            }
            verify(raw).close();
            assertEquals(0, pool.numberOfConnections());
            pool.reapExpiredConnections();
            assertEquals(0, pool.reapExpiredConnections());
            assertEquals(1, events.list.stream().filter(event -> event.getLevel() == Level.WARN).count());
            assertEquals(1, events.list.stream().filter(event -> event.getLevel() == Level.ERROR).count());
        } finally {
            logger.detachAppender(events);
            logger.setLevel(previous);
            events.stop();
        }
    }

    @Test void permanentlyFailingClosedMetadataCleanupStopsAfterItsBudget() throws Exception {
        ClientConnection raw = transport();
        pool.cacheConnection(URI.create("https://metadata-failure.invalid"), raw);
        Field cacheField = Http2ClientConnectionPool.class.getDeclaredField("connectionPool");
        cacheField.setAccessible(true);
        Map<?, ?> original = (Map<?, ?>) cacheField.get(pool);
        AtomicInteger removals = new AtomicInteger();
        Map<Object, Object> broken = new HashMap<>(original) {
            @Override public boolean remove(Object key, Object value) {
                removals.incrementAndGet();
                throw new AssertionError("permanent metadata failure");
            }
        };
        cacheField.set(pool, broken);
        IoThreadClientConnection.wrap(raw).close();
        drainOwner();
        assertEquals(0, pool.numberOfConnections());
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertEquals(1, pool.reapExpiredConnections());
            if (attempt < 5) clock.addAndGet(1000L << (attempt - 1));
        }
        assertEquals(5, removals.get());
        clock.addAndGet(1_000_000);
        assertEquals(0, pool.reapExpiredConnections());
        assertEquals(5, removals.get());
        verify(raw).close();
    }

    @Test void persistentGlobalFailureUsesCappedBackoffAndLogsOnce() {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        Queue<Runnable> ticks = new ConcurrentLinkedQueue<>();
        List<Long> delays = new ArrayList<>();
        doAnswer(invocation -> {
            ticks.add(invocation.getArgument(0));
            delays.add(invocation.getArgument(1));
            return null;
        }).when(scheduler).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS));
        pool = new Http2ClientConnectionPool(request, () -> { throw new AssertionError("clock unavailable"); }, scheduler);
        Logger logger = (Logger) LoggerFactory.getLogger(Http2ClientConnectionPool.class);
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.start();
        logger.setLevel(Level.WARN);
        logger.addAppender(events);
        try {
            pool.startReaper();
            for (int i = 0; i < 10; i++) assertDoesNotThrow(ticks.remove()::run);
            assertEquals(List.of(1000L, 1000L, 2000L, 4000L, 8000L, 16000L, 30000L, 30000L, 30000L, 30000L, 30000L), delays);
            assertEquals(1, events.list.stream().filter(event -> event.getLevel() == Level.ERROR).count());
            assertEquals(1, ticks.size());
        } finally {
            logger.detachAppender(events);
            logger.setLevel(previous);
            events.stop();
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void closedUriDoesNotEvictALiveUriBeforeMetadataCleanup(boolean multiplexed) throws Exception {
        URI first = URI.create("https://live.invalid");
        URI closed = URI.create("https://closed-slot.invalid");
        URI next = URI.create("https://next.invalid");
        ClientConnection live = transport();
        when(live.isMultiplexingSupported()).thenReturn(multiplexed);
        pool.cacheConnection(first, live);
        pool.resetConnectionStatus(live);
        ClientConnection dead = transport();
        pool.cacheConnection(closed, dead);
        IoThreadClientConnection.wrap(dead).close();
        drainOwner(); // Do not run the reaper: the closed notice is still queued.
        pool.cacheConnection(next, transport());
        drainOwner();
        verify(live, never()).close();
        assertSame(IoThreadClientConnection.wrap(live), pool.getConnection(first));
        assertEquals(2, pool.numberOfConnections());
    }

    @Test void closedCleanupOwnershipIsPublishedBeforeUntracking() throws Exception {
        ClientConnection raw = transport();
        URI uri = URI.create("https://publication.invalid");
        pool.cacheConnection(uri, raw);
        Field trackedField = Http2ClientConnectionPool.class.getDeclaredField("tracked");
        trackedField.setAccessible(true);
        Map<?, ?> original = (Map<?, ?>) trackedField.get(pool);
        Object cached = original.values().iterator().next();
        CountDownLatch removed = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ConcurrentMap<Object, Object> gated = new ConcurrentHashMap<>(original) {
            @Override public boolean remove(Object key, Object value) {
                boolean result = super.remove(key, value);
                removed.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("Close publication gate timed out");
                } catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new CompletionException(error); }
                return result;
            }
        };
        trackedField.set(pool, gated);
        CompletableFuture<Void> closeDone = new CompletableFuture<>();
        owner.execute(() -> { IoThreadClientConnection.wrap(raw).close(); closeDone.complete(null); });
        try {
            assertTrue(removed.await(5, TimeUnit.SECONDS));
            var managed = Http2ClientConnectionPool.class.getDeclaredMethod("isManaged", cached.getClass());
            managed.setAccessible(true);
            assertEquals(Boolean.TRUE, managed.invoke(pool, cached));
            clock.set(30000);
            pool.reapExpiredConnections(); // Cleanup can complete before the native notice is enqueued.
        } finally { release.countDown(); }
        closeDone.get(5, TimeUnit.SECONDS);
        pool.reapExpiredConnections();
        assertEquals(0, pool.numberOfConnections());
        assertNull(pool.getConnection(uri));
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
