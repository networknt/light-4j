package com.networknt.cache;

import org.junit.Assert;
import org.junit.Test;

public class CacheConfigTest {
    @Test
    public void testConfig() {
        CacheConfig config = CacheConfig.load();
        System.out.println("config = " + config);
        Assert.assertEquals(2, config.getCaches().size());
    }
}
