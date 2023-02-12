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

    public SimpleConnectionHolder.ConnectionToken borrow(long createConnectionTimeout, boolean isHttp2, URI uri) {
        if(!pools.containsKey(uri)) {
            synchronized (pools) {
                if (!pools.containsKey(uri))
                    pools.put(uri, new SimpleURIConnectionPool(uri, expireTime, poolSize));
            }
        }
        return pools.get(uri).borrow(createConnectionTimeout, isHttp2);
    }

    public void restore(SimpleConnectionHolder.ConnectionToken connectionToken) {
        if(pools.containsKey(connectionToken.uri()))
            pools.get(connectionToken.uri()).restore(connectionToken);
    }
}
