package com.networknt.client.simplepool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/***
    A connection pool for a single URI.
    Connection pool contains 4 Sets of ConnectionHolders:

        1. All:                     the complete set of holders
        2. Borrowable:              holders that can be borrowed from
        3. Borrowed:                holders that have borrowed tokens
        4. Not Borrowed Expired:    expired holders that have no borrowed tokens -- these can be closed
*/
public final class SimpleURIConnectionPool {
    private static final Logger logger = LoggerFactory.getLogger(SimpleURIConnectionPool.class);
    private final SimpleConnectionMaker connectionMaker;
    private final long EXPIRY_TIME;
    private final int poolSize;
    private final URI uri;

    /***
     * The set of all connections created by the SimpleConnectionMaker for this uri
     */
    private final Set<SimpleConnection> allCreatedConnections = ConcurrentHashMap.newKeySet();

    /***
     * The set containing all connections known to this connection pool
     * (It is not considered a state set)
     */
    private final Set<SimpleConnectionHolder> allKnownConnections = new HashSet<>();

    /***
     * State Sets
     * The existence or non-existence of a connection in one of these sets means that the connection is in one of these
     * states or its opposite.
     *
     * A connection can be in multiple state sets at o time (e.g.: a connection can be both borrowable and not borrowed)
     */
    private final Set<SimpleConnectionHolder> borrowable = new HashSet<>();
    private final Set<SimpleConnectionHolder> borrowed = new HashSet<>();
    private final Set<SimpleConnectionHolder> notBorrowedExpired = new HashSet<>();

    public SimpleURIConnectionPool(URI uri, long expireTime, int poolSize, SimpleConnectionMaker connectionMaker) {
        EXPIRY_TIME = expireTime;
        this.uri = uri;
        this.poolSize = poolSize;
        this.connectionMaker = connectionMaker;
    }

    public synchronized SimpleConnectionHolder.ConnectionToken borrow(long createConnectionTimeout, boolean isHttp2) throws RuntimeException {
        long now = System.currentTimeMillis();
        final SimpleConnectionHolder holder;

        readAllConnectionHolders(now);

        if(borrowable.size() > 0) {
            holder = borrowable.toArray(new SimpleConnectionHolder[0])[ThreadLocalRandom.current().nextInt(borrowable.size())];
        } else {
            if (borrowed.size() < poolSize) {
                holder = new SimpleConnectionHolder(EXPIRY_TIME, createConnectionTimeout, isHttp2, uri, allCreatedConnections, connectionMaker);
                allKnownConnections.add(holder);
            } else
                throw new RuntimeException("An attempt was made to exceed the maximum size was of the " + uri.toString() + " connection pool");
        }

        SimpleConnectionHolder.ConnectionToken connectionToken = holder.borrow(createConnectionTimeout, now);
        readConnectionHolder(holder, now, () -> allKnownConnections.remove(holder));

        logger.debug(showConnections("borrow"));

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
    public synchronized void restore(SimpleConnectionHolder.ConnectionToken connectionToken) {
        if(connectionToken == null)
            return;

        SimpleConnectionHolder holder = connectionToken.holder();
        long now = System.currentTimeMillis();

        holder.restore(connectionToken);
        readAllConnectionHolders(now);

        logger.debug(showConnections("restore"));
    }

    /**
     * A key method that orchestrates the update of the connection pool's state
     * It is guaranteed to run every time a transition method is called on SimpleURIConnectionPool
     *
     * NOTE: Thread Safety
     *     This method is private, and is only called either directly or transitively by synchronized
     *     methods in this class.
     *
     * Note:
     *     'knownConnectionHolders::remove' is just Java syntactic sugar for '() -> knownConnectionHolders.remove()'
     *
     * @param now the current time in ms
     */
    private void readAllConnectionHolders(long now)
    {
        /**
         * Sweep all known connections and update sets
         *
         * Remove any connections that have unexpectedly closed
         * Move all remaining connections to appropriate sets based on their properties
         *
         */
        final Iterator<SimpleConnectionHolder> knownConnectionHolders = allKnownConnections.iterator();
        while (knownConnectionHolders.hasNext()) {
            SimpleConnectionHolder connection = knownConnectionHolders.next();

            // remove connections that have unexpectedly closed
            if (connection.closed()) {
                logger.debug("[{}: CLOSED]: Connection unexpectedly closed - Removing from known-connections set", port(connection.connection()));
                readConnectionHolder(connection, now, knownConnectionHolders::remove);
            }
            // else, move connections to correct sets
            else {
                readConnectionHolder(connection, now, knownConnectionHolders::remove);

                // close and remove connections if they are in a closeable set
                if (notBorrowedExpired.contains(connection)) {
                    connection.safeClose(now);
                    readConnectionHolder(connection, now, knownConnectionHolders::remove);
                }
            }
        }

        // find and close any leaked connections
        findAndCloseLeakedConnections();
    }

    private interface RemoveFromAllKnownConnections { void remove(); }
    /***
     * This method reads a connection and moves it to the correct sets based on its properties.
     * It will also remove a connection from all sets (i.e.: stop tracking the connection) if it is closed.
     *
     * NOTE: Connection closing methods
     *     This method and findAndCloseLeakedConnections() are the only two methods that close connections.
     *     This can be helpful to know for debugging
     *
     * NOTE: Thread Safety
     *     This method is private, and is only called either directly or transitively by synchronized
     *     methods in this class.
     *
     * @param connection
     * @param now
     * @param knownConnections a lambda expression to remove a closed-connection from allKnownConnections, either using
     *                         an Iterator of allKnownConnections, or directly using allKnownConnections.remove()
     */
    private void readConnectionHolder(SimpleConnectionHolder connection, long now, RemoveFromAllKnownConnections knownConnections) {

        /**
         * Remove all references to closed connections
         * After the connection is removed, the only reference to it will be in any unrestored ConnectionTokens
         */
        if(connection.closed()) {
            logger.debug("[{}: CLOSED]: Connection closed - Stopping connection tracking", port(connection.connection()));

            allCreatedConnections.remove(connection.connection());  // connection.connection() returns a SimpleConnection
            knownConnections.remove();  // this will remove the connection from allKnownConnections directly, or via Iterator

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
     * Takes a Set, a boolean, and a connectionHolder
     * If the boolean is true, it will add the connectionHolder to the Set, otherwise, it will remove it from the Set
     *
     * NOTE: Thread Safety
     *     This method is private, and is only called either directly or transitively by synchronized
     *     methods in this class.
     *
     * @param set the set to potentially add or remove the connectionHolder from
     * @param isMember if true, it will add connectionHolder to set, otherwise, it will remove connectionHolder from set
     * @param connectionHolder the connectionHolder to add or remove from the set
     */
    private void updateSet(Set<SimpleConnectionHolder> set, boolean isMember, SimpleConnectionHolder connectionHolder) {
        if(isMember)
            set.add(connectionHolder);
        else
            set.remove(connectionHolder);
    }

    /**
     * Remove leaked connections
     *
     * Remove any connections that were created by the SimpleConnectionMaker, but were not returned.
     * This can occur if a connection-creation callback thread finishes creating a connection after a timeout has
     * occurred.
     *
     * If this happens, then the created-connection will not be tracked by the connection pool, and therefore
     * never closed, causing a connection leak.
     *
     * NOTE: Connection closing methods
     *     This method and readConnectionHolder() are the only two methods that close connections.
     *     This can be helpful to know for debugging
     *
     */
    private void findAndCloseLeakedConnections()
    {
        // remove all connections that the connection pool is tracking, from the set of all created connections
        for(SimpleConnectionHolder knownConnection: allKnownConnections)
            allCreatedConnections.remove(knownConnection.connection());

        // any remaining connections are leaks, and can now be safely closed
        if(allCreatedConnections.size() > 0) {
            logger.debug("{} untracked connection found", allCreatedConnections.size());

            Iterator<SimpleConnection> leakedConnections = allCreatedConnections.iterator();
            while(leakedConnections.hasNext()) {
                SimpleConnection leakedConnection = leakedConnections.next();

                leakedConnection.safeClose();
                leakedConnections.remove();

                logger.debug("Connection closed {} -> {}", port(leakedConnection), uri.toString());
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
     *     notBorrowedExpired, or allKnownConnections sets
     *
     */
    private String showConnections(String transitionName) {
        return "After " + transitionName + " - CONNECTIONS: " +
                showConnections("BORROWABLE", borrowable) +
                showConnections("BORROWED", borrowed) +
                showConnections("NOT_BORROWED_EXPIRED", notBorrowedExpired) +
                showConnections("KNOWN_CONNECTIONS", allKnownConnections);
    }

    /***
     * For logging
     *
     * NOTE: Thread Safety
     *     This method is private, and is only called either directly or transitively by synchronized
     *     methods in this class.
     *
     */
    private static String showConnections(String name, Set<SimpleConnectionHolder> set) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(name).append(": ");
        for(SimpleConnectionHolder holder: set)
            sb.append(port(holder.connection())).append(" ");
        sb.append("] ");
        return sb.toString();
    }

    /***
     * For logging
     *
     * NOTE: Thread Safety
     *     This method is private, and is only called either directly or transitively by synchronized
     *     methods in this class.
     *
     */
    private static String port(SimpleConnection connection) {
        if(connection == null) return "NULL";
        String url = connection.getLocalAddress();
        int semiColon = url.lastIndexOf(":");
        if(semiColon == - 1) return "PORT?";
        return url.substring(url.lastIndexOf(":")+1);
    }
}
