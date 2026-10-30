package com.networknt.client.http;

import com.networknt.client.ClientConfig;
import com.networknt.client.RequestConfig;
import io.undertow.client.ClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;

import java.net.URI;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * URI LRU pool. Pool-initiated retirement allows a 100-second borrower-drain grace.
 * The underlying transport may close earlier.
 * @author Jiachen Sun
 */
public class Http2ClientConnectionPool {
    private enum ConnectionStatus { HANGING, AVAILABLE, CLOSE, MULTIPLEX_SUPPORT }
    private static final Logger logger = LoggerFactory.getLogger(Http2ClientConnectionPool.class);
    private static final long RETIREMENT_GRACE_MS = 100_000;
    private final Map<String, UriConnections> connectionPool;
    private final Set<UriConnections> emptyBuckets = ConcurrentHashMap.newKeySet();
    private final int maxUriCount;
    // Canonical views have identity equality, including transports supplied by custom providers.
    private final ConcurrentMap<ClientConnection, CachedConnection> tracked;
    private final Queue<CachedConnection> pendingClose = new ConcurrentLinkedQueue<>();
    private final Queue<CachedConnection> closedRecords = new ConcurrentLinkedQueue<>();
    private final AtomicInteger reaperFailures = new AtomicInteger();
    private static final int MAX_MAINTENANCE_FAILURES = 5;
    private final Supplier<RequestConfig> requestConfig;
    private final LongSupplier clock;
    private final ScheduledExecutorService reaper;
    private final NavigableSet<Deadline> deadlines = new ConcurrentSkipListSet<>();
    private final AtomicLong deadlineSequence = new AtomicLong();
    private static final int REAPER_BATCH_SIZE = 64;
    private static final RetryablePool shared = new RetryablePool(() -> {
        Http2ClientConnectionPool pool = new Http2ClientConnectionPool(
                () -> ClientConfig.get().getRequest(), () -> TimeUnit.NANOSECONDS.toMillis(System.nanoTime()));
        pool.startReaper();
        return pool;
    });

    // Only the successful result is published. Configuration failures can be retried.
    static final class RetryablePool {
        private final Supplier<Http2ClientConnectionPool> factory;
        private volatile Http2ClientConnectionPool instance;
        RetryablePool(Supplier<Http2ClientConnectionPool> factory) { this.factory = factory; }
        Http2ClientConnectionPool get() {
            Http2ClientConnectionPool result = instance;
            if (result != null) return result;
            synchronized (this) {
                if (instance == null) instance = factory.get();
                return instance;
            }
        }
    }

    // Package-local configuration and clock keep lifecycle tests independent of global config/time.
    Http2ClientConnectionPool(RequestConfig requestConfig, LongSupplier clock) {
        this(() -> requestConfig, clock, null);
    }

    Http2ClientConnectionPool(RequestConfig requestConfig, LongSupplier clock, ScheduledExecutorService reaper) {
        this(() -> requestConfig, clock, reaper);
    }

    private Http2ClientConnectionPool(Supplier<RequestConfig> requestConfig, LongSupplier clock) {
        this(requestConfig, clock, Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "client-connection-reaper");
            thread.setDaemon(true);
            return thread;
        }));
    }

    private Http2ClientConnectionPool(Supplier<RequestConfig> requestConfig, LongSupplier clock, ScheduledExecutorService reaper) {
        this(requestConfig, clock, reaper, new StateMaps() { });
    }

    Http2ClientConnectionPool(Supplier<RequestConfig> requestConfig, LongSupplier clock, ScheduledExecutorService reaper,
                              StateMaps maps) {
        this.requestConfig = requestConfig;
        this.clock = clock;
        this.reaper = reaper;
        this.tracked = maps.tracked(new ConcurrentHashMap<>());
        int poolSize = requestConfig.get().getConnectionPoolSize();
        maxUriCount = poolSize;
        Map<String, UriConnections> lru = new LinkedHashMap<>((int) Math.ceil(poolSize / 0.75f) + 1, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<String, UriConnections> eldest) {
                if (size() <= poolSize) return false;
                logger.debug("Evicting {} cached connections for uri: {}", eldest.getValue().connections.size(), eldest.getKey());
                eldest.getValue().connections.forEach(Http2ClientConnectionPool.this::retire);
                eldest.getValue().discarded = true;
                emptyBuckets.remove(eldest.getValue());
                return true;
            }
        };
        connectionPool = maps.uris(lru);
    }

    /** Decorators must delegate storage operations to the supplied production backing maps. */
    interface StateMaps {
        default <K, V> ConcurrentMap<K, V> tracked(ConcurrentMap<K, V> backing) { return backing; }
        default <K, V> Map<K, V> uris(Map<K, V> backing) { return backing; }
    }

    /** @return the shared connection pool */
    public static Http2ClientConnectionPool getInstance() { return shared.get(); }

    /**
     * Borrow a cached connection. Return each borrow with resetConnectionStatus.
     * @param uri backend URI
     * @return a connection, or null when none is selectable
     */
    public ClientConnection getConnection(URI uri) {
        try {
            synchronized (this) {
                if (uri == null) return null;
                UriConnections bucket = connectionPool.get(uri.toString());
                List<CachedConnection> connections = bucket == null ? null : bucket.connections;
                logger.debug("Got {} connections for uri: {}", connections == null ? 0 : connections.size(), uri);
                CachedConnection selected = selectConnection(uri, connections);
                logger.debug("Got cached connection: {} for uri: {}", selected, uri);
                if (selected == null) return null;
                selected.borrowers.incrementAndGet();
                hangConnection(selected);
                return selected.get();
            }
        } finally { closeRetiredConnections(); }
    }

    /**
     * Cache a new transport, initially reserved by its connecting caller.
     * @param uri backend URI
     * @param connection connected transport
     */
    public void cacheConnection(URI uri, ClientConnection connection) {
        String uriString = Objects.requireNonNull(uri, "uri").toString();
        ClientConnection owned = IoThreadClientConnection.wrap(connection);
        CachedConnection added = null;
        boolean committed = false;
        try {
            synchronized (this) {
                pruneEmptyBucketsForAdmission();
                if (tracked.containsKey(owned)) return;
                UriConnections existing = connectionPool.get(uriString);
                if (existing != null) removeClosedConnections(existing.connections);
                added = new CachedConnection(owned, clock.getAsLong(), requestConfig.get());
                tracked.put(owned, added);
                UriConnections bucket = connectionPool.computeIfAbsent(uriString, UriConnections::new);
                bucket.connections.add(added);
                added.owner = new WeakReference<>(bucket);
                bucket.members.incrementAndGet();
                added.bucketMember.set(true);
                logger.debug("Cached connection for uri: {}, per-uri count: {}, total tracked: {}", uri, bucket.connections.size(), tracked.size());
                if (!connectionPool.containsKey(uriString)) retire(added);
                else if (added.requestLimitReached()) {
                    detachFromUri(added);
                    retire(added);
                } else replaceDeadline(added, added.lifeStartTime + added.expireTime);
            }
            CachedConnection registered = added;
            owned.addCloseListener(ignored -> connectionClosed(registered));
            committed = true;
        } finally {
            if (!committed && added != null) rollbackCache(added);
            closeRetiredConnections();
        }
    }

    private synchronized void rollbackCache(CachedConnection added) {
        tracked.remove(added.get(), added);
        cancelDeadline(added);
        detachFromUri(added);
    }

    // Retain access order: LinkedHashMap.get() here would turn cleanup into apparent URI traffic.
    private void detachFromUri(CachedConnection connection) {
        releaseBucketMember(connection);
        UriConnections bucket = connection.owner == null ? null : connection.owner.get();
        if (bucket != null) {
            bucket.connections.remove(connection);
            if (bucket.connections.isEmpty()) discardBucket(bucket);
        }
    }

    // Admission handles completed close notices before considering eviction of any live URI.
    private void pruneEmptyBucketsForAdmission() {
        Iterator<UriConnections> candidates = emptyBuckets.iterator();
        while (connectionPool.size() >= maxUriCount && candidates.hasNext()) {
            UriConnections bucket = candidates.next();
            emptyBuckets.remove(bucket);
            if (bucket.members.get() == 0) discardBucket(bucket);
        }
    }

    private void discardBucket(UriConnections bucket) {
        connectionPool.remove(bucket.uri, bucket);
        bucket.discarded = true;
        emptyBuckets.remove(bucket);
    }

    private void releaseBucketMember(CachedConnection connection) {
        if (!connection.bucketMember.compareAndSet(true, false)) return;
        UriConnections bucket = connection.owner == null ? null : connection.owner.get();
        if (bucket != null && bucket.members.decrementAndGet() == 0) {
            emptyBuckets.add(bucket);
            // A clear/eviction may have raced the native close notification.
            if (bucket.discarded) emptyBuckets.remove(bucket);
        }
    }

    private CachedConnection selectConnection(URI uri, List<CachedConnection> connections) {
        if (connections == null) return null;
        trimConnections(uri, connections);
        removeClosedConnections(connections);
        if (connections.isEmpty()) return null;
        int offset = ThreadLocalRandom.current().nextInt(connections.size());
        for (int i = 0; i < connections.size(); i++) {
            CachedConnection candidate = connections.get((i + offset) % connections.size());
            ConnectionStatus status = getConnectionStatus(candidate);
            if (status == ConnectionStatus.AVAILABLE || status == ConnectionStatus.MULTIPLEX_SUPPORT) return candidate;
            logger.debug("Connection status for uri: {} is {}", uri, status);
        }
        logger.debug("None of the connection cached can be used for uri: {}", uri);
        return null;
    }

    private void trimConnections(URI uri, List<CachedConnection> connections) {
        RequestConfig config = requestConfig.get();
        if (connections.size() <= config.getMaxConnectionNumPerHost() * 0.75) return;
        logger.debug("Before removing max connections per host: {} connections for uri: {}", connections.size(), uri);
        int minimum = Math.max(0, config.getMinConnectionNumPerHost());
        while (connections.size() > minimum) retire(connections.removeFirst());
        logger.debug("After removing max connections per host: {} connections for uri: {}", connections.size(), uri);
    }

    private void removeClosedConnections(List<CachedConnection> connections) {
        Iterator<CachedConnection> iterator = connections.iterator();
        while (iterator.hasNext()) {
            CachedConnection connection = iterator.next();
            if (getConnectionStatus(connection) == ConnectionStatus.CLOSE) {
                iterator.remove();
                retire(connection);
            }
        }
    }

    private ConnectionStatus getConnectionStatus(CachedConnection connection) {
        if (!connection.bucketMember.get() || connection.retired || !connection.isOpen(clock.getAsLong())) return ConnectionStatus.CLOSE;
        return connection.get().isMultiplexingSupported() ? ConnectionStatus.MULTIPLEX_SUPPORT : connection.status;
    }

    /**
     * Return a borrow after its complete response or failure. Normal returns do not take the pool monitor.
     * @param connection the returned transport
     */
    public void resetConnectionStatus(ClientConnection connection) {
        if (connection == null) return;
        ClientConnection existing = connection;
        if (!(connection instanceof IoThreadClientConnection)) {
            ConnectionWrappers.Entry entry = ConnectionWrappers.INSTANCE.lookup(connection);
            if (entry == null) return;
            existing = entry.currentView();
        }
        if (existing == null) return;
        CachedConnection cached = tracked.get(existing);
        if (cached == null) return;
        cached.borrowers.updateAndGet(count -> Math.max(0, count - 1));
        if (cached.retired) {
            releaseRetired(cached);
            closeRetiredConnections();
        } else cached.status = ConnectionStatus.AVAILABLE;
    }

    private void hangConnection(CachedConnection connection) {
        connection.requestCount.incrementAndGet();
        connection.status = ConnectionStatus.HANGING;
        if (connection.requestLimitReached()) {
            detachFromUri(connection);
            retire(connection);
        }
    }

    // Called under the pool monitor; returns only read the published retirement state.
    private void retire(CachedConnection connection) {
        releaseBucketMember(connection);
        if (!connection.retired) {
            connection.retiredAt = clock.getAsLong();
            connection.retired = true;
            replaceDeadline(connection, connection.retiredAt + RETIREMENT_GRACE_MS);
            logger.debug("Parked the connection: {}, borrowers: {}, grace: {} ms", connection.get(), connection.borrowers.get(), RETIREMENT_GRACE_MS);
        }
        releaseRetired(connection);
    }

    private void releaseRetired(CachedConnection connection) {
        // Non-selectability (for example HTTP/1 CLOSE_REQ) does not establish native closure.
        if (connection.borrowers.get() == 0) queueClose(connection);
    }

    private void queueClose(CachedConnection connection) {
        if (tracked.get(connection.get()) == connection && connection.closing.compareAndSet(false, true)) {
            pendingClose.add(connection);
        }
    }

    private void connectionClosed(CachedConnection connection) {
        // Publish cleanup ownership before untracking so isManaged never loses pending work.
        connection.closedCleanup = true;
        releaseBucketMember(connection);
        if (tracked.remove(connection.get(), connection)) closedRecords.add(connection);
        else connection.closedCleanup = false;
    }

    private void cleanClosedRecord(CachedConnection connection) {
        // Invoked by maintenance, never by the transport's native close listener.
        detachFromUri(connection);
        cancelDeadline(connection);
        connection.closedCleanup = false;
        connection.maintenanceFailures = 0;
    }

    private void closeRetiredConnections() {
        CachedConnection connection;
        while ((connection = pendingClose.poll()) != null) {
            // clear() forgets metadata without requesting native close.
            if (tracked.get(connection.get()) == connection) IoUtils.safeClose(connection.get());
        }
    }

    void startReaper() { scheduleReaper(1000); }

    private void scheduleReaper(long delay) {
        if (reaper != null) reaper.schedule(new ReaperTask(), delay, TimeUnit.MILLISECONDS);
    }

    private final class ReaperTask implements Runnable {
        private long nextDelay = 1000;
        @Override public void run() {
            try {
                ClientConnectionOperations.runWithFailureHandler(() -> {
                    int processed = reapExpiredConnections();
                    reaperFailures.set(0);
                    nextDelay = processed == REAPER_BATCH_SIZE ? 0 : 1000;
                }, error -> {
                    int failures = reaperFailures.incrementAndGet();
                    nextDelay = retryDelay(failures);
                    if (failures == 1 || IoThreadClientConnection.isFatal(error)) {
                        logger.error("Client connection maintenance failed; next run in {} ms", nextDelay, error);
                    } else logger.debug("Client connection maintenance retry in {} ms", nextDelay, error);
                });
            } finally {
                // One-shot scheduling survives a failing run, including a rethrown fatal Error.
                scheduleReaper(nextDelay);
            }
        }
    }

    private static long retryDelay(int failures) {
        return Math.min(30_000L, 1000L << Math.min(5, failures - 1));
    }

    private void replaceDeadline(CachedConnection connection, long at) {
        Deadline next = new Deadline(at, deadlineSequence.incrementAndGet(), connection);
        Deadline previous = connection.deadline.getAndSet(next);
        if (previous != null) deadlines.remove(previous);
        deadlines.add(next);
        // Do not retain a deadline if its record is no longer tracked.
        if (connection.deadline.get() != next || !isManaged(connection)) deadlines.remove(next);
    }

    private void cancelDeadline(CachedConnection connection) {
        Deadline previous = connection.deadline.getAndSet(null);
        if (previous != null) deadlines.remove(previous);
    }

    private boolean isManaged(CachedConnection connection) {
        return tracked.get(connection.get()) == connection || connection.closedCleanup;
    }

    // Share each bounded batch between closed-record cleanup and time/request-count deadlines.
    int reapExpiredConnections() {
        long now = clock.getAsLong();
        try {
            int processed = drainClosedRecords(REAPER_BATCH_SIZE / 2, now);
            processed += drainDeadlines(REAPER_BATCH_SIZE - processed, now);
            return processed + drainClosedRecords(REAPER_BATCH_SIZE - processed, now);
        } finally { closeRetiredConnections(); }
    }

    private int drainClosedRecords(int limit, long now) {
        int processed = 0;
        CachedConnection closed;
        while (processed < limit && (closed = closedRecords.poll()) != null) {
            CachedConnection current = closed;
            processed++;
            ClientConnectionOperations.runWithFailureHandler(() -> processClosedRecord(current, now),
                    error -> retryClosedRecord(current, now, error));
        }
        return processed;
    }

    private int drainDeadlines(int limit, long now) {
        NavigableSet<Deadline> due = deadlines.headSet(new Deadline(now, Long.MAX_VALUE, null), true);
        int processed = 0;
        Deadline deadline;
        while (processed < limit && (deadline = due.pollFirst()) != null) {
            Deadline current = deadline;
            processed++;
            ClientConnectionOperations.runWithFailureHandler(() -> processDeadline(current, now),
                    error -> retryDeadline(current, now, error));
        }
        return processed;
    }

    private void processClosedRecord(CachedConnection connection, long now) {
        synchronized (this) {
            Deadline pending = connection.deadline.get();
            // A deadline can have seen the close before its queued notice arrived.
            if (connection.maintenanceFailures > 0 && pending != null && pending.at > now) return;
            if (connection.closedCleanup) cleanClosedRecord(connection);
        }
    }

    private void processDeadline(Deadline deadline, long now) {
        synchronized (this) {
            CachedConnection connection = deadline.connection;
            if (!isManaged(connection) || connection.deadline.get() != deadline) return;
            if (connection.closedCleanup) {
                if (tracked.get(connection.get()) == connection) replaceDeadline(connection, now + 1000);
                else cleanClosedRecord(connection);
            } else {
                if (!connection.retired) {
                    detachFromUri(connection);
                    retire(connection);
                } else if (now >= connection.retiredAt + RETIREMENT_GRACE_MS) {
                    logger.debug("ParkedConnection expired: {}, outstanding borrowers: {}", connection.get(), connection.borrowers.get());
                    queueClose(connection);
                } else replaceDeadline(connection, connection.retiredAt + RETIREMENT_GRACE_MS);
                connection.maintenanceFailures = 0;
            }
        }
    }

    private void retryClosedRecord(CachedConnection connection, long now, Throwable error) {
        synchronized (this) {
            if (connection.closedCleanup) retryMaintenance(connection, now, error);
        }
    }

    private void retryDeadline(Deadline deadline, long now, Throwable error) {
        synchronized (this) {
            CachedConnection connection = deadline.connection;
            if (isManaged(connection) && connection.deadline.get() == deadline) {
                retryMaintenance(connection, now, error);
            }
        }
    }

    private void retryMaintenance(CachedConnection connection, long now, Throwable error) {
        int failures = ++connection.maintenanceFailures;
        if (failures < MAX_MAINTENANCE_FAILURES) {
            long delay = retryDelay(failures);
            replaceDeadline(connection, now + delay);
            if (failures == 1) logger.warn("Unable to maintain client connection; retrying in {} ms", delay, error);
            else logger.debug("Retrying client connection maintenance in {} ms", delay, error);
            return;
        }
        cancelDeadline(connection);
        if (connection.closedCleanup) connection.closedCleanup = false;
        else {
            // Use this run's timestamp, avoiding the clock/path that exhausted its retry budget.
            // Maintenance failure starts bounded draining, rather than closing active streams now.
            if (!connection.retired) connection.retiredAt = now;
            connection.retired = true;
            connection.status = ConnectionStatus.CLOSE;
            releaseBucketMember(connection);
            replaceDeadline(connection, connection.retiredAt + RETIREMENT_GRACE_MS);
            releaseRetired(connection);
        }
        logger.error("Stopping client connection maintenance after {} failures", failures, error);
    }

    private record Deadline(long at, long sequence, CachedConnection connection) implements Comparable<Deadline> {
        @Override public int compareTo(Deadline other) {
            int order = Long.compare(at, other.at);
            return order == 0 ? Long.compare(sequence, other.sequence) : order;
        }
    }

    /** @return all tracked transports, including evicted transports still borrowed */
    public int numberOfConnections() { return tracked.size(); }

    /** Clear pool metadata without closing caller-owned transports, including in-flight requests. */
    public synchronized void clear() {
        pendingClose.clear();
        closedRecords.clear();
        deadlines.clear();
        tracked.clear();
        connectionPool.values().forEach(bucket -> bucket.discarded = true);
        connectionPool.clear();
        emptyBuckets.clear();
        logger.debug("Cleared client connection pool metadata");
    }

    // Object equality deliberately gives each URI bucket identity semantics.
    private static final class UriConnections {
        private final String uri;
        private final List<CachedConnection> connections = new LinkedList<>();
        private final AtomicInteger members = new AtomicInteger();
        private volatile boolean discarded;
        private UriConnections(String uri) { this.uri = uri; }
    }

    private static final class CachedConnection {
        private final AtomicInteger requestCount = new AtomicInteger(1);
        private final ClientConnection clientConnection;
        private WeakReference<UriConnections> owner;
        private final AtomicBoolean bucketMember = new AtomicBoolean();
        private final AtomicReference<Deadline> deadline = new AtomicReference<>();
        private final long lifeStartTime;
        private final int maxReqCount;
        private final long expireTime;
        private final AtomicInteger borrowers = new AtomicInteger(1);
        private volatile ConnectionStatus status = ConnectionStatus.HANGING;
        private volatile boolean retired;
        private volatile boolean closedCleanup;
        private int maintenanceFailures;
        private volatile long retiredAt;
        private final AtomicBoolean closing = new AtomicBoolean();

        private CachedConnection(ClientConnection connection, long now, RequestConfig config) {
            clientConnection = connection;
            lifeStartTime = now;
            maxReqCount = config.getMaxReqPerConn();
            expireTime = config.getConnectionExpireTime();
        }
        private boolean requestLimitReached() { return maxReqCount != -1 && requestCount.get() >= maxReqCount; }
        private boolean isOpen(long now) {
            return now - lifeStartTime < expireTime && !requestLimitReached() && clientConnection.isOpen();
        }
        private ClientConnection get() { return clientConnection; }
    }
}
