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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import com.networknt.client.simplepool.exceptions.*;

/***
 SimpleURIConnectionPool is a connection pool for a single URI, which means that it manages a pool of reusable
 connections to a single URI.
 Threads borrow connections from the pool, use them, and then return them back to the pool. Since these returned
 connections are already active / established, they can be used immediately without going through the lengthy connection
 establishment process. This can very significantly increase the performance of systems that make many simultaneous
 outgoing API calls.

 Internally, SimpleURIConnectionPool organizes connections into 4 (possibly overlapping) sets:

 1. allCreatedConnections    all connections created for the pool by the pool's SimpleConnectionMaker
 2. trackedConnections:      all connections tracked by the connection pool
 3. Borrowable:              all tracked connections that can be borrowed from
 4. Borrowed:                all tracked connections that have borrowed tokens
 5. notBorrowedExpired:      all tracked connections that are both expired and not borrowed -- only these can be closed by the pool
 */
public final class SimpleURIConnectionPool {
    private static final Logger logger = LoggerFactory.getLogger(SimpleURIConnectionPool.class);
    private final SimpleConnectionMaker connectionMaker;
    private final long EXPIRY_TIME;
    private final int poolSize;
    private final URI uri;

    /** Connection Pool Sets
     *  These sets determine the mutable state of the connection pool
     */
    /** The set of all connections created for the pool by the pool's SimpleConnectionMaker */
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

    /***
     * Creates a new SimpleURIConnectionPool
     *
     * @param uri the URI that this connection pool creates connections to
     * @param expireTime the length of time in milliseconds a connection is eligible to be borrowed
     * @param poolSize the maximum number of unexpired connections the pool can hold at a given time
     * @param connectionMaker a class that SimpleConnectionPool uses to create new connections
     */
    public SimpleURIConnectionPool(URI uri, long expireTime, int poolSize, SimpleConnectionMaker connectionMaker) {
        EXPIRY_TIME = expireTime;
        this.uri = uri;
        this.poolSize = poolSize;
        this.connectionMaker = connectionMaker;
    }

    /***
     * Returns a network connection to a URI
     *
     * @param createConnectionTimeout The maximum time in seconds to wait for a new connection to be established
     * @param isHttp2 if true, SimpleURIConnectionPool will attempt to establish an HTTP/2 connection, otherwise it will
     *          attempt to create an HTTP/1.1 connection
     * @return a ConnectionToken object that contains the borrowed connection. The thread using the connection must
     *          return this connection to the pool when it is done with it by calling the restore() method with the
     *          ConnectionToken as the argument
     * @throws RuntimeException if connection creation takes longer than <code>createConnectionTimeout</code> seconds,
     *          or other issues that prevent connection creation
     */
    public synchronized SimpleConnectionState.ConnectionToken borrow(long createConnectionTimeout, boolean isHttp2) throws RuntimeException {
        findAndCloseLeakedConnections();
        long now = System.currentTimeMillis();

        final SimpleConnectionState connectionState;

        // update the connection pool's state
        applyAllConnectionStates(now);

        if(borrowable.size() > 0) {
            connectionState = borrowable.toArray(new SimpleConnectionState[0])[ThreadLocalRandom.current().nextInt(borrowable.size())];
        } else {
            if (trackedConnections.size() < poolSize) {
                connectionState = new SimpleConnectionState(EXPIRY_TIME, createConnectionTimeout, isHttp2, uri, allCreatedConnections, connectionMaker);
                trackedConnections.add(connectionState);
            } else
                throw new SimplePoolConnectionLimitReachedException(
                    "An attempt was made to exceed the maximum size of the " + uri.toString() + " connection pool");
        }

        SimpleConnectionState.ConnectionToken connectionToken = connectionState.borrow(now);
        applyConnectionState(connectionState, now, () -> trackedConnections.remove(connectionState));

        if(logger.isDebugEnabled()) logger.debug(showConnections("borrow"));
        return connectionToken;
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
    public synchronized void restore(SimpleConnectionState.ConnectionToken connectionToken) {
        findAndCloseLeakedConnections();

        if(connectionToken != null) {
            // restore connection token
            SimpleConnectionState connectionState = connectionToken.state();
            connectionState.restore(connectionToken);
        }

        long now = System.currentTimeMillis();

        // update the connection pool's state
        applyAllConnectionStates(now);

        if(logger.isDebugEnabled()) logger.debug(showConnections("restore"));
    }

    /**
     * A key method that orchestrates the update of the connection pool's state
     * It is guaranteed to run every time a transition method is called on SimpleURIConnectionPool
     *
     * NOTE: Thread Safety
     *     This method is private, and is only called either directly or transitively by synchronized
     *     methods in this class.
     *
     * @param now the Unix Epoch time in milliseconds at which to evaluate the connection states
     */
    private void applyAllConnectionStates(long now)
    {
        /**
         * Sweep all known (tracked) connections and apply their state changes to the connection pool's state. Also, close
         * any unborrowed expired connections
         */
        final Iterator<SimpleConnectionState> trackedConnectionStates = trackedConnections.iterator();
        while (trackedConnectionStates.hasNext())
            applyConnectionState(trackedConnectionStates.next(), now, () -> trackedConnectionStates.remove());
    }

    @FunctionalInterface private interface RemoveFromTrackedConnections { void remove(); }
    /***
     * This method reads a connection and moves it to the correct sets based on its properties.
     * It will remove a connection from all sets (i.e.: stop tracking the connection) if it is closed.
     * It will also close unborrowed expired connections.
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
     * @param connectionState the connection to read and move to the appropriate sets
     * @param now the Unix Epoch time in milliseconds at which to evaluate the connection's state
     * @param trackedConnectionRemover a lambda expression to remove a closed-connection from trackedConnections, either using
     *                                 an Iterator of trackedConnections, or directly using trackedConnections.remove()
     */
    private void applyConnectionState(SimpleConnectionState connectionState, long now, RemoveFromTrackedConnections trackedConnectionRemover) {

        // Remove all references to closed connections
        if(connectionState.closed()) {
            if(logger.isDebugEnabled())
                logger.debug("[{}: CLOSED]: Connection unexpectedly closed - Stopping connection tracking", port(connectionState.connection()));

            removeFromConnectionTracking(connectionState, trackedConnectionRemover);
            return;
        }

        // if connection is open, move it to the correct state-sets based on its properties
        boolean isExpired =             connectionState.expired(now);
        boolean isBorrowed =            connectionState.borrowed();
        boolean isBorrowable =          connectionState.borrowable(now);
        boolean isNotBorrowedExpired =  !isBorrowed && isExpired;

        updateSet(borrowable, isBorrowable, connectionState);
        updateSet(borrowed, isBorrowed, connectionState);
        updateSet(notBorrowedExpired, isNotBorrowedExpired, connectionState);

        // close and remove connection if it is in a closeable set
        if (notBorrowedExpired.contains(connectionState))
        {
            connectionState.safeClose(now);
            removeFromConnectionTracking(connectionState, trackedConnectionRemover);

            if(logger.isDebugEnabled())
                logger.debug("[{}: CLOSED]: Expired connection was closed - Connection tracking stopped", port(connectionState.connection()));
        }
    }

    /***
     * Removes all references to a connection (and its connection state) from being tracked by the pool
     *
     * NOTE: Only call this method on closed connections
     *
     * After the connection is removed, the only reference to it will be in any unrestored ConnectionTokens.
     * However, ConnectionTokens restored after the connection is closed will not be re-added to any sets
     * (and will therefore be garbage collected)
     *
     * @param connectionState the connection state (and connection) to remove from connection tracking
     * @param trackedConnectionRemover a lamda expression to remove the state that depends on whether it is removed in
     *          an <code>Iterator</code> loop or directly from the trackedConnections <code>Set</code>
     */
    private void removeFromConnectionTracking(SimpleConnectionState connectionState, RemoveFromTrackedConnections trackedConnectionRemover)
    {
        if(logger.isDebugEnabled()) {
            if(!trackedConnections.contains(connectionState))
                logger.debug("connection [{}] was already removed from connection tracking", port(connectionState.connection()));
        }

        allCreatedConnections.remove(connectionState.connection());
        trackedConnectionRemover.remove();  // this will remove the connection from trackedConnections directly, or via Iterator

        borrowable.remove(connectionState);
        borrowed.remove(connectionState);
        notBorrowedExpired.remove(connectionState);
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
     * NOTE: Closing connections and modifying sets
     *     applyConnectionState() and findAndCloseLeakedConnections() are the only two methods that close connections
     *     and modify sets. This can be helpful to know for debugging since the sets comprise the entirety of the
     *     mutable state of this SimpleURIConnectionPool objects
     *
     * NOTE: This method must not throw any exceptions
     */
    private void findAndCloseLeakedConnections()
    {
        // remove all connections that the connection pool is tracking, from the set of all created connections
        for(SimpleConnectionState trackedConnection: trackedConnections)
            allCreatedConnections.remove(trackedConnection.connection());

        // any remaining connections are leaks, and can now be safely closed
        if(allCreatedConnections.size() > 0) {
            if(logger.isDebugEnabled()) logger.debug("{} untracked connection(s) found", allCreatedConnections.size());

            Iterator<SimpleConnection> leakedConnections = allCreatedConnections.iterator();
            while(leakedConnections.hasNext()) {
                SimpleConnection leakedConnection = leakedConnections.next();

                if(leakedConnection.isOpen()) {
                    leakedConnection.safeClose();
                    if(logger.isDebugEnabled()) logger.debug("Connection closed {} -> {}", port(leakedConnection), uri.toString());
                } else {
                    if (logger.isDebugEnabled()) logger.debug("Connection was already closed {} -> {}", port(leakedConnection), uri.toString());
                }

                leakedConnections.remove();
            }
        }
    }

    /***
     * This is a convenience method that immediately closes a connection even if there are still threads actively
     * using it (i.e: it will be closed even if it is currently borrowed by other threads).
     *
     * 1. WARNING: Connection Token must still be returned: After closing a connection, users must still return the
     *     connection token to the pool.
     *
     * 2. WARNING: Overuse of this method will negate all benefits of the connection pool: Under normal circumstances,
     *     users should never close connections themselves (either via this method or directly via the raw connection)
     *     but instead, always let the connection pool handle all connection closures. Manually closing connections
     *     negates all benefits of using a connection pool. Be certain that this method is only used in cases where
     *     there is a need to ensure the connection is immediately closed in all threads using it.
     *
     * 3. All threads sharing this connection will be affected: Closing connections yourself (using this method or
     *     directly via the raw connection) will cause all threads that are currently using this connection to experience
     *     unexpected connection failures.
     *
     * 4. This method is a convenience method: This method is a convenience method to allow users to close connections
     *      via the SimplePool API rather than closing the raw connection directly. However, if there is a concrete need
     *      to do so (see point 2 above) users can also close raw connections directly at any time without calling this
     *      method since SimpleURIConnectionPool will safely handle unexpected closures of raw connections. For example:
     *
     *          // get raw Undertow connection
     *          ClientConnection connection = (ClientConnection) connectionToken.getRawConnection();
     *
     *          ...
     *
     *          if(isMajorConnectionIssueDetected)
     *              IoUtils.safeClose(connection);     // safely close the raw connection directly
     *
     *   This means that users can leverage this method to close connections if it is convenient, but can also close
     *   the raw connection directly at any time if that is more convenient (and after considering item 2 above).
     *
     * @param connectionToken the connection token of the connection to close
     */
    public synchronized void safeClose(SimpleConnectionState.ConnectionToken connectionToken) {
        findAndCloseLeakedConnections();

        if(connectionToken == null)
            return;

        long now = System.currentTimeMillis();
        SimpleConnection connection = connectionToken.connection();
        final SimpleConnectionState connectionState = connectionToken.state();

        // must bypass ConnectionState protections and close connection directly
        if(logger.isDebugEnabled()) logger.debug("immediately closing connection [{}]", port(connection));
        connection.safeClose();

        // update pool about state change to this connection
        applyConnectionState(connectionState, now, () -> trackedConnections.remove(connectionState));
    }

    /***
     * Causes the connection to be closed as soon as all threads currently using it have restored it to the pool. This
     * prevents the connection from ever being reused, while also preventing threads that are currently using it from
     * experiencing unexpected connection closures.
     *
     * This method 'expires' a connection which results in:
     *     (a) the connection no longer being borrowable, and
     *     (b) the connection being closed as soon as all threads currently using it have restored it to the pool.
     *
     * 1. WARNING: Connection Token must still be returned: After closing a connection, users must still return the
     *     connection token to the pool.
     *
     * 2. WARNING: Overuse of this method will negate all benefits of the connection pool: Under normal circumstances,
     *     users should never close connections themselves (either via this method or directly via the raw connection)
     *     but instead, always let the connection pool handle all connection closures. Manually closing connections
     *     negates all benefits of using a connection pool. Be certain that this method is only used in cases where
     *     there is a need to ensure the connection is not reused.
     *
     * @param connectionToken the connection token for the connection to close
     * @return true if the connection has been closed;
     *         false if (1) the connection is still open due to there being threads that are still actively using it,
     *         or (2) if the connectionToken was null
     */
    public synchronized boolean scheduleSafeClose(SimpleConnectionState.ConnectionToken connectionToken) {
        findAndCloseLeakedConnections();
        long now = System.currentTimeMillis();

        if(connectionToken == null)
            return false;

        // expire connection state
        SimpleConnectionState connectionState = connectionToken.state();
        connectionState.forceExpire();

        if(logger.isDebugEnabled()) logger.debug("closure scheduled for connection [{}]", port(connectionState.connection()));

        // update pool about state change to this connection
        applyConnectionState(connectionState, now, () -> trackedConnections.remove(connectionState));
        return connectionState.closed();
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
}
