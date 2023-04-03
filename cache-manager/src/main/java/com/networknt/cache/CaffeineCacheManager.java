package com.networknt.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CaffeineCacheManager implements CacheManager {
    private final Map<String, Cache<Object, Object>> caches = new ConcurrentHashMap<>();

    @Override
    public void addCache(String cacheName, long maximumSize, long expiryInMinutes) {
        Cache<Object, Object> cache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(expiryInMinutes, TimeUnit.MINUTES)
                .build();
        caches.put(cacheName, cache);
    }

    @Override
    public Cache<Object, Object> getCache(String cacheName) {
        return caches.get(cacheName);
    }

    @Override
    public void removeCache(String cacheName) {
        Cache<Object, Object> cache = caches.get(cacheName);
        if (cache != null) {
            cache.invalidateAll();
            caches.remove(cacheName);
        }
    }
}
