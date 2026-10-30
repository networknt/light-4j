package com.networknt.client.http;

import com.networknt.client.ClientConfig;
import com.networknt.client.RequestConfig;
import io.undertow.client.ClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * URI LRU pool. Retired transports drain borrowers for a 100-second grace period.
 * @author Jiachen Sun
 */
public class Http2ClientConnectionPool {
    private enum ConnectionStatus { HANGING, AVAILABLE, CLOSE, MULTIPLEX_SUPPORT }
    private static final Logger logger = LoggerFactory.getLogger(Http2ClientConnectionPool.class);
    private static final long RETIREMENT_GRACE_MS = 100_000;
    private final Map<String, List<CachedConnection>> connectionPool;
    // Canonical views have identity equality, including transports supplied by custom providers.
    private final ConcurrentMap<ClientConnection, CachedConnection> tracked = new ConcurrentHashMap<>();
    private final Queue<CachedConnection> pendingClose = new ConcurrentLinkedQueue<>();
    private final Supplier<RequestConfig> requestConfig;
    private final LongSupplier clock;

    private static final class Holder {
        static final Http2ClientConnectionPool INSTANCE = new Http2ClientConnectionPool(
                () -> ClientConfig.get().getRequest(), () -> TimeUnit.NANOSECONDS.toMillis(System.nanoTime()));
        static {
            ScheduledExecutorService reaper = Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "client-connection-reaper");
                thread.setDaemon(true);
                return thread;
            });
            reaper.scheduleWithFixedDelay(INSTANCE::reapSafely, 1, 1, TimeUnit.SECONDS);
        }
    }

    // Package-local configuration and clock keep lifecycle tests independent of global config/time.
    Http2ClientConnectionPool(RequestConfig requestConfig, LongSupplier clock) {
        this(() -> requestConfig, clock);
    }

    private Http2ClientConnectionPool(Supplier<RequestConfig> requestConfig, LongSupplier clock) {
        this.requestConfig = requestConfig;
        this.clock = clock;
        int poolSize = requestConfig.get().getConnectionPoolSize();
        connectionPool = new LinkedHashMap<>((int) Math.ceil(poolSize / 0.75f) + 1, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<String, List<CachedConnection>> eldest) {
                if (size() <= poolSize) return false;
                logger.debug("Evicting {} cached connections for uri: {}", eldest.getValue().size(), eldest.getKey());
                eldest.getValue().forEach(Http2ClientConnectionPool.this::retire);
                return true;
            }
        };
    }

    /** @return the shared connection pool */
    public static Http2ClientConnectionPool getInstance() { return Holder.INSTANCE; }

    /**
     * Borrow a cached connection. Return each borrow with resetConnectionStatus.
     * @param uri backend URI
     * @return a connection, or null when none is selectable
     */
    public ClientConnection getConnection(URI uri) {
        try {
            synchronized (this) {
                if (uri == null) return null;
                List<CachedConnection> connections = connectionPool.get(uri.toString());
                logger.debug("Got {} connections for uri: {}", connections == null ? 0 : connections.size(), uri);
                CachedConnection selected = selectConnection(uri, connections);
                logger.debug("Got cached connection: {} for uri: {}", selected, uri);
                if (selected == null) return null;
                hangConnection(selected);
                selected.borrowers.incrementAndGet();
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
                if (tracked.containsKey(owned)) return;
                List<CachedConnection> existing = connectionPool.get(uriString);
                if (existing != null) removeClosedConnections(existing);
                added = new CachedConnection(owned, clock.getAsLong(), requestConfig.get());
                tracked.put(owned, added);
                List<CachedConnection> connections = connectionPool.computeIfAbsent(uriString, key -> new LinkedList<>());
                connections.add(added);
                logger.debug("Cached connection for uri: {}, per-uri count: {}, total tracked: {}", uri, connections.size(), tracked.size());
                if (!connectionPool.containsKey(uriString)) retire(added);
            }
            CachedConnection registered = added;
            owned.addCloseListener(ignored -> connectionClosed(registered));
            committed = true;
        } finally {
            if (!committed && added != null) rollbackCache(uriString, added);
            closeRetiredConnections();
        }
    }

    private synchronized void rollbackCache(String uri, CachedConnection added) {
        tracked.remove(added.get(), added);
        List<CachedConnection> connections = connectionPool.get(uri);
        if (connections != null) {
            connections.remove(added);
            if (connections.isEmpty()) connectionPool.remove(uri);
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
            logger.warn("Connection status for uri: {} is {}", uri, status);
        }
        logger.warn("None of the connection cached can be used for uri: {}", uri);
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
        if (connection.retired || !connection.isOpen(clock.getAsLong())) return ConnectionStatus.CLOSE;
        return connection.get().isMultiplexingSupported() ? ConnectionStatus.MULTIPLEX_SUPPORT : connection.status;
    }

    /**
     * Return a borrow after its complete response or failure. Normal returns do not take the pool monitor.
     * @param connection the returned transport
     */
    public void resetConnectionStatus(ClientConnection connection) {
        if (connection == null) return;
        CachedConnection cached = tracked.get(IoThreadClientConnection.wrap(connection));
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
    }

    // Called under the pool monitor; returns only read the published retirement state.
    private void retire(CachedConnection connection) {
        if (!connection.retired) {
            connection.retiredAt = clock.getAsLong();
            connection.retired = true;
            logger.debug("Parked the connection: {}, borrowers: {}, grace: {} ms", connection.get(), connection.borrowers.get(), RETIREMENT_GRACE_MS);
        }
        releaseRetired(connection);
    }

    private void releaseRetired(CachedConnection connection) {
        if (connection.borrowers.get() > 0 && connection.get().isOpen()) return;
        queueClose(connection);
    }

    private void queueClose(CachedConnection connection) {
        if (tracked.get(connection.get()) == connection && connection.closing.compareAndSet(false, true)) {
            pendingClose.add(connection);
        }
    }

    private void connectionClosed(CachedConnection connection) { tracked.remove(connection.get(), connection); }

    private void closeRetiredConnections() {
        CachedConnection connection;
        while ((connection = pendingClose.poll()) != null) {
            // clear() forgets metadata without requesting native close.
            if (tracked.get(connection.get()) == connection) IoUtils.safeClose(connection.get());
        }
    }

    private void reapSafely() {
        try { reapExpiredConnections(); }
        catch (RuntimeException error) { logger.warn("Unable to reap retired client connections", error); }
    }

    // The singleton timer runs this even if no request returns or touches this URI again.
    void reapExpiredConnections() {
        synchronized (this) {
            connectionPool.values().forEach(this::removeClosedConnections);
            long now = clock.getAsLong();
            for (CachedConnection connection : tracked.values()) {
                if (connection.retired && now - connection.retiredAt >= RETIREMENT_GRACE_MS) {
                    logger.debug("ParkedConnection expired: {}, outstanding borrowers: {}", connection.get(), connection.borrowers.get());
                    queueClose(connection);
                }
            }
        }
        closeRetiredConnections();
    }

    /** @return all tracked transports, including evicted transports still borrowed */
    public int numberOfConnections() { return tracked.size(); }

    /** Clear pool metadata without closing caller-owned transports, including in-flight requests. */
    public synchronized void clear() {
        pendingClose.clear();
        tracked.clear();
        connectionPool.clear();
        logger.debug("Cleared client connection pool metadata");
    }

    private static final class CachedConnection {
        private final AtomicInteger requestCount = new AtomicInteger();
        private final ClientConnection clientConnection;
        private final long lifeStartTime;
        private final int maxReqCount;
        private final long expireTime;
        private final AtomicInteger borrowers = new AtomicInteger(1);
        private volatile ConnectionStatus status = ConnectionStatus.HANGING;
        private volatile boolean retired;
        private volatile long retiredAt;
        private final AtomicBoolean closing = new AtomicBoolean();

        private CachedConnection(ClientConnection connection, long now, RequestConfig config) {
            clientConnection = connection;
            lifeStartTime = now;
            maxReqCount = config.getMaxReqPerConn();
            expireTime = config.getConnectionExpireTime();
        }
        private boolean isOpen(long now) {
            return now - lifeStartTime < expireTime
                    && (maxReqCount == -1 || requestCount.get() < maxReqCount) && clientConnection.isOpen();
        }
        private ClientConnection get() { return clientConnection; }
    }
}
