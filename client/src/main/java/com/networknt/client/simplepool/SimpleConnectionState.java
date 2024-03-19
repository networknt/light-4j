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
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import com.networknt.client.simplepool.exceptions.*;

/***
 * A SimpleConnectionState is a simplified interface for a connection, that also keeps track of the connection's state.
 * (In fact--in this document--a connection and its state are used interchangeably)
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
 *   [ NOT_BORROWED_VALID ] --(borrow)--&gt;   [ BORROWED_VALID ]
 *             |            &lt;-(restore)--           |
 *             |                                    |
 *          (expire)                             (expire)
 *             |                                    |
 *            \/                                   \/
 *   [ NOT_BORROWED_EXPIRED ] &lt;-(restore)-- [ BORROWED_EXPIRED ]
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
 *   This is important in methods that calculate a state based on the results of multiple reads from one or more
 *   time-dependent variables over the course of executing the method. For consistency of the calculation, its important
 *   that the values of time dependent variables do not change over the course of the execution of the method.
 *
 *   Not doing so (i.e.: not freezing the time) may allow inconsistent states to be reached.
 */
public final class SimpleConnectionState {
    private static final Logger logger = LoggerFactory.getLogger(SimpleConnectionState.class);

    // how long in milliseconds a connection can be eligible to be borrowed
    private final long EXPIRE_TIME;

    // the maximum number of borrowed tokens a connection can have at a time
    private final int MAX_BORROWS;

    // the time this connection was created represented as Unix Epoch time in milliseconds
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
     If the connection is HTTP/1.1, it can only be borrowed by 1 thread at a time
     If the connection is HTTP/2, it can be borrowed by an unlimited number of threads at a time
     */
    private final SimpleConnection connection;

    /** a Set containing all borrowed connection tokens */
    private final Set<ConnectionToken> borrowedTokens = ConcurrentHashMap.newKeySet();

    /***
     * Connections and ConnectionStates are paired 1-1. For every connection there is a single ConnectionState and
     * vice versa.
     *
     * This is why connections are created at the same time a ConnectionState is created (see SimpleConnectionState
     * constructor).
     *
     * The SimpleConnectionState acts as a simplified interface to the connection, and keeps track of how many
     * threads are using it at any given time. The maximum number of threads that can use it at the same time
     * is determined by the connection's type: HTTP/1.1 (1 thread at a time) or HTTP/2 (multiple threads at a time).
     *
     * @param expireTime the length of time in milliseconds a connection is eligible to be borrowed
     * @param connectionCreateTimeout The maximum time in seconds to wait for a new connection to be established before
     *          throwing an exception
     * @param isHttp2 if true, tries to upgrade to HTTP/2. if false, will try to open an HTTP/1.1 connection
     * @param uri the URI the connection will try to connect to
     * @param allCreatedConnections this Set will be passed to the callback thread that creates the connection.
     *          The connectionMaker will always add every successfully created connection to this Set. This Set must be
     *          threadsafe (such as <code>ConcurrentHashMap.newKeySet()</code>)
     * @param connectionMaker a class that SimpleConnectionState uses to create new SimpleConnection objects
     * @throws RuntimeException thrown if connection cannot be established within <code>connectionCreateTimeout</code>
     *          seconds or prematurely closes
     */
    public SimpleConnectionState(
            long expireTime,
            long connectionCreateTimeout,
            boolean isHttp2,
            URI uri,
            Set<SimpleConnection> allCreatedConnections,
            SimpleConnectionMaker connectionMaker) throws RuntimeException
    {
        this.uri = uri;
        EXPIRE_TIME = expireTime;

        // for logging
        long now = System.currentTimeMillis();

        // create initial connection to uri
        connection = connectionMaker.makeConnection(connectionCreateTimeout, isHttp2, uri, allCreatedConnections);

        // throw exception if connection creation failed
        if(!connection.isOpen()) {
            if(logger.isDebugEnabled()) logger.debug("{} closed connection", logLabel(connection, now));
            throw new SimplePoolConnectionFailureException("[" + port(connection) + "] Error creating connection to " + uri.toString());

            // start life-timer and determine connection type
        } else {
            startTime = System.currentTimeMillis();

            // HTTP/1.1 connections have a MAX_BORROW of 1, while HTTP/2 connections can have > 1 MAX_BORROWS
            MAX_BORROWS = connection().isMultiplexingSupported() ? Integer.MAX_VALUE : 1;

            if(logger.isDebugEnabled())
                logger.debug("{} New connection : {}", logLabel(connection, now), MAX_BORROWS > 1 ? "HTTP/2" : "HTTP/1.1");
        }
    }

    /**
     * State Transition - Borrow
     *
     * @param now the Unix Epoch time in milliseconds at which to evaluate whether there are borrowable connections or not
     * @return returns a ConnectionToken representing the borrowing of the connection
     * @throws RuntimeException      if the connection is closed
     * @throws IllegalStateException if the connection is not borrowable
     */
    public synchronized ConnectionToken borrow(long now) throws RuntimeException, IllegalStateException {
        /***
         * Connections can only be borrowed when the connection is in a BORROWABLE state.
         *
         * This will throw an IllegalStateException if borrow is called when the connection is not borrowable.
         * This means that users need to check the state of the connection (i.e.: the state of the ConnectionState)
         * before using it, e.g.:
         *
         *     ConnectionToken connectionToken = null;
         *     long now = System.currentTimeMillis();
         *
         *     if(connectionState.borrowable(now))
         *         connectionToken = connectionState.borrow(now);
         *
         * Also note the use of a single consistent value for the current time ('now'). This ensures
         * that the state returned in the 'if' statement will still be true in the 'borrow' statement
         * (as long as the connection does not close between the 'if' and 'borrow').
         *
         */

        ConnectionToken connectionToken;

        if(borrowable(now)) {
            if(closed())
                throw new SimplePoolConnectionClosureException("Connection was unexpectedly closed");

            // add connectionToken to the Set of borrowed tokens
            borrowedTokens.add( (connectionToken = new ConnectionToken(connection)) );

            if(logger.isDebugEnabled())
                logger.debug("{} borrow - connection now has {} borrows", logLabel(connection, now), borrowedTokens.size());

            return connectionToken;
        }
        else {
            if(closed())
                throw new SimplePoolConnectionClosureException("Connection was unexpectedly closed");
            else
                throw new IllegalStateException("Attempt made to borrow connection outside of BORROWABLE state");
        }
    }

    /**
     * State Transition - Restore
     *
     * NOTE: A connection that unexpectedly closes may be removed from connection pool tracking before all of its
     *       ConnectionTokens have been restored.
     *
     * @param connectionToken
     */
    public synchronized void restore(ConnectionToken connectionToken) {
        borrowedTokens.remove(connectionToken);

        long now = System.currentTimeMillis();
        if(logger.isDebugEnabled())
            logger.debug("{} restore - connection now has {} borrows", logLabel(connection, now), borrowedTokens.size());
    }

    /**
     * State Transition - Close
     *
     * @param now the Unix Epoch time in milliseconds at which to evaluate whether this connection is closable or not
     * @return true if the connection was closed and false otherwise
     */
    public synchronized boolean safeClose(long now) {
        if(logger.isDebugEnabled())
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
         This is vital to ensure that connections are never closed until after all threads that
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
     * @return true if the connection is closed and false otherwise
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
     * @param now the Unix Epoch time in milliseconds at which to evaluate whether this connection has expired or not
     * @return true if the connection has expired and false otherwise
     */
    public synchronized boolean expired(long now) {
        return now - startTime >= EXPIRE_TIME;
    }

    /**
     * State Property - isBorrowed
     *
     * @return true if the connection is currently borrowed and false otherwise
     */
    public synchronized boolean borrowed() {
        return borrowedTokens.size() > 0;
    }

    /**
     * State Property - isAtMaxBorrows
     *
     * @return true if the connection is at its maximum number of borrows, and false otherwise
     */
    public synchronized boolean maxBorrowed() {
        return borrowedTokens.size() >= MAX_BORROWS;
    }

    /**
     * State Property - isBorrowable
     *
     * @param now the Unix Epoch time in milliseconds at which to evaluate the borrowability of this connection
     * @return true if the connection is borrowable and false otherwise
     */
    public synchronized boolean borrowable(long now) {
        return connection.isOpen() && !expired(now) && !maxBorrowed();
    }

    /**
     * Returns the SimpleConnection that SimpleConnectionState holds
     *
     * @return the SimpleConnection that SimpleConnectionState holds
     */
    public SimpleConnection connection() { return connection; }

    public class ConnectionToken {
        private final SimpleConnection connection;
        private final SimpleConnectionState connectionState;
        private final URI uri;

        ConnectionToken(SimpleConnection connection) {
            this.connection = connection;
            this.connectionState = SimpleConnectionState.this;
            this.uri = SimpleConnectionState.this.uri;
        }

        SimpleConnectionState state() { return connectionState; }
        SimpleConnection connection() { return connection; }
        public Object getRawConnection() { return connection.getRawConnection(); }
        public URI uri() { return uri; }
    }

    /***
     * For logging
     *
     * NOTE: Thread Safety
     *     This method is private, and is only called either directly or transitively by synchronized
     *     methods in this class.
     */
    private String logLabel(SimpleConnection connection, long now) {
        return "[" + port(connection) + ": " + state(now) + "]";
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

    /***
     * For logging
     *
     * NOTE: Thread Safety
     *     This method is private, and is only called either directly or transitively by synchronized
     *     methods in this class.
     */
    private enum State { CLOSED, BORROWABLE, NOT_BORROWABLE, NOT_BORROWED, VALID, BORROWED, EXPIRED }
    private String state(long now) {
        List<State> stateList = new ArrayList<>();
        if(closed())        { stateList.add(State.CLOSED); }
        if(borrowed())      { stateList.add(State.BORROWED);} else{ stateList.add(State.NOT_BORROWED);}
        if(borrowable(now)) { stateList.add(State.BORROWABLE);} else{ if(!expired(now)) { stateList.add(State.NOT_BORROWABLE);}}
        if(expired(now))    { stateList.add(State.EXPIRED);} /* else{ states.add(State.VALID);} */

        StringBuilder state = new StringBuilder();
        for(int i = 0; i < stateList.size(); ++i) {
            state.append(stateList.get(i));
            if(i+1 < stateList.size()) state.append(" ");
        }
        return stateList.size() > 0 ? state.toString() : "ILLEGAL_STATE";
    }
}
