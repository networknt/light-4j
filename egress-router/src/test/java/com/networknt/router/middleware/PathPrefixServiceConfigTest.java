package com.networknt.router.middleware;

import org.junit.Assert;
import org.junit.Test;

public class PathPrefixServiceConfigTest {
    @Test
    public void testLoadConfig() {
        PathPrefixServiceConfig config = PathPrefixServiceConfig.load(PathPrefixServiceConfig.CONFIG_NAME);
        Assert.assertTrue(config.isEnabled());
        Assert.assertEquals(3, config.getMapping().size());
    }
}
