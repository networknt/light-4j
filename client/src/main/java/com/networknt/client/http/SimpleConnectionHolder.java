package com.networknt.client.http;

import com.networknt.utility.ConcurrentHashSet;
import io.undertow.client.ClientConnection;
import org.xnio.IoUtils;
import java.net.URI;
import java.util.Set;

/*
  - keeps track of whether this connection
    - is borrowed
    - is borrowable
    - has expired
    - is closeable
    - has closed
*/

/*
        expired() = System.currentTimeMillis() - startTime > EXPIRE_TIME

        borrowed() = { isBorrowed, isAtMax }
                   = { T         , T         -> valid -> borrowedMax
                       T         , F         -> valid -> borrowedBelowMax
                       F         , T         -> invalid
                       F         , F         -> valid -> notBorrowed
                     }

        CLOSED,
        NOT_BORROWED_VALID
        BORROWABLE_VALID
        MAX_BORROWED_VALID
        NOT_BORROWED_EXPIRED
        BORROWABLE_EXPIRED
        MAX_BORROWED_EXPIRED

        INUSE_MAXBORROWED_UNEXPIRED,
        NOTINUSE_NOTMAXBORROWED_UNEXPIRED,
        INUSE_MAXBORROWED_EXPIRED,
        NOTINUSE_MAXBORROWED_EXPIRED,
        NOTINUSE_MAXBORROWED_UNEXPIRED,
        INUSE_NOTMAXBORROWED_EXPIRED,
        NOTINUSE_NOTMAXBORROWED_EXPIRED


    @Override
    public synchronized boolean inUse() {
        return borrowedTokens.size() > 0;
    }
    @Override
    public synchronized boolean maxBorrowed() {
        return borrowedTokens.size() >= MAX_BORROWS;
    }
*/

public class SimpleConnectionHolder {
    enum HolderState {
        CLOSED,
        NOT_BORROWED_VALID,
        NOT_BORROWED_EXPIRED,
        BORROWED_VALID,
        BORROWED_EXPIRED,
        BORROWABLE
    }

    private long EXPIRE_TIME;
    private ClientConnection connection;
    private Set<ConnectionToken> borrowedTokens = new ConcurrentHashSet<>();
    private long startTime;
    private URI uri;
    private int MAX_BORROWS;
    private boolean closed = false;

    private SimpleConnectionHolder() {}

    public SimpleConnectionHolder(long expireTime, long createConnectionTimeout, boolean isHttp2, URI uri) {
        this.uri = uri;
        EXPIRE_TIME = expireTime;

        // create initial connection to uri
        connection = SimpleConnectionMaker.makeConnection(createConnectionTimeout, isHttp2, uri);
        if(connection == null || !connection.isOpen())
            throw new RuntimeException("Error creating connection to " + uri.toString());
        else {
            startTime = System.currentTimeMillis();
            MAX_BORROWS = connection().isMultiplexingSupported() ? Integer.MAX_VALUE : 1;
        }
    }

    public ClientConnection connection() { return connection; }

    // state transition
    private boolean firstUse = true;
    public synchronized ConnectionToken borrow(long createConnectionTimeout) throws RuntimeException {
        ConnectionToken token;
        switch(state()) {
            case BORROWABLE:
                if(firstUse) {
                    firstUse = false;
                    token = new ConnectionToken(connection);
                } else {
                    token = new ConnectionToken(SimpleConnectionMaker.reuseConnection(createConnectionTimeout, connection));
                }
                borrowedTokens.add(token);
                return token;

            default:
                throw new IllegalStateException();
        }
    }

    // state transition
    public synchronized void restore(ConnectionToken token) {
        if(borrowedTokens.contains(token))
            borrowedTokens.remove(token);
    }

    // state transition
    public synchronized boolean close() {
        if(state() != HolderState.NOT_BORROWED_EXPIRED)
            throw new IllegalStateException();

        closed = true;
        if(connection != null && connection.isOpen())
            IoUtils.safeClose(connection);
        return closed;
    }

    public synchronized boolean closed() {
        return closed;
    }

    public synchronized boolean closeable() {
        return state() == HolderState.NOT_BORROWED_EXPIRED;
    }

    public synchronized boolean expired() {
        return System.currentTimeMillis() - startTime >= EXPIRE_TIME;
    }

    public synchronized boolean borrowed() {
        return borrowedTokens.size() > 0;
    }

    private synchronized boolean maxBorrowed() {
        return borrowedTokens.size() >= MAX_BORROWS;
    }

    public synchronized boolean borrowable() { return !expired() && !maxBorrowed(); }

    public class ConnectionToken {
        private ClientConnection connection;
        private SimpleConnectionHolder holder;
        private URI uri;

        private ConnectionToken() {}

        public ConnectionToken(ClientConnection connection) {
            this.connection = connection;
            this.holder = SimpleConnectionHolder.this;
            this.uri = SimpleConnectionHolder.this.uri;
        }

        public SimpleConnectionHolder holder() { return holder; }
        public ClientConnection connection() { return connection; }
        public URI uri() { return uri; }
    }

    private HolderState state() throws IllegalStateException {
        boolean expired = expired();
        boolean valid = !expired;
        boolean borrowed = borrowed();
        boolean maxBorrowed = maxBorrowed();
        boolean borrowable = valid && !maxBorrowed;

        if(closed)                  return HolderState.CLOSED;
        if(borrowable)              return HolderState.BORROWABLE;
        if(!borrowed && valid)      return HolderState.NOT_BORROWED_VALID;
        if(borrowed && valid)       return HolderState.BORROWED_VALID;
        if(borrowed && expired)     return HolderState.BORROWED_EXPIRED;
        if(!borrowed && expired)    return HolderState.NOT_BORROWED_EXPIRED;

        throw new IllegalStateException();
    }
}

