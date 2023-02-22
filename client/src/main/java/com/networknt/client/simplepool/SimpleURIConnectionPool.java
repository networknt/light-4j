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
    private final Set<SimpleConnection> allCreatedConnections = ConcurrentHashMap.newKeySet();
    private final URI uri;
    private final Set<SimpleConnectionHolder> allKnownConnections = new HashSet<>();
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
         * Move connections to appropriate sets based on their properties
         * Also, remove any connections that have unexpectedly closed
         *
         * Note that we iterate using a copy of allKnownConnections (see below), since readConnectionHolder() may
         * remove connections from allKnownConnections using Set.remove(). If we iterated using allKnownConnections
         * it could cause a ConcurrentModificationException.
         */

        for(SimpleConnectionHolder connection: new HashSet<>(allKnownConnections))
            // move connection to correct set or remove it if it unexpectedly closed
            readConnectionHolder(connection, now);

        // close any open connections in a closeable set
        HashSet<SimpleConnectionHolder> closedConnections = new HashSet<>();
        for(SimpleConnectionHolder closeableConnection: notBorrowedExpired)
        {
            closeableConnection.safeClose(now);
            closedConnections.add(closeableConnection);     // record that this connection has been closed
        }
        notBorrowedExpired.removeAll(closedConnections);
        allKnownConnections.removeAll(closedConnections);


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

        // remove all connections that the connection pool is tracking from the set of all created connections
        allCreatedConnections.removeAll(knownConnections);

        // any remaining connections are leaks, and can now be safely closed
        if(allCreatedConnections.size() > 0) {
            logger.debug("{} leaked connection found", allCreatedConnections.size());

            Iterator<SimpleConnection> closedLeakedCons = allCreatedConnections.iterator();
            while(closedLeakedCons.hasNext()) {
                closedLeakedCons.next().safeClose();
                closedLeakedCons.remove();
            }
        }
    }

    /***
     * This method reads a connection and updates the state of the SimpleURIConnectionPool based on the state of connection.
     * It will also remove a connection from all sets (i.e.: stop tracking the connection) if it unexpectedly closed.
     *
     * NOTE: Thread Safety
     *     This method is private, and is only called either directly or transitively by synchronized
     *     methods in this class.
     *
     * @param connection
     * @param now
     */
    private void readConnectionHolder(SimpleConnectionHolder connection, long now) {

        if(connection.closed())
        {
            allKnownConnections.remove(connection);
            borrowable.remove(connection);
            borrowed.remove(connection);
            notBorrowedExpired.remove(connection);

            return;
        }

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
