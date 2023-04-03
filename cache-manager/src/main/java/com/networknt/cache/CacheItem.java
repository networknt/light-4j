package com.networknt.cache;

public class CacheItem {
    String cacheName;
    int expiryInMinutes;
    int maxSize;

    public CacheItem() {
    }

    public CacheItem(String cacheName, int expiryInMinutes, int maxSize) {
        this.cacheName = cacheName;
        this.expiryInMinutes = expiryInMinutes;
        this.maxSize = maxSize;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public int getExpiryInMinutes() {
        return expiryInMinutes;
    }

    public void setExpiryInMinutes(int expiryInMinutes) {
        this.expiryInMinutes = expiryInMinutes;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }
}
