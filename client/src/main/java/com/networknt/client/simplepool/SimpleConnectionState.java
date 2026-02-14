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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/***
 * SimpleConnectionState is a simplified interface for a connection, that also keeps track of the connection's state.
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
 * Time-freezing
 *   Calculates the state of the connection based on its internal properties at a specific point in time.
 *
 *   Users must provide a fixed 'now' value for the current time (Unix Epoch time in milliseconds).
 *   This freezes a single time value for all time-dependent properties.
 *   This is important when calculating an aggregate state based on the values of 2 or more time-dependent states.
 */
public final class SimpleConnectionState {
    private static final Logger logger = LoggerFactory.getLogger(SimpleConnectionState.class);

    // how long a connection can be eligible to be borrowed (in milliseconds)
    private final long EXPIRE_TIME;

    // the maximum number of borrowed tokens a connection can have at a time
    private final int MAX_BORROWS;

    // the time this connection was created (Unix Epoch time in milliseconds)
    private final long startTime;

    // the URI this connection is connected to
    private final URI uri;

    /**
      if true, this connection should be treated as CLOSED
    */
    private volatile boolean closed = false;

    private final SimpleConnectionMaker connectionMaker;
    private final SimpleConnection connection;

    /** a Set containing all borrowed connection tokens */
    private final Set<ConnectionToken> borrowedTokens = ConcurrentHashMap.newKeySet();

    /***
     * Connections and SimpleConnectionStates are paired 1-1. For every connection there is a single SimpleConnectionState and
     * vice-versa.
     *
     * The SimpleConnectionState acts as a simplified interface to the connection, and keeps track of how many
     * processes are using it at any given time.
     *
     * @param expireTime how long a connection is eligible to be borrowed (in milliseconds)
     * @param connectionCreateTimeout how long it can take a connection be created (in milliseconds) before an exception thrown
     * @param uri the URI the connection will try to connect to
     * @param bindAddress the address the connection will bind to
     * @param worker the XnioWorker that will create the connection
     * @param bufferPool the buffer pool that will be used to create the connection
     * @param ssl the ssl context that will be used to create the connection
     * @param options the options that will be used to create the connection
     * @param allCreatedConnections this Set will be passed to the callback thread that creates the connection.
     * @param connectionMaker a class that SimpleConnectionState uses to create new SimpleConnection objects
     */
    public SimpleConnectionState(
            long expireTime,
            long connectionCreateTimeout,
            URI uri,
            InetSocketAddress bindAddress,
            XnioWorker worker,
            ByteBufferPool bufferPool,
            XnioSsl ssl,
            OptionMap options,
            Set<SimpleConnection> allCreatedConnections,
            SimpleConnectionMaker connectionMaker)
    {
        this.connectionMaker = connectionMaker;
        this.uri = uri;
        EXPIRE_TIME = expireTime;

        long now = System.currentTimeMillis();

        connection = connectionMaker.makeConnection(connectionCreateTimeout, bindAddress, uri, worker, ssl, bufferPool, options, allCreatedConnections);
        if(!connection.isOpen()) {
            logger.debug("{} closed connection", logLabel(connection, now));
            throw new RuntimeException("[" + port(connection) + "] Error creating connection to " + uri.toString());
        } else {
            startTime = System.currentTimeMillis();
            MAX_BORROWS = connection().isMultiplexingSupported() ? Integer.MAX_VALUE : 1;
            logger.debug("{} New connection : {}", logLabel(connection, now), MAX_BORROWS > 1 ? "HTTP/2" : "HTTP/1.1");
        }
    }

    public synchronized ConnectionToken borrow(long now) throws RuntimeException {
        if(borrowable(now)) {
            ConnectionToken connectionToken = new ConnectionToken(connection);
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

    public synchronized void restore(ConnectionToken connectionToken) {
        borrowedTokens.remove(connectionToken);
        long now = System.currentTimeMillis();
        logger.debug("{} restore - connection now has {} borrows", logLabel(connection, now), borrowedTokens.size());
    }

    public synchronized boolean safeClose(long now) {
        logger.debug("{} close - closing connection with {} borrows...", logLabel(connection, now), borrowedTokens.size());
        if(closed())
            return true;

        boolean notBorrowedExpired = !borrowed() && expired(now);
        if(!notBorrowedExpired)
            throw new IllegalStateException();

        closed = true;
        connection.safeClose();
        return closed;
    }

    public synchronized boolean closed() {
        if(closed)
            return closed;
        if(!connection.isOpen())
            closed = true;
        return closed;
    }

    public synchronized boolean expired(long now) {
        return now - startTime >= EXPIRE_TIME;
    }

    public synchronized boolean borrowed() {
        return borrowedTokens.size() > 0;
    }

    public synchronized boolean maxBorrowed() {
        return borrowedTokens.size() >= MAX_BORROWS;
    }

    public synchronized boolean borrowable(long now) {
        return connection.isOpen() && !expired(now) && !maxBorrowed();
    }

    public SimpleConnection connection() { return connection; }

    public class ConnectionToken {
        private final SimpleConnection connection;
        private final SimpleConnectionState holder;
        private final URI uri;

        ConnectionToken(SimpleConnection connection) {
            this.connection = connection;
            this.holder = SimpleConnectionState.this;
            this.uri = SimpleConnectionState.this.uri;
        }

        public SimpleConnectionState holder() { return holder; }
        public SimpleConnection connection() { return connection; }
        public Object getRawConnection() { return connection.getRawConnection(); }
        public URI uri() { return uri; }
    }

    private String logLabel(SimpleConnection connection, long now) {
        return "[" + port(connection) + ": " + state(now) + "]";
    }

    private static String port(SimpleConnection connection) {
        if(connection == null) return "NULL";
        String url = connection.getLocalAddress();
        int semiColon = url.lastIndexOf(":");
        if(semiColon == - 1) return "PORT?";
        return url.substring(url.lastIndexOf(":")+1);
    }

    private enum State { CLOSED, BORROWABLE, NOT_BORROWABLE, NOT_BORROWED, VALID, BORROWED, EXPIRED }
    private String state(long now) {
        List<State> stateList = new ArrayList<>();
        if(closed())        { stateList.add(State.CLOSED); }
        if(borrowed())      { stateList.add(State.BORROWED);} else{ stateList.add(State.NOT_BORROWED);}
        if(borrowable(now)) { stateList.add(State.BORROWABLE);} else{ if(!expired(now)) { stateList.add(State.NOT_BORROWABLE);}}
        if(expired(now))    { stateList.add(State.EXPIRED);}

        StringBuilder state = new StringBuilder();
        for(int i = 0; i < stateList.size(); ++i) {
            state.append(stateList.get(i));
            if(i+1 < stateList.size()) state.append(" ");
        }
        return stateList.size() > 0 ? state.toString() : "ILLEGAL_STATE";
    }
}
