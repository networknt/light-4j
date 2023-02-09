package com.networknt.client.http;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


/***
    A connection pool for a single URI.
    Connection pool contains 4 Sets of ConnectionHolders

    1. All holders -- the complete set of holders
    2. Borrowable holders -- holders that can be borrowed from
    3. Borrowed holders -- holders that have borrowed tokens
    4. Not Borrowed Expired holders -- expired holders that have no borrowed tokens -- these can be closed
*/
public class SimpleURIConnectionPool {
    private long EXPIRY_TIME;
    private int poolSize;
    private URI uri;
    private Set<SimpleConnectionHolder> all = new HashSet<>();
    private Set<SimpleConnectionHolder> borrowable = new HashSet<>();
    private Set<SimpleConnectionHolder> borrowed = new HashSet<>();
    private Set<SimpleConnectionHolder> notBorrowedExpired = new HashSet<>();

    private SimpleURIConnectionPool() {}

    public SimpleURIConnectionPool(URI uri, long expireTime, int poolSize) {
        EXPIRY_TIME = expireTime;
        this.uri = uri;
        this.poolSize = poolSize;
    }

    public synchronized SimpleConnectionHolder.ConnectionToken borrow(long timeout, long createConnectionTimeout, boolean isHttp2) {
        SimpleConnectionHolder holder = null;
        readHolders();

        if(borrowable.size() > 0) {
            holder = borrowable.toArray(new SimpleConnectionHolder[0])[ThreadLocalRandom.current().nextInt(borrowable.size())];
        }
        else if(borrowed.size() < poolSize)
            holder = new SimpleConnectionHolder(timeout, createConnectionTimeout, isHttp2, uri);

        SimpleConnectionHolder.ConnectionToken token = holder.borrow(timeout);
        readHolder(holder);
        return token;
    }

    public synchronized void restore(SimpleConnectionHolder.ConnectionToken token) {
        readHolders();
        token.holder().restore(token);
        readHolder(token.holder());
    }

    private void readHolders() {
        for(SimpleConnectionHolder holder: all) readHolder(holder);

        for(SimpleConnectionHolder holder: notBorrowedExpired) {
            holder.close();
            notBorrowedExpired.remove(holder);
            all.remove(holder);
        }
    }

    private void readHolder(SimpleConnectionHolder holder) {
        updateSet(borrowable, holder.borrowable(), holder);
        updateSet(borrowed, holder.borrowed(), holder);
        updateSet(notBorrowedExpired, holder.closeable(), holder);
    }

    private void updateSet(Set<SimpleConnectionHolder> set, boolean isMember, SimpleConnectionHolder holder) {
        if(isMember && !set.contains(holder))
            set.add(holder);
        else if(!isMember && set.contains(holder))
            set.remove(holder);
    }
}
