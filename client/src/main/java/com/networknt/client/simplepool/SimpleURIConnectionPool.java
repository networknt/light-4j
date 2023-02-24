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
        SimpleConnectionHolder holder = null;

        readAllConnectionHolders(now);

        if(borrowable.size() > 0) {
            holder = borrowable.toArray(new SimpleConnectionHolder[0])[ThreadLocalRandom.current().nextInt(borrowable.size())];
        } else {
            if (borrowed.size() < poolSize) {
                holder = new SimpleConnectionHolder(EXPIRY_TIME, createConnectionTimeout, isHttp2, uri, allCreatedConnections, connectionMaker);
                allKnownConnections.add(holder);
            } else
                throw new RuntimeException("An attempt to exceed the connection pool's maximum size was made. Increase request.connectionPoolSize in client.yml");
        }

        SimpleConnectionHolder.ConnectionToken connectionToken = holder.borrow(createConnectionTimeout, now);
        readConnectionHolder(holder, now);

        logger.debug(showConnections("borrow"));

        return connectionToken;
    }

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
     * A key method that actually closes connections
     * It is guaranteed to run every time a transition method is called on SimpleURIConnectionPool
     *
     * NOTE: Thread Safety
     *     This method is private, and is only called either directly or transitively by synchronized
     *     methods in this class.
     *
     * @param now the current time in ms
     */
    private void readAllConnectionHolders(long now)
    {
        /**
         * Sweep all connections and update sets
         *
         * Remove any connections that have unexpectedly closed
         * Move all remaining connections to appropriate sets based on their properties
         *
         */

        Iterator<SimpleConnectionHolder> knownConnectionHolders = allKnownConnections.iterator();
        while(knownConnectionHolders.hasNext())
        {
            SimpleConnectionHolder connection = knownConnectionHolders.next();

            // remove connections that have unexpectedly closed
            if(connection.closed()) {
                knownConnectionHolders.remove();
                readConnectionHolder(connection, now);
                continue;
            }

            // move connections to correct sets
            readConnectionHolder(connection, now);

            // close and remove connections if they are in a closeable set
            if(notBorrowedExpired.contains(connection)) {
                connection.safeClose(now);
                readConnectionHolder(connection, now);
            }
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
         */

        // create a Set containing only the open SimpleConnections that the connection pool is aware of (i.e.: tracking)
        Set<SimpleConnection> knownConnections = new HashSet<>();
        for(SimpleConnectionHolder connectionHolder: allKnownConnections)
            knownConnections.add(connectionHolder.connection());

        // remove all connections that the connection pool is tracking, from the set of all created connections
        allCreatedConnections.removeAll(knownConnections);

        // any remaining connections are leaks, and can now be safely closed
        if(allCreatedConnections.size() > 0) {
            logger.debug("{} untracked connection found", allCreatedConnections.size());

            Iterator<SimpleConnection> closedLeakedCons = allCreatedConnections.iterator();
            while(closedLeakedCons.hasNext())
            {
                SimpleConnection connection = closedLeakedCons.next();
                connection.safeClose();
                closedLeakedCons.remove();
                logger.debug("Connection closed {} -> {}", port(connection), uri.toString());
            }
        }
    }

    /***
     * This method reads a connection and moves it to the correct sets based on its properties.
     * It will also remove a connection from all state sets (i.e.: stop tracking the connection) if it unexpectedly closed.
     *
     * NOTE: It does not remove it from the set of all connections (as that is not a state set)
     *
     * NOTE: Thread Safety
     *     This method is private, and is only called either directly or transitively by synchronized
     *     methods in this class.
     *
     * @param connection
     * @param now
     */
    private void readConnectionHolder(SimpleConnectionHolder connection, long now) {

        boolean isExpired =             connection.expired(now);
        boolean isBorrowed =            connection.borrowed();
        boolean isBorrowable =          connection.borrowable(now);
        boolean isNotBorrowedExpired =  !isBorrowed && isExpired;

        // check whether connection should be added or removed from these sets based on its current state
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
        if(isMember && !set.contains(connectionHolder))
            set.add(connectionHolder);
        else if(!isMember)
            set.remove(connectionHolder);
    }

    /***
     * For logging
     *
     * NOTE: Thread Safety
     *     This method is private, and is only called either directly or transitively by synchronized
     *     methods in this class.
     *
     */
    private String showConnections(String transitionName) {
        return "After " + transitionName + " - CONNECTIONS: " +
                showConnections("BORROWABLE", borrowable) +
                showConnections("BORROWED", borrowed);
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
        String url = connection.getLocalAddress().toString();
        int semiColon = url.lastIndexOf(":");
        if(semiColon == - 1) return "PORT?";
        return url.substring(url.lastIndexOf(":")+1);
    }
}
