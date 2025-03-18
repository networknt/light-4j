package com.networknt.cache;

import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.StringField;

public class CacheItem {

    @StringField(
            configFieldName = "cacheName",
            description = "The name of the cache."
    )
    String cacheName;

    @IntegerField(
            configFieldName = "expiryInMinutes",
            description = "The expiry time of the cache in minutes.",
            min = 0
    )
    int expiryInMinutes;

    @IntegerField(
            configFieldName = "maxSize",
            description = "The maximum size of the cache.",
            min = 0
    )
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
