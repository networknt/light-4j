package com.networknt.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.networknt.service.SingletonServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public interface CacheManager {
    Logger logger = LoggerFactory.getLogger(CacheManager.class);
    static CacheManager getInstance() {
        List<CacheItem> caches = CacheConfig.load().getCaches();
        if(caches != null && caches.size() > 0) {
            CacheManager cacheManager = SingletonServiceFactory.getBean(CacheManager.class);
            if (cacheManager == null) {
                logger.warn("No cache manager configured in service.yml; default to CaffeineCacheManager");
                cacheManager = new CaffeineCacheManager();
                for(CacheItem cacheItem: CacheConfig.load().getCaches()) {
                    cacheManager.addCache(cacheItem.getCacheName(), cacheItem.getMaxSize(), cacheItem.getExpiryInMinutes());
                }
                SingletonServiceFactory.setBean(CacheManager.class.getName(), cacheManager);
            }
            return cacheManager;
        } else {
            return null;
        }
    }

    void addCache(String cacheName, long maxSize, long expiryInMinutes);
    Cache<Object, Object> getCache(String cacheName);
    void removeCache(String cacheName);
}
