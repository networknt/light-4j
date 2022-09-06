package com.networknt.router.middleware;

import org.junit.Assert;
import org.junit.Test;

public class PathServiceConfigTest {
    @Test
    public void testLoadConfig() {
        PathServiceConfig config = PathServiceConfig.load(PathServiceConfig.CONFIG_NAME);
        Assert.assertTrue(config.isEnabled());
        Assert.assertEquals(3, config.getMapping().size());
    }
}
