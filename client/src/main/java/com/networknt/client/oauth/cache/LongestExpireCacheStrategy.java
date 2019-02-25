package com.networknt.client.oauth.cache;

import com.networknt.client.oauth.Jwt;

import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

public class LongestExpireCacheStrategy implements ICacheStrategy {
    private final PriorityQueue<LongestExpireCacheKey> expiryQueue;
    private final HashMap<Jwt.Key, Jwt> cachedJwts;
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
        expiryQueue = new PriorityQueue(capacity, comparator);
        cachedJwts = new HashMap<>();
    }

    @Override
    public synchronized void cacheJwt(Jwt.Key cachedKey, Jwt jwt) {
        //update the expire time
        LongestExpireCacheKey leCachKey = new LongestExpireCacheKey(cachedKey);
        leCachKey.setExpiry(jwt.getExpire());
        if(cachedJwts.size() >= capacity) {
            if(expiryQueue.contains(leCachKey)) {
                expiryQueue.remove(leCachKey);
            } else {
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
    }
}
