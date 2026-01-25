package com.networknt.cache;

import com.networknt.service.SingletonServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public interface CacheManager {
    Logger logger = LoggerFactory.getLogger(CacheManager.class);

    // Holder class for lazy initialization of the CacheManager instance and state
    class Holder {
        static CacheConfig config;
        static volatile CacheManager instance = null;
    }

    static CacheManager getInstance() {
        CacheConfig currentConfig = CacheConfig.load();
        if (Holder.instance == null || Holder.config != currentConfig) {
            synchronized (Holder.class) {
                if (Holder.instance == null || Holder.config != currentConfig) {
                    List<CacheItem> caches = currentConfig.getCaches();
                    if (caches != null && !caches.isEmpty()) {
                        CacheManager cacheManager = Holder.instance;
                        if (cacheManager == null) {
                            cacheManager = SingletonServiceFactory.getBean(CacheManager.class);
                        }
                        if (cacheManager != null) {
                            for (CacheItem cacheItem : caches) {
                                cacheManager.addCache(cacheItem.getCacheName(), cacheItem.getMaxSize(), cacheItem.getExpiryInMinutes());
                            }
                            Holder.instance = cacheManager;
                            Holder.config = currentConfig;
                        } else {
                            logger.error("CacheManager implementation is not found in the service.yml");
                        }
                    } else {
                        logger.error("No cache is configured in cache.yml");
                    }
                }
            }
        }
        return Holder.instance;
    }

    void addCache(String cacheName, long maxSize, long expiryInMinutes);
    Map<Object, Object> getCache(String cacheName);
    void put(String cacheName, String key, Object value);
    Object get(String cacheName, String key);
    void delete(String cacheName, String key);
    void removeCache(String cacheName);
    int getSize(String cacheName);
}
