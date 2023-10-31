package com.networknt.cache;

import org.junit.Assert;
import org.junit.Test;

public class DefaultCacheManagerTest {
    @Test
    public void testDefaultCacheManager() {
        CacheManager cacheManager = CacheManager.getInstance();
        cacheManager.addCache("test", 100, 10);
        cacheManager.put("test", "key", "value");
        Object value = cacheManager.get("test", "key");
        Assert.assertEquals("value", value);
        cacheManager.removeCache("test");
        value = cacheManager.get("test", "key");
        Assert.assertNull(value);
    }

    @Test
    public void testConfiguredCacheManager() {
        CacheManager cacheManager = CacheManager.getInstance();
        // there should be two caches in the cache manager.
        cacheManager.put("cache1", "key", "value");
        Object value = cacheManager.get("cache1", "key");
        Assert.assertEquals("value", value);
    }
}
