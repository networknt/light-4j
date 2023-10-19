package com.networknt.client.oauth.cache;

import com.networknt.client.oauth.Jwt;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * This class is to cache Jwts with "LongestExpire" strategy, which means:
 * When the cache pool meets the capacity and a new jwt with provided key needs to be cached:
 * the token with the longest expiry time will be the last to be replaced,
 * the token with the least expiry time will be the first to be replace.
 *
 */
public class LongestExpireCacheStrategy implements ICacheStrategy {
    /**
     * to store the priorities by expiry time.
     */
    private final PriorityBlockingQueue<LongestExpireCacheKey> expiryQueue;

    /**
     * to store the actual Jwt based on Jwt.Key.
     */
    private final ConcurrentHashMap<Jwt.Key, Jwt> cachedJwts;

    /**
     * the capacity of the cache pool.
     */
    private int capacity;

    public LongestExpireCacheStrategy(int capacity) {
        this.capacity = capacity;
        Comparator<LongestExpireCacheKey> comparator = (o1, o2) -> {
            if(o1.getExpiry() > o2.getExpiry()) {
                return 1;
            } else if(o1.getExpiry() == o2.getExpiry()){
                return 0;
            } else {
                return -1;
            }
        };
        expiryQueue = new PriorityBlockingQueue(capacity, comparator);
        cachedJwts = new ConcurrentHashMap<>();
    }

    /**
     * This method is to cache a jwt LongestExpireCacheStrategy based on a given Jwt.Key and a Jwt.
     * Every time it updates the expiry time of a jwt, and shift it up to a proper position.
     * Since the PriorityQueue is implemented by heap, the average O(n) should be O(log n).
     * @param cachedKey Jwt.Key the key to be used to cache
     * @param jwt Jwt the jwt to be cached
     */
    @Override
    public synchronized void cacheJwt(Jwt.Key cachedKey, Jwt jwt) {
        //update the expire time
        LongestExpireCacheKey leCachKey = new LongestExpireCacheKey(cachedKey);
        leCachKey.setExpiry(jwt.getExpire());
        if(cachedJwts.size() >= capacity) {
            if(expiryQueue.contains(leCachKey)) {
                expiryQueue.remove(leCachKey);
            } else {
                cachedJwts.remove(expiryQueue.peek().getCacheKey());
                expiryQueue.poll();
            }
        } else {
            if(expiryQueue.contains(leCachKey)) {
                expiryQueue.remove(leCachKey);
            }
        }
        expiryQueue.add(leCachKey);
        cachedJwts.put(cachedKey, jwt);
    }

    /**
     * get Jwt from cache pool based on provided Jwt.Key key
     * @param key Jwt.Key
     * @return Jwt jwt
     */
    @Override
    public Jwt getCachedJwt(Jwt.Key key) {
        return cachedJwts.get(key);
    }

    private static class LongestExpireCacheKey {
        private long expiry;
        private Jwt.Key cacheKey;

        @Override
        public int hashCode() {
            return cacheKey.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return cacheKey.equals(obj);
        }

        LongestExpireCacheKey(Jwt.Key key) {
            this.cacheKey = key;
        }

        long getExpiry() {
            return expiry;
        }

        void setExpiry(long expiry) {
            this.expiry = expiry;
        }

        Jwt.Key getCacheKey() {
            return cacheKey;
        }
    }
}
