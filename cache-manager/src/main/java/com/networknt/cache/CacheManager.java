package com.networknt.cache;

import com.networknt.service.SingletonServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * CacheManager is a singleton class that is used to manage all the caches in the system. The underline implementation
 * can be anything as long as it implements the CacheManager interface. The default implementation is CaffeineCacheManager
 * Please note that the CacheManager doesn't have any reference to the underlying cache implementation.
 *
 * @author Steve Hu
 */
public interface CacheManager {
    Logger logger = LoggerFactory.getLogger(CacheManager.class);
    static CacheManager getInstance() {
        List<CacheItem> caches = CacheConfig.load().getCaches();
        if(caches != null && !caches.isEmpty()) {
            CacheManager cacheManager = SingletonServiceFactory.getBean(CacheManager.class);
            if(cacheManager != null) {
                for(CacheItem cacheItem: caches) {
                    cacheManager.addCache(cacheItem.getCacheName(), cacheItem.getMaxSize(), cacheItem.getExpiryInMinutes());
                }
            } else {
                logger.error("CacheManager implementation is not found in the service.yml");
            }
            return cacheManager;
        } else {
            logger.error("No cache is configured in cache.yml");
            return null;
        }
    }

    void addCache(String cacheName, long maxSize, long expiryInMinutes);
    void put(String cacheName, String key, Object value);
    Object get(String cacheName, String key);
    void removeCache(String cacheName);
}
