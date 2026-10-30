package com.networknt.client.http;

import com.networknt.client.ClientConfig;
import io.undertow.client.ClientConnection;
import org.xnio.IoUtils;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * URI LRU pool. Evicted transports remain tracked until their borrowers return them.
 * @author Jiachen Sun
 */
public class Http2ClientConnectionPool {
    private enum ConnectionStatus { HANGING, AVAILABLE, CLOSE, MULTIPLEX_SUPPORT }
    private static Http2ClientConnectionPool http2ClientConnectionPool;
    private final Map<String, List<CachedConnection>> connectionPool;
    private final Map<ClientConnection, ConnectionStatus> connectionStatusMap = new ConcurrentHashMap<>();
    private final Map<ClientConnection, CachedConnection> tracked = new IdentityHashMap<>();
    private final List<ClientConnection> pendingClose = new ArrayList<>();
    private final AtomicInteger connectionCount = new AtomicInteger();

    private Http2ClientConnectionPool() {
        int poolSize = ClientConfig.get().getRequest().getConnectionPoolSize();
        connectionPool = new LinkedHashMap<>((int) Math.ceil(poolSize / 0.75f) + 1, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<String, List<CachedConnection>> eldest) {
                if (size() <= poolSize) return false;
                eldest.getValue().forEach(Http2ClientConnectionPool.this::retire);
                return true;
            }
        };
    }

    /** @return the shared connection pool */
    public static Http2ClientConnectionPool getInstance() {
        if (http2ClientConnectionPool == null) http2ClientConnectionPool = new Http2ClientConnectionPool();
        return http2ClientConnectionPool;
    }

    /**
     * Borrow a cached connection. Return each borrow with resetConnectionStatus.
     * @param uri backend URI
     * @return a connection, or null when none is selectable
     */
    public ClientConnection getConnection(URI uri) {
        try {
            synchronized (this) {
                if (uri == null) return null;
                CachedConnection selected = selectConnection(connectionPool.get(uri.toString()), false);
                if (selected == null) return null;
                hangConnection(selected);
                selected.borrowers++;
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
        ClientConnection owned = IoThreadClientConnection.wrap(connection);
        CachedConnection added = null;
        try {
            synchronized (this) {
                ClientConnection identity = IoThreadClientConnection.identity(owned);
                if (tracked.containsKey(identity)) return;
                getAndRemoveClosedConnection(uri);
                added = new CachedConnection(owned);
                tracked.put(identity, added);
                connectionCount.incrementAndGet();
                List<CachedConnection> connections = connectionPool.computeIfAbsent(uri.toString(), key -> new LinkedList<>());
                connections.add(added);
                if (!connectionPool.containsKey(uri.toString())) retire(added);
            }
            CachedConnection registered = added;
            owned.addCloseListener(ignored -> connectionClosed(registered));
        } finally { closeRetiredConnections(); }
    }

    private CachedConnection getAndRemoveClosedConnection(URI uri) {
        return uri == null ? null : selectConnection(connectionPool.get(uri.toString()), true);
    }

    private CachedConnection selectConnection(List<CachedConnection> connections, boolean removeClosed) {
        if (connections == null) return null;
        trimConnections(connections);
        if (removeClosed) removeClosedConnections(connections);
        if (connections.isEmpty()) return null;
        int offset = ThreadLocalRandom.current().nextInt(connections.size());
        for (int i = 0; i < connections.size(); i++) {
            CachedConnection candidate = connections.get((i + offset) % connections.size());
            ConnectionStatus status = getConnectionStatus(candidate);
            if (status == ConnectionStatus.AVAILABLE || status == ConnectionStatus.MULTIPLEX_SUPPORT) return candidate;
        }
        return null;
    }

    private void trimConnections(List<CachedConnection> connections) {
        if (connections.size() <= ClientConfig.get().getRequest().getMaxConnectionNumPerHost() * 0.75) return;
        int minimum = Math.max(0, ClientConfig.get().getRequest().getMinConnectionNumPerHost());
        while (connections.size() > minimum) retire(connections.remove(0));
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
        if (connection == null || connection.retired || !connection.isOpen()) return ConnectionStatus.CLOSE;
        if (connection.isHttp2Connection()) return ConnectionStatus.MULTIPLEX_SUPPORT;
        return connectionStatusMap.get(IoThreadClientConnection.identity(connection.get()));
    }

    /**
     * Return a borrow after its complete response or failure; retired transports close on the last return.
     * @param connection the returned transport
     */
    public void resetConnectionStatus(ClientConnection connection) {
        if (connection == null) return;
        try {
            synchronized (this) {
                ClientConnection identity = IoThreadClientConnection.identity(connection);
                CachedConnection cached = tracked.get(identity);
                if (cached != null && cached.borrowers > 0) cached.borrowers--;
                if (cached != null && cached.retired) releaseRetired(cached);
                else updateConnectionStatus(connection, identity);
            }
        } finally { closeRetiredConnections(); }
    }

    private void updateConnectionStatus(ClientConnection connection, ClientConnection identity) {
        if (!connection.isOpen()) connectionStatusMap.remove(identity);
        else connectionStatusMap.put(identity, connection.isMultiplexingSupported()
                ? ConnectionStatus.MULTIPLEX_SUPPORT : ConnectionStatus.AVAILABLE);
    }

    private void hangConnection(CachedConnection connection) {
        connection.requestCount.incrementAndGet();
        ConnectionStatus status = getConnectionStatus(connection);
        if ((status == null && !connection.isHttp2Connection()) || status == ConnectionStatus.AVAILABLE) {
            connectionStatusMap.put(IoThreadClientConnection.identity(connection.get()), ConnectionStatus.HANGING);
        }
    }

    private void retire(CachedConnection connection) {
        connection.retired = true;
        connectionStatusMap.remove(IoThreadClientConnection.identity(connection.get()));
        releaseRetired(connection);
    }

    private void releaseRetired(CachedConnection connection) {
        if (connection.borrowers > 0 && connection.get().isOpen()) return;
        ClientConnection identity = IoThreadClientConnection.identity(connection.get());
        if (!connection.closing && tracked.get(identity) == connection) {
            connection.closing = true;
            pendingClose.add(connection.get());
        }
    }

    private boolean removeTracked(CachedConnection connection) {
        ClientConnection identity = IoThreadClientConnection.identity(connection.get());
        if (!tracked.remove(identity, connection)) return false;
        connectionStatusMap.remove(identity);
        connectionCount.decrementAndGet();
        return true;
    }

    private synchronized void connectionClosed(CachedConnection connection) {
        removeTracked(connection);
    }

    private void closeRetiredConnections() {
        List<ClientConnection> closing;
        synchronized (this) {
            closing = new ArrayList<>(pendingClose);
            pendingClose.clear();
        }
        // Native close and user listeners must never run under the pool monitor.
        closing.forEach(IoUtils::safeClose);
    }

    /** @return all tracked transports, including evicted transports still borrowed */
    public int numberOfConnections() { return connectionCount.get(); }

    /** Close all tracked transports and clear their pool metadata. */
    public void clear() {
        List<ClientConnection> closing;
        synchronized (this) {
            closing = new ArrayList<>(pendingClose);
            tracked.values().forEach(connection -> closing.add(connection.get()));
            pendingClose.clear();
            tracked.clear();
            connectionPool.clear();
            connectionStatusMap.clear();
            connectionCount.set(0);
        }
        closing.forEach(IoUtils::safeClose);
    }

    private static final class CachedConnection {
        private final AtomicInteger requestCount = new AtomicInteger();
        private final ClientConnection clientConnection;
        private final long lifeStartTime = System.currentTimeMillis();
        private final int maxReqCount = ClientConfig.get().getRequest().getMaxReqPerConn();
        private final long expireTime = ClientConfig.get().getRequest().getConnectionExpireTime();
        private int borrowers = 1;
        private boolean retired;
        private boolean closing;

        private CachedConnection(ClientConnection connection) { clientConnection = connection; }
        private boolean isOpen() {
            return System.currentTimeMillis() - lifeStartTime < expireTime
                    && (maxReqCount == -1 || requestCount.get() < maxReqCount) && clientConnection.isOpen();
        }
        private boolean isHttp2Connection() { return clientConnection.isMultiplexingSupported(); }
        private ClientConnection get() { return clientConnection; }
    }
}
