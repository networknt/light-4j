package com.networknt.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CaffeineCacheManager implements CacheManager {
    private final Map<String, Cache<Object, Object>> caches = new ConcurrentHashMap<>();

    public CaffeineCacheManager() {
        if(logger.isInfoEnabled()) logger.info("CaffeineCacheManager is constructed.");
    }

    @Override
    public void addCache(String cacheName, long maximumSize, long expiryInMinutes) {
        Cache<Object, Object> cache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(expiryInMinutes, TimeUnit.MINUTES)
                .build();
        caches.put(cacheName, cache);
    }

    @Override
    public void put(String cacheName, String key, Object value) {
        Cache<Object, Object> cache = caches.get(cacheName);
        if (cache != null) {
            cache.put(key, value);
        }
    }

    @Override
    public Object get(String cacheName, String key) {
        Cache<Object, Object> cache = caches.get(cacheName);
        if (cache != null) {
            return cache.getIfPresent(key);
        }
        return null;
    }

    @Override
    public void removeCache(String cacheName) {
        Cache<Object, Object> cache = caches.get(cacheName);
        if (cache != null) {
            cache.invalidateAll();
            caches.remove(cacheName);
        }
    }

    @Override
    public int getSize(String cacheName) {
        Cache<Object, Object> cache = caches.get(cacheName);
        if (cache != null) {
            return (int) cache.estimatedSize();
        } else {
            return 0;
        }
    }


}
