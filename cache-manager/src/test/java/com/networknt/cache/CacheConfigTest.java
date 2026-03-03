package com.networknt.cache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CacheConfigTest {
    @Test
    public void testConfig() {
        CacheConfig config = CacheConfig.load();
        System.out.println("config = " + config);
        Assertions.assertEquals(2, config.getCaches().size());
    }
}
