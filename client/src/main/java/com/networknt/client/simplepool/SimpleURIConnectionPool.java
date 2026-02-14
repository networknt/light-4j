/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author miklish Michael N. Christoff
 *
 * testing / QA
 *   AkashWorkGit
 *   jaydeepparekh1311
 */
package com.networknt.client.simplepool;

import io.undertow.connector.ByteBufferPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;
import org.xnio.XnioWorker;
import org.xnio.ssl.XnioSsl;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/***
    A connection pool for a single URI.
    Connection pool contains 4 Sets of SimpleConnectionStates:

        1. allCreatedConnections:   all connections created by connection makers are added to this set
        2. trackedConnections:      the set of all connections tracked by the connection pool
        3. Borrowable:              connection that can be borrowed from
        4. Borrowed:                connections that have borrowed tokens
        5. notBorrowedExpired:      connections that have no borrowed tokens -- only these can be closed by the pool
*/
public final class SimpleURIConnectionPool {
    private static final Logger logger = LoggerFactory.getLogger(SimpleURIConnectionPool.class);
    private final SimpleConnectionMaker connectionMaker;
    private final long EXPIRY_TIME;
    private final int poolSize;
    private final URI uri;
    private InetSocketAddress bindAddress;
    private XnioWorker worker;
    private ByteBufferPool bufferPool;
    private XnioSsl ssl;
    private OptionMap options;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();


    /** Connection Pool Sets
     *  These sets determine the mutable state of the connection pool
     */
    /** The set of all connections created by the SimpleConnectionMaker for this uri */
    private final Set<SimpleConnection> allCreatedConnections = ConcurrentHashMap.newKeySet();
    /** The set containing all connections known to this connection pool (It is not considered a state set) */
    private final Set<SimpleConnectionState> trackedConnections = new HashSet<>();
    /**
     * State Sets
     * The existence or non-existence of a connection in one of these sets means that the connection is or is not in
     * one of these states. A connection can be in multiple state sets at a time (e.g.: a connection can be both borrowed and borrowable) */
    private final Set<SimpleConnectionState> borrowable = new HashSet<>();
    private final Set<SimpleConnectionState> borrowed = new HashSet<>();
    private final Set<SimpleConnectionState> notBorrowedExpired = new HashSet<>();

    public SimpleURIConnectionPool(URI uri, long expireTime, int poolSize, SimpleConnectionMaker connectionMaker) {
        EXPIRY_TIME = expireTime;
        this.uri = uri;
        this.poolSize = poolSize;
        this.connectionMaker = connectionMaker;
    }

    public SimpleURIConnectionPool(URI uri, long expireTime, int poolSize, InetSocketAddress bindAddress, XnioWorker worker, ByteBufferPool bufferPool, XnioSsl ssl, OptionMap options, SimpleConnectionMaker connectionMaker) {
        EXPIRY_TIME = expireTime;
        this.uri = uri;
        this.poolSize = poolSize;
        this.bindAddress = bindAddress;
        this.worker = worker;
        this.bufferPool = bufferPool;
        this.ssl = ssl;
        this.options = options;
        this.connectionMaker = connectionMaker;
    }

    /***
     *
     * @param createConnectionTimeout the maximum time to wait for a connection to be created
     * @return a connection token that represents the borrowing of a connection by a thread
     * @throws RuntimeException if an attempt is made to exceed the maximum size of the connection pool
     */
    public SimpleConnectionState.ConnectionToken borrow(long createConnectionTimeout) throws RuntimeException {
        lock.writeLock().lock();
        try {
            findAndCloseLeakedConnections();
            long now = System.currentTimeMillis();
            final SimpleConnectionState state;

            applyAllConnectionStates(now);

            if (borrowable.size() > 0) {
                state = borrowable.toArray(new SimpleConnectionState[0])[ThreadLocalRandom.current().nextInt(borrowable.size())];
            } else {
                if (trackedConnections.size() < poolSize) {
                    state = new SimpleConnectionState(EXPIRY_TIME, createConnectionTimeout, uri, bindAddress, worker, bufferPool, ssl, options, allCreatedConnections, connectionMaker);
                    trackedConnections.add(state);
                } else
                    throw new RuntimeException("An attempt was made to exceed the maximum size was of the " + uri.toString() + " connection pool");
            }

            SimpleConnectionState.ConnectionToken connectionToken = state.borrow(now);
            applyConnectionState(state, now, () -> trackedConnections.remove(state));

            logger.debug(showConnections("borrow"));

            return connectionToken;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /***
     * Restores borrowed connections
     *
     * NOTE: A connection that unexpectedly closes may be removed from connection pool tracking before all of its
     *       ConnectionTokens have been restored. This can result in seeing log messages about CLOSED connections
     *       being restored to the pool that are no longer tracked / known by the connection pool
     *
     * @param connectionToken the connection token that represents the borrowing of a connection by a thread
     */
    public void restore(SimpleConnectionState.ConnectionToken connectionToken) {
        lock.writeLock().lock();
        try {
            findAndCloseLeakedConnections();
            if (connectionToken == null)
                return;

            SimpleConnectionState state = connectionToken.holder();
            long now = System.currentTimeMillis();

            state.restore(connectionToken);
            applyAllConnectionStates(now);

            logger.debug(showConnections("restore"));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * A key method that orchestrates the update of the connection pool's state
     * It is guaranteed to run every time a transition method is called on SimpleURIConnectionPool
     *
     * NOTE: Thread Safety
     *     This method is private, and is only called either directly or transitively by synchronized
     *     methods in this class.
     *
     * @param now the current time in ms
     */
    private void applyAllConnectionStates(long now)
    {
        /**
         * Sweep all known connections and update sets
         *
         * Remove any connections that have unexpectedly closed
         * Move all remaining connections to appropriate sets based on their properties
         */
        final Iterator<SimpleConnectionState> knownConnectionStates = trackedConnections.iterator();
        while (knownConnectionStates.hasNext()) {
            SimpleConnectionState connection = knownConnectionStates.next();

            // remove connections that have unexpectedly closed
            if (connection.closed()) {
                logger.debug("[{}: CLOSED]: Connection unexpectedly closed - Removing from known-connections set", port(connection.connection()));
                applyConnectionState(connection, now, knownConnectionStates::remove);
            }
            // else, move connections to correct sets
            else {
                applyConnectionState(connection, now, knownConnectionStates::remove);

                // close and remove connections if they are in a closeable set
                if (notBorrowedExpired.contains(connection)) {
                    connection.safeClose(now);
                    applyConnectionState(connection, now, knownConnectionStates::remove);
                }
            }
        }

    }

    private interface RemoveFromTrackedConnections { void remove(); }
    /***
     * This method reads a connection and moves it to the correct sets based on its properties.
     * It will also remove a connection from all sets (i.e.: stop tracking the connection) if it is closed.
     *
     * NOTE: Closing connections and modifying sets
     *     applyConnectionState() and findAndCloseLeakedConnections() are the only two methods that close connections
     *     and modify sets. This can be helpful to know for debugging since the sets comprise the entirety of the
     *     mutable state of this SimpleURIConnectionPool objects
     *
     * NOTE: Thread Safety
     *     This method is private, and is only called either directly or transitively by synchronized
     *     methods in this class.
     *
     * @param connection
     * @param now
     * @param knownConnections a lambda expression to remove a closed-connection from trackedConnections, either using
     *                         an Iterator of trackedConnections, or directly using trackedConnections.remove()
     */
    private void applyConnectionState(SimpleConnectionState connection, long now, RemoveFromTrackedConnections knownConnections) {

        /**
         * Remove all references to closed connections
         * After the connection is removed, the only reference to it will be in any unrestored ConnectionTokens,
         * however, ConnectionTokens restored after the connection is closed will not be re-added to any sets
         * (and will therefore be garbage collected)
         */
        if(connection.closed()) {
            logger.debug("[{}: CLOSED]: Connection closed - Stopping connection tracking", port(connection.connection()));

            allCreatedConnections.remove(connection.connection());  // connection.connection() returns a SimpleConnection
            knownConnections.remove();  // this will remove the connection from trackedConnections directly, or via Iterator

            borrowable.remove(connection);
            borrowed.remove(connection);
            notBorrowedExpired.remove(connection);
            return;
        }

        // if connection is open, move it to the correct state-sets based on its properties
        boolean isExpired =             connection.expired(now);
        boolean isBorrowed =            connection.borrowed();
        boolean isBorrowable =          connection.borrowable(now);
        boolean isNotBorrowedExpired =  !isBorrowed && isExpired;

        updateSet(borrowable, isBorrowable, connection);
        updateSet(borrowed, isBorrowed, connection);
        updateSet(notBorrowedExpired, isNotBorrowedExpired, connection);
    }

    /***
     * Takes a Set, a boolean, and a connectionState
     * If the boolean is true, it will add the connectionState to the Set, otherwise, it will remove it from the Set
     *
     * NOTE: Thread Safety
     *     This method is private, and is only called either directly or transitively by synchronized
     *     methods in this class.
     *
     * @param set the set to potentially add or remove the connectionState from
     * @param isMember if true, it will add connectionState to set, otherwise, it will remove connectionState from set
     * @param connectionState the connectionState to add or remove from the set
     */
    private void updateSet(Set<SimpleConnectionState> set, boolean isMember, SimpleConnectionState connectionState) {
        if(isMember)
            set.add(connectionState);
        else
            set.remove(connectionState);
    }

    /**
     * Remove leaked connections
     * A leaked connection is any connection that was created by a SimpleConnectionMaker, but was not returned by the
     * SimpleConnectionState.borrow() method. This can happen if an error occurs (specifically, if an exception is
     * thrown) during the creation of a SimpleConnectionState. A SimpleConnectionState can fail to instantiate
     * (after it has created a new connection) if, for example:
     *
     *     1) the connection-creation callback thread finishes creating the connection after a timeout has occurred
     *     2) the raw connection unexpectedly closes during the creation of its SimpleConnectionState
     *
     * NOTE: Closing connection and modifying sets
     *     applyConnectionState() and findAndCloseLeakedConnections() are the only two methods that close connections
     *     and modify sets. This can be helpful to know for debugging since the sets comprise the entirety of the
     *     mutable state of this SimpleURIConnectionPool objects
     */
    private void findAndCloseLeakedConnections()
    {
        // remove all connections that the connection pool is tracking, from the set of all created connections
        for(SimpleConnectionState knownConnection: trackedConnections)
            allCreatedConnections.remove(knownConnection.connection());

        // any remaining connections are leaks, and can now be safely closed
        if(allCreatedConnections.size() > 0) {
            logger.debug("{} untracked connection found", allCreatedConnections.size());

            Iterator<SimpleConnection> leakedConnections = allCreatedConnections.iterator();
            while(leakedConnections.hasNext()) {
                SimpleConnection leakedConnection = leakedConnections.next();

                if(leakedConnection.isOpen()) {
                    leakedConnection.safeClose();
                    logger.debug("Connection closed {} -> {}", port(leakedConnection), uri.toString());
                } else
                    logger.debug("Connection was already closed {} -> {}", port(leakedConnection), uri.toString());

                leakedConnections.remove();
            }
        }
    }


    /***
     * For logging
     *
     * NOTE: Thread Safety
     *     This method is private, and is only called either directly or transitively by synchronized
     *     methods in this class.
     *
     * NOTE: Iteration Safety
     *     This method should not be used inside loops that iterate through elements of borrowable, borrowed,
     *     notBorrowedExpired, or trackedConnections sets
     */
    private String showConnections(String transitionName) {
        return "After " + transitionName + " - " +
                showConnections("BORROWABLE", borrowable) +
                showConnections("BORROWED", borrowed) +
                showConnections("NOT_BORROWED_EXPIRED", notBorrowedExpired) +
                showConnections("TRACKED", trackedConnections);
    }

    /***
     * For logging
     *
     * NOTE: Thread Safety
     *     This method is private, and is only called either directly or transitively by synchronized
     *     methods in this class.
     */
    private static String showConnections(String name, Set<SimpleConnectionState> set) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(name).append(": ");
        if(set.size() == 0)
            sb.append("0");
        else {
            int numCons = set.size();
            for (SimpleConnectionState state : set) {
                sb.append(port(state.connection()));
                if (--numCons > 0) sb.append(" ");
            }
        }
        sb.append("] ");
        return sb.toString();
    }

    /***
     * For logging
     *
     * NOTE: Thread Safety
     *     This method is private, and is only called either directly or transitively by synchronized
     *     methods in this class.
     */
    private static String port(SimpleConnection connection) {
        if(connection == null) return "NULL";
        String url = connection.getLocalAddress();
        int semiColon = url.lastIndexOf(":");
        if(semiColon == - 1) return "PORT?";
        return url.substring(url.lastIndexOf(":")+1);
    }

    /**
     * Validates all connections and removes stale/closed ones.
     * Called by the health checker to proactively clean the pool.
     *
     * @return the number of connections that were cleaned up
     */
    public int validateAndCleanConnections() {
        lock.writeLock().lock();
        try {
            long now = System.currentTimeMillis();
            int initialSize = trackedConnections.size();

            // Trigger the standard read which cleans up closed/expired connections
            applyAllConnectionStates(now);

            int cleaned = initialSize - trackedConnections.size();
            if (cleaned > 0) {
                logger.debug("validateAndCleanConnections cleaned {} connections for {}", cleaned, uri);
            }
            return cleaned;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Pre-establishes connections for pool warm-up.
     * Creates the specified number of connections up to the pool limit.
     *
     * @param count number of connections to pre-establish
     * @param createConnectionTimeout timeout for connection creation in ms
     * @return the number of connections actually created
     */
    public int warmUp(int count, long createConnectionTimeout) {
        lock.writeLock().lock();
        try {
            int created = 0;
            long now = System.currentTimeMillis();

            for (int i = 0; i < count && trackedConnections.size() < poolSize; i++) {
                try {
                    SimpleConnectionState state = new SimpleConnectionState(
                            EXPIRY_TIME, createConnectionTimeout, uri, bindAddress,
                            worker, bufferPool, ssl, options, allCreatedConnections, connectionMaker
                    );
                    trackedConnections.add(state);
                    applyConnectionState(state, now, () -> trackedConnections.remove(state));
                    created++;
                    logger.debug("warmUp: pre-established connection {} of {} for {}", created, count, uri);
                } catch (Exception e) {
                    logger.warn("warmUp: failed to create connection for {}: {}", uri, e.getMessage());
                    break;
                }
            }

            if (created > 0) {
                logger.info("warmUp: created {} connections for {}", created, uri);
            }
            return created;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the current number of active (tracked) connections.
     * @return the number of connections in trackedConnections
     */
    public int getActiveConnectionCount() {
        lock.readLock().lock();
        try {
            return trackedConnections.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the current number of borrowable connections.
     * @return the number of connections available to borrow
     */
    public int getBorrowableCount() {
        lock.readLock().lock();
        try {
            return borrowable.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the current number of borrowed connections.
     * @return the number of connections currently borrowed
     */
    public int getBorrowedCount() {
        lock.readLock().lock();
        try {
            return borrowed.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the URI this pool manages.
     * @return the URI
     */
    public URI getUri() {
        return uri;
    }
}
