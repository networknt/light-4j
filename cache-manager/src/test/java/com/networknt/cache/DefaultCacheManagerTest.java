package com.networknt.cache;

import com.github.benmanes.caffeine.cache.Cache;
import org.junit.Assert;
import org.junit.Test;

public class DefaultCacheManagerTest {
    @Test
    public void testDefaultCacheManager() {
        CacheManager cacheManager = CacheManager.getInstance();
        cacheManager.addCache("test", 100, 10);
        Cache<Object, Object> cache = cacheManager.getCache("test");
        cache.put("key", "value");
        Assert.assertEquals("value", cache.getIfPresent("key"));
        cacheManager.removeCache("test");
        Assert.assertNull(cache.getIfPresent("key"));
    }

    @Test
    public void testConfiguredCacheManager() {
        CacheManager cacheManager = CacheManager.getInstance();
        // there should be two caches in the cache manager.
        Cache<Object, Object> cache1 = cacheManager.getCache("cache1");
        cache1.put("key", "value");
        Assert.assertEquals("value", cache1.getIfPresent("key"));
    }
}
