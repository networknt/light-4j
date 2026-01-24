package com.networknt.cache;

import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.utility.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public interface CacheManager {
    Logger logger = LoggerFactory.getLogger(CacheManager.class);

    // Holder class for lazy initialization of the CacheManager instance and state
    class Holder {
        static volatile boolean initialized = false;
        static volatile CacheManager instance = null;
    }

    static CacheManager getInstance() {
        if (!Holder.initialized) {
            synchronized (Holder.class) {
                if (!Holder.initialized) {
                    CacheConfig config = CacheConfig.load();
                    ModuleRegistry.registerModule(CacheConfig.CONFIG_NAME, CacheManager.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfig(CacheConfig.CONFIG_NAME), null);
                    List<CacheItem> caches = config.getCaches();
                    if (caches != null && !caches.isEmpty()) {
                        CacheManager cacheManager = SingletonServiceFactory.getBean(CacheManager.class);
                        if (cacheManager != null) {
                            for (CacheItem cacheItem : caches) {
                                cacheManager.addCache(cacheItem.getCacheName(), cacheItem.getMaxSize(), cacheItem.getExpiryInMinutes());
                            }
                            Holder.instance = cacheManager;
                        } else {
                            logger.error("CacheManager implementation is not found in the service.yml");
                        }
                    } else {
                        logger.error("No cache is configured in cache.yml");
                    }
                    Holder.initialized = true;
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
