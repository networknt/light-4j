package com.networknt.client.http;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a class through which multiple URI Connection Pools
 * can be accessed
 */
public class SimpleConnectionPool {
    private Map<URI, SimpleURIConnectionPool> pools = new ConcurrentHashMap<>();
    private long expireTime;
    private int poolSize;

    private SimpleConnectionPool() {}

    public SimpleConnectionPool(long expireTime, int poolSize) {
        this.expireTime = expireTime;
        this.poolSize = poolSize;
    }

    public synchronized SimpleConnectionHolder.ConnectionToken borrow(long timeout, long createConnectionTimeout, boolean isHttp2, URI uri) {
        if(!pools.containsKey(uri)) pools.put(uri, new SimpleURIConnectionPool(uri, expireTime, poolSize));
        return pools.get(uri).borrow(timeout, createConnectionTimeout, isHttp2);
    }

    public synchronized void restore(SimpleConnectionHolder.ConnectionToken token) {
        pools.get(token.uri()).restore(token);
    }
}
