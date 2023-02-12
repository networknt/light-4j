package com.networknt.client.http;

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
    private SimpleConnectionMaker connectionMaker;
    private long EXPIRY_TIME;
    private int poolSize;
    private URI uri;
    private final Set<SimpleConnectionHolder> all = new HashSet<>();
    private final Set<SimpleConnectionHolder> borrowable = new HashSet<>();
    private final Set<SimpleConnectionHolder> borrowed = new HashSet<>();
    private final Set<SimpleConnectionHolder> notBorrowedExpired = new HashSet<>();

    private SimpleURIConnectionPool() {}

    public SimpleURIConnectionPool(URI uri, long expireTime, int poolSize, SimpleConnectionMaker connectionMaker) {
        EXPIRY_TIME = expireTime;
        this.uri = uri;
        this.poolSize = poolSize;
        this.connectionMaker = connectionMaker;
    }

    public synchronized SimpleConnectionHolder.ConnectionToken borrow(long createConnectionTimeout, boolean isHttp2) throws RuntimeException {
        long now = System.currentTimeMillis();
        SimpleConnectionHolder holder = null;

        readAllHolders(now);

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
        readSingleHolder(holder, now);

        logger.debug(showConnections("borrow"));

        return connectionToken;
    }

    public synchronized void restore(SimpleConnectionHolder.ConnectionToken connectionToken) {
        if(connectionToken == null)
            return;

        //SimpleConnectionHolder holder = null;
        SimpleConnectionHolder holder = connectionToken.holder();
        long now = System.currentTimeMillis();

        holder.restore(connectionToken);
        readAllHolders(now);

        logger.debug(showConnections("restore"));
    }

    /**
     * A key method that actually closes connections
     * It is guaranteed to run every time a transition method is called on SimpleURIConnectionPool
     *
     * @param now the current time in ms
     */
    private void readAllHolders(long now) {
        // sweep all connections and update sets
        for(SimpleConnectionHolder holder: all)
            readSingleHolder(holder, now);

        // close any connections found in a closeable set
        for(SimpleConnectionHolder closeableConnection: notBorrowedExpired)
        {
            closeableConnection.close(now);

            notBorrowedExpired.remove(closeableConnection);
            all.remove(closeableConnection);
        }
    }

    /***
     * This method reads a holder and updates the state of the SimpleURIConnectionPool based on the state of connection.
     *
     *
     * @param holder
     * @param now
     */
    private void readSingleHolder(SimpleConnectionHolder holder, long now) {

        if(holder.closed())
        {
            all.remove(holder);
            borrowable.remove(holder);
            borrowed.remove(holder);
            notBorrowedExpired.remove(holder);

            return;
        }

        boolean isExpired =             holder.expired(now);
        boolean isBorrowed =            holder.borrowed();
        boolean isBorrowable =          holder.borrowable(now);
        boolean isNotBorrowedExpired =  !isBorrowed && isExpired;

        // check whether holder should be added or removed from these sets based on its current state
        updateSet(borrowable, isBorrowable, holder);
        updateSet(borrowed, isBorrowed, holder);
        updateSet(notBorrowedExpired, isNotBorrowedExpired, holder);
    }

    /***
     * Takes a Set, a boolean, and a holder
     * If the boolean is true, it will add the holder to the Set, otherwise, it will remove it from the Set
     *
     * @param set the set to potentially add or remove the holder from
     * @param isMember if true, it will add holder to set, otherwise, it will remove holder from set
     * @param holder
     */
    // TODO: Ensure this does not throw errors!
    private void updateSet(Set<SimpleConnectionHolder> set, boolean isMember, SimpleConnectionHolder holder) {
        if(isMember && !set.contains(holder))
            set.add(holder);
        else if(!isMember)
            set.remove(holder);
    }

    // for logging
    public String showConnections(String transitionName) {
        return "After " + transitionName + " - CONNECTIONS: " +
                showConnections("BORROWABLE", borrowable) +
                showConnections("BORROWED", borrowed) +
                showConnections("NOT_BORROWED_EXPIRED", notBorrowedExpired);
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
