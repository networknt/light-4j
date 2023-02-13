package com.networknt.client.simplepool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/***
    A connection pool for a single URI.
    Connection pool contains 4 Sets of ConnectionHolders:

        1. All:                     the complete set of holders
        2. Borrowable:              holders that can be borrowed from
        3. Borrowed:                holders that have borrowed tokens
        4. Not Borrowed Expired:    expired holders that have no borrowed tokens -- these can be closed
*/
public class SimpleURIConnectionPool {
    private static final Logger logger = LoggerFactory.getLogger(SimpleURIConnectionPool.class);
    private final SimpleConnectionMaker connectionMaker;
    private final long EXPIRY_TIME;
    private final int poolSize;
    private final URI uri;
    private final Set<SimpleConnectionHolder> all = new HashSet<>();
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
                holder = new SimpleConnectionHolder(EXPIRY_TIME, createConnectionTimeout, isHttp2, uri, connectionMaker);
                all.add(holder);
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
     * @param now the current time in ms
     */
    private void readAllConnectionHolders(long now) {
        // sweep all connections and update sets
        for(SimpleConnectionHolder connection: all)
            readConnectionHolder(connection, now);

        // close any connections found in a closeable set
        for(SimpleConnectionHolder closeableConnection: notBorrowedExpired)
        {
            closeableConnection.close(now);

            notBorrowedExpired.remove(closeableConnection);
            all.remove(closeableConnection);
        }
    }

    /***
     * This method reads a connection and updates the state of the SimpleURIConnectionPool based on the state of connection.
     *
     *
     * @param connection
     * @param now
     */
    private void readConnectionHolder(SimpleConnectionHolder connection, long now) {

        if(connection.closed())
        {
            all.remove(connection);
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
     * Takes a Set, a boolean, and a connection
     * If the boolean is true, it will add the connection to the Set, otherwise, it will remove it from the Set
     *
     * @param set the set to potentially add or remove the connection from
     * @param isMember if true, it will add connection to set, otherwise, it will remove connection from set
     * @param connection the connection to add or remove from the set
     */
    // TODO: Ensure this does not throw errors!
    private void updateSet(Set<SimpleConnectionHolder> set, boolean isMember, SimpleConnectionHolder connection) {
        if(isMember && !set.contains(connection))
            set.add(connection);
        else if(!isMember)
            set.remove(connection);
    }

    // for logging
    public String showConnections(String transitionName) {
        return "After " + transitionName + " - CONNECTIONS: " +
                showConnections("BORROWABLE", borrowable) +
                showConnections("BORROWED", borrowed);
    }

    public static String showConnections(String name, Set<SimpleConnectionHolder> set) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(name).append(": ");
        for(SimpleConnectionHolder holder: set)
            sb.append(port(holder.connection())).append(" ");
        sb.append("] ");
        return sb.toString();
    }
    public static String port(SimpleConnection connection) {
        if(connection == null) return "NULL";
        String url = connection.getLocalAddress().toString();
        int semiColon = url.lastIndexOf(":");
        if(semiColon == - 1) return "PORT?";
        return url.substring(url.lastIndexOf(":")+1);
    }
}
