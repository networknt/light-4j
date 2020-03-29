package com.networknt.client.http;

import com.networknt.client.ClientConfig;
import io.undertow.client.ClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A connection pool that caches connections based on the peer address by using LRU.
 * To optimize the establishment mechanism of http/1.1 and http/2 connections.
 *
 * @author Jiachen Sun
 */
public class Http2ClientConnectionPool {
    private enum ConnectionStatus {HANGING, AVAILABLE, CLOSE, MULTIPLEX_SUPPORT}

    private static final Logger logger = LoggerFactory.getLogger(Http2ClientConnectionPool.class);

    private final Map<String, List<CachedConnection>> connectionPool;

    private final Map<ClientConnection, ConnectionStatus> connectionStatusMap;

    private static Http2ClientConnectionPool http2ClientConnectionPool;

    private AtomicInteger connectionCount;

    private Http2ClientConnectionPool() {
        int poolSize = ClientConfig.get().getConnectionPoolSize();
        connectionCount = new AtomicInteger(0);
        // Initialize a LRU to cache the ClientConnection based on peer address
        connectionPool = Collections.synchronizedMap(new LinkedHashMap<String, List<CachedConnection>>((int) Math.ceil(poolSize / 0.75f) + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, List<CachedConnection>> eldest) {
                if (connectionPool.size() > poolSize) {
                    for (CachedConnection connection : eldest.getValue()) {
                        connectionStatusMap.remove(connection.get());
                        connectionCount.getAndDecrement();
                    }
                    return true;
                }
                return false;
            }
        });
        connectionStatusMap = new ConcurrentHashMap<>((int) Math.ceil(poolSize / 0.75f) + 1, 0.75f);
    }

    public static Http2ClientConnectionPool getInstance() {
        if (http2ClientConnectionPool == null) {
            http2ClientConnectionPool = new Http2ClientConnectionPool();
        }
        return http2ClientConnectionPool;
    }

    public synchronized ClientConnection getConnection(URI uri) {
        if (uri == null) {
            return null;
        }
        String uriString = uri.toString();
        List<CachedConnection> result = connectionPool.get(uriString);
        if (result == null) {
            synchronized (Http2ClientConnectionPool.class) {
                result = connectionPool.get(uriString);
            }
        }
        CachedConnection cachedConnection = selectConnection(result, false);
        if (cachedConnection != null) {
            hangConnection(cachedConnection);
            return cachedConnection.get();
        }
        return null;
    }

    public synchronized void cacheConnection(URI uri, ClientConnection connection) {
        CachedConnection cachedConnection = getAndRemoveClosedConnection(uri);
        if (cachedConnection == null || getConnectionStatus(cachedConnection) != ConnectionStatus.MULTIPLEX_SUPPORT) {
            CachedConnection newConnection = new CachedConnection(connection);
            connectionPool.computeIfAbsent(uri.toString(), k -> new LinkedList<>()).add(newConnection);
            connectionCount.getAndIncrement();
        }
    }

    private synchronized CachedConnection getAndRemoveClosedConnection(URI uri) {
        if (uri == null) {
            return null;
        }
        String uriString = uri.toString();
        List<CachedConnection> result = connectionPool.get(uriString);
        if (result == null) {
            synchronized (Http2ClientConnectionPool.class) {
                result = connectionPool.get(uriString);
            }
        }
        return selectConnection(result, true);
    }

    private synchronized CachedConnection selectConnection(List<CachedConnection> connections, boolean isRemoveClosedConnection) {
        if (connections != null) {
            if (connections.size() > ClientConfig.get().getMaxConnectionNumPerHost() * 0.75) {
                while (connections.size() > ClientConfig.get().getMinConnectionNumPerHost() && connections.size() > 0) {
                    connections.remove(0);
                }
            }
            if (isRemoveClosedConnection) {
                Iterator<CachedConnection> iterator = connections.iterator();
                while (iterator.hasNext()) {
                    CachedConnection connection = iterator.next();
                    if (connection != null) {
                        ConnectionStatus status = getConnectionStatus(connection);
                        if (ConnectionStatus.CLOSE == status) {
                            // Remove unavailable connection from the list.
                            iterator.remove();
                               connectionStatusMap.remove(connection.get());
                            connectionCount.getAndDecrement();
                        }
                    }
                }
            }
            if (connections.size() > 0) {
                // Balance the selection of each connection
                int randomInt = ThreadLocalRandom.current().nextInt(0, connections.size());
                for (int i = 0; i < connections.size(); i++) {
                    CachedConnection connection = connections.get((i + randomInt) % connections.size());
                    ConnectionStatus status = getConnectionStatus(connection);
                    // Return non-hanging connection
                    if (status == ConnectionStatus.AVAILABLE || status == ConnectionStatus.MULTIPLEX_SUPPORT) {
                        return connection;
                    }
                }
            }
        }
        return null;
    }

    private ConnectionStatus getConnectionStatus(CachedConnection connection) {
        ConnectionStatus status = connectionStatusMap.get(connection.get());
        if (status == null) {
            synchronized (Http2ClientConnectionPool.class) {
                status = connectionStatusMap.get(connection.get());
            }
        }
        if (connection == null || !connection.isOpen()) {
            return ConnectionStatus.CLOSE;
        } else if (connection.isHttp2Connection()) {
            return ConnectionStatus.MULTIPLEX_SUPPORT;
        }
        return status;
    }

    public void resetConnectionStatus(ClientConnection connection) {
        if (connection != null) {
            if (!connection.isOpen()) {
                connectionStatusMap.remove(connection);
            } else if (connection.isMultiplexingSupported()) {
                connectionStatusMap.put(connection, ConnectionStatus.MULTIPLEX_SUPPORT);
            } else {
                connectionStatusMap.put(connection, ConnectionStatus.AVAILABLE);
            }
        }
    }

    private void hangConnection(CachedConnection connection) {
        if (connection != null) {
            // Increase the request count for the connection for every invocation
            connection.incrementRequestCount();
            ConnectionStatus status = getConnectionStatus(connection);
            // Hanging new or old Http/1.1 connection
            if ((status == null && !connection.isHttp2Connection()) || status == ConnectionStatus.AVAILABLE) {
                connectionStatusMap.put(connection.get(), ConnectionStatus.HANGING);
            }
        }
    }

    public int numberOfConnections() {
        return connectionCount.get();
    }

    public void clear() {
        this.connectionCount = new AtomicInteger(0);
        connectionStatusMap.clear();
        connectionPool.clear();
    }

    private class CachedConnection {
        private AtomicInteger requestCount;
        private ClientConnection clientConnection;
        private long lifeStartTime;
        private int maxReqCount = ClientConfig.get().getMaxRequestPerConnection();
        private long expireTime = ClientConfig.get().getConnectionExpireTime();

        protected CachedConnection(ClientConnection connection) {
            requestCount = new AtomicInteger(0);
            this.clientConnection = connection;
            this.lifeStartTime = System.currentTimeMillis();
        }

        public boolean isOpen() {
            if (System.currentTimeMillis() - lifeStartTime >= expireTime || (requestCount.get() >= maxReqCount && maxReqCount != -1)) {
                logger.debug("Connection expired.");
                try {
                    this.clientConnection.close();
                } catch (Exception ignored) {
                }
                return false;
            }
            return this.clientConnection.isOpen();
        }

        protected boolean isHttp2Connection() {
            return this.clientConnection.isMultiplexingSupported();
        }

        public ClientConnection get() {
            return this.clientConnection;
        }

        protected void incrementRequestCount() {
            this.requestCount.getAndIncrement();
        }
    }
}
