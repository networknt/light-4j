package com.networknt.client.simplepool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/***
 * A SimpleConnectionHolder is a simplified interface for a connection, that also keeps track of the connection's state.
 * (In fact--in this document--the state of a connection and the state of its holder are used interchangeably)
 *
 * Connection States
 *
 *   NOT_BORROWED_VALID     - not borrowed and valid (i.e.: borrowed and not expired)
 *   BORROWED_VALID         - borrowed and valid (i.e.: borrowed not expired)
 *   NOT_BORROWED_EXPIRED   - not borrowed and expired
 *   BORROWED_EXPIRED       - borrowed and expired
 *   CLOSED                 - closed
 *
 *   BORROWABLE
 *   A connection is BORROWABLE if it is VALID (i.e.: it is not EXPIRED), and its borrows are below its MAX_BORROW
 *
 * State diagram for a connection
 *
 *             |
 *            \/
 *   [ NOT_BORROWED_VALID ] --(borrow)-->   [ BORROWED_VALID ]
 *             |            <-(restore)--           |
 *             |                                    |
 *          (expire)                             (expire)
 *             |                                    |
 *            \/                                   \/
 *   [ NOT_BORROWED_EXPIRED ] <-(restore)-- [ BORROWED_EXPIRED ]
 *            |
 *         (close) (*)
 *           |
 *          \/
 *       [ CLOSED ]
 *
 * (*) A connection can be closed explicitly by the connection pool, or it can be closed at any time by the OS
 *     If it is closed unexpectedly by the OS, then the state can jump directly to CLOSED regardless of what state
 *     it is currently in
 *
 * Connection Tokens
 *   Tokens are a mechanism used to track whether or not any threads are still using a connection object. When
 *   users need to borrow a connection, they are given a connection token. This token contains a reference to the
 *   connection as well as other metadata.
 *
 *   When users are done with the connection, they must return the connection token to the connection pool.
 *
 *   The correct use requires some discipline on the part of the connection pool user. Connection leaks can
 *   occur if a borrowed token is not returned.
 *
 *   TODO: add a setting that sets the max time after expiry to give connections that still have unrestored tokens
 *
 * Time-freezing
 *   Calculates the state of the connection based on its internal properties at a specific point in time.
 *
 *   Users must provide a fixed 'now' value for the current time.
 *   This freezes a single time value for all time-dependent properties.
 *   This is important when calculating an aggregate state based on the values of 2 or more time-dependent states.
 *
 *   Not doing so (i.e.: not freezing the time) may allow inconsistent states to be reached.
 */
public final class SimpleConnectionHolder {
    private static final Logger logger = LoggerFactory.getLogger(SimpleConnectionHolder.class);

    // how long a connection can be eligible to be borrowed
    private final long EXPIRE_TIME;

    // the maximum number of borrowed tokens a connection can have at a time
    private final int MAX_BORROWS;

    // the time this connection was created
    private final long startTime;

    // the URI this connection is connected to
    private final URI uri;

    /**
      if true, this connection should be treated as CLOSED
      note: CLOSED may be true before a connection is actually closed since there may be a delay
            between setting close = false, and the network connection actually being fully closed
    */
    private volatile boolean closed = false;

    /**
      If the connection is HTTP/1.1, it can only be borrowed by 1 process at a time
      If the connection is HTTP/2, it can be borrowed by an unlimited number of processes at a time
    */
    private final SimpleConnectionMaker connectionMaker;
    private final SimpleConnection connection;

    /** a Set containing all borrowed connection tokens */
    private final Set<ConnectionToken> borrowedTokens = ConcurrentHashMap.newKeySet();

    /***
     * Connections and ConnectionHolders are paired 1-1. For every connection there is a single ConnectionHolder and
     * vice versa.
     *
     * This is why connections are created at the same time a ConnectionHolder is created (see SimpleConnectionHolder
     * constructor).
     *
     * The connection holder acts as a simplified interface to the connection, and keeps track of how many
     * processes are using it at any given time. The maximum number of processes using it at the same time
     * is determined by the connections type: HTTP/1.1 (1 process at a time) or HTTP/2 (multiple processes at a time).
     *
     * @param expireTime how long a connection is eligible to be borrowed
     * @param createConnectionTimeout how long it can take a connection be created before an exception thrown
     * @param isHttp2 if true, tries to upgrade to HTTP/2. if false, will try to open an HTTP/1.1 connection
     * @param uri the URI the connection will try to connect to
     * @param allCreatedConnections this Set will be passed to the callback thread that creates the connection.
     *                              The connectionMaker will always add every successfully created connection
     *                              to this Set.
     * @param connectionMaker a class that SimpleConnectionHolder uses to create new SimpleConnection objects
     */
    public SimpleConnectionHolder(
        long expireTime,
        long createConnectionTimeout,
        boolean isHttp2,
        URI uri,
        Set<SimpleConnection> allCreatedConnections,
        SimpleConnectionMaker connectionMaker)
    {
        this.connectionMaker = connectionMaker;

        this.uri = uri;
        EXPIRE_TIME = expireTime;

        // for logging
        long now = System.currentTimeMillis();

        // create initial connection to uri
        connection = connectionMaker.makeConnection(createConnectionTimeout, isHttp2, uri, allCreatedConnections);

        // throw exception if connection creation failed
        if(!connection.isOpen()) {
            logger.debug("{} null or non-open connection", logLabel(connection, now));
            throw new RuntimeException("[" + port(connection) + "] Error creating connection to " + uri.toString());

        // start life-timer and determine connection type
        } else {
            startTime = System.currentTimeMillis();

            // HTTP/1.1 connections have a MAX_BORROW of 1, while HTTP/2 connections can have > 1 MAX_BORROWS
            MAX_BORROWS = connection().isMultiplexingSupported() ? Integer.MAX_VALUE : 1;

            logger.debug("{} New connection : HTTP/2: {}", logLabel(connection, now), MAX_BORROWS > 1);
        }
    }

    /**
     * State Transition - Borrow
     *
     * @param createConnectionTimeout
     * @param now
     * @return
     * @throws RuntimeException
     */
    private volatile boolean firstUse = true;
    public synchronized ConnectionToken borrow(long createConnectionTimeout, long now) throws RuntimeException {
        /***
         * Connections can only be borrowed when the connection is in a BORROWABLE state.
         *
         * This will throw an IllegalStateException if borrow is called when the connection is not borrowable.
         * This means that users need to check the state of the connection (i.e.: the state of the ConnectionHolder)
         * before using it, e.g.:
         *
         *     ConnectionToken connectionToken = null;
         *     long now = System.currentTimeMillis();
         *
         *     if(connectionHolder.borrowable(now))
         *         connectionToken = connectionHolder.borrow(createConnectionTimeout, now);
         *
         * Also note the use of a single consistent value for the current time ('now'). This ensures
         * that the state returned in the 'if' statement will still be true in the 'borrow' statement
         * (as long as the connection does not close between the 'if' and 'borrow').
         *
         */
        ConnectionToken connectionToken;

        if(borrowable(now)) {
            if (firstUse) {
                firstUse = false;
                connectionToken = new ConnectionToken(connection);
            } else {
                SimpleConnection reusedConnection = connectionMaker.reuseConnection(createConnectionTimeout, connection);
                connectionToken = new ConnectionToken(reusedConnection);
            }

            // add connectionToken to the Set of borrowed tokens
            borrowedTokens.add(connectionToken);

            logger.debug("{} borrow - connection now has {} borrows", logLabel(connection, now), borrowedTokens.size());

            return connectionToken;
        }
        else {
            if(closed())
                throw new RuntimeException("Connection was unexpectedly closed");
            else
                throw new IllegalStateException("Attempt made to borrow connection outside of BORROWABLE state");
        }
    }

    /**
     * State Transition - Restore
     *
     * @param connectionToken
     */
    public synchronized void restore(ConnectionToken connectionToken) {
        borrowedTokens.remove(connectionToken);

        long now = System.currentTimeMillis();
        logger.debug("{} restore - connection now has {} borrows", logLabel(connection, now), borrowedTokens.size());
    }

    /**
     * State Transition - Close
     *
     * @param now
     * @return
     */
    // state transition
    public synchronized boolean safeClose(long now) {
        logger.debug("{} close - closing connection with {} borrows...", logLabel(connection, now), borrowedTokens.size());

        /**
        Connection may still be open even if closed == true
        However, for consistency, we treat the connection as closed as soon as closed == true,
        even if IoUtils.safeClose(connection) has not completed closing the connection yet
        */
        if(closed())
            return true;

        /**
        Ensures that a connection is never closed unless the connection is in the NOT_BORROWED_EXPIRED state
        This is vital to ensure that connections are never closed until after all processes that
        borrowed them are no longer using them
        */
        boolean notBorrowedExpired = !borrowed() && expired(now);
        if(notBorrowedExpired != true)
            throw new IllegalStateException();

        closed = true;
        connection.safeClose();
        return closed;
    }

    /**
     * State Property - isClosed
     *
     * @return
     */
    public synchronized boolean closed() {
        if(closed)
            return closed;

        if(!connection.isOpen())
            closed = true;

        return closed;
    }

    /**
     * State Property - isExpired
     *
     * @param now
     * @return
     */
    public synchronized boolean expired(long now) {
        return now - startTime >= EXPIRE_TIME;
    }

    /**
     * State Property - isBorrowed
     *
     * @return
     */
    public synchronized boolean borrowed() {
        return borrowedTokens.size() > 0;
    }

    /**
     * State Property - isAtMaxBorrows
     * 
     * @return
     */
    public synchronized boolean maxBorrowed() {
        return borrowedTokens.size() >= MAX_BORROWS;
    }

    /**
     * State Property - isBorrowable
     *
     * @param now
     * @return
     */
    public synchronized boolean borrowable(long now) {
        return connection.isOpen() && !expired(now) && !maxBorrowed();
    }

    /**
     *
     * @return
     */
    public SimpleConnection connection() { return connection; }

    public class ConnectionToken {
        private final SimpleConnection connection;
        private final SimpleConnectionHolder holder;
        private final URI uri;

        public ConnectionToken(SimpleConnection connection) {
            this.connection = connection;
            this.holder = SimpleConnectionHolder.this;
            this.uri = SimpleConnectionHolder.this.uri;
        }

        public SimpleConnectionHolder holder() { return holder; }
        public SimpleConnection connection() { return connection; }
        public Object getRawConnection() { return connection.getRawConnection(); }
        public URI uri() { return uri; }
    }

    /***
     * For logging
     *
     * WARNING: Thread Safety Note
     *     This method *must* remain private, and *must only* be called either directly or transitively by synchronized
     *     methods in this class.
     *
     *     Any changes to this will almost certainly result in multi-threading related FAILURES of this connection pool
     *
     */
    private String logLabel(SimpleConnection connection, long now) {
        return "[" + port(connection) + ": " + state(now) + "]:";
    }

    /***
     * For logging
     *
     * WARNING: Thread Safety Note
     *     This method *must* remain private, and *must only* be called either directly or transitively by synchronized
     *     methods in this class.
     *
     *     Any changes to this will almost certainly result in multi-threading related FAILURES of this connection pool
     *
     */
    private static String port(SimpleConnection connection) {
        if(connection == null) return "NULL";
        String url = connection.getLocalAddress();
        int semiColon = url.lastIndexOf(":");
        if(semiColon == - 1) return "PORT?";
        return url.substring(url.lastIndexOf(":")+1);
    }

    /***
     * For logging
     *
     * WARNING: Thread Safety Note
     *     This method *must* remain private, and *must only* be called either directly or transitively by synchronized
     *     methods in this class.
     *
     *     Any changes to this will almost certainly result in multi-threading related FAILURES of this connection pool
     *
     */
    private String state(long now) {
        if(closed())                        return "CLOSED";
        if(borrowable(now))                 return "BORROWABLE";
        if(!borrowed() && !expired(now))    return "NOT_BORROWED_VALID";
        if(borrowed() && !expired(now))     return "BORROWED_VALID";
        if(borrowed() && expired(now))      return "BORROWED_EXPIRED";
        if(!borrowed() && expired(now))     return "NOT_BORROWED_EXPIRED";

        // check that the connection did not close after the call to closed() above
        if(closed())
            return "CLOSED";

        return "ILLEGAL_STATE";
    }
}
