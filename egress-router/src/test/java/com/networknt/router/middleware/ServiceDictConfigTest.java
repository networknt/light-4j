package com.networknt.router.middleware;

import org.junit.Assert;
import org.junit.Test;

public class ServiceDictConfigTest {
    @Test
    public void testLoadConfig() {
        ServiceDictConfig config = ServiceDictConfig.load(ServiceDictConfig.CONFIG_NAME);
        Assert.assertTrue(config.isEnabled());
        Assert.assertEquals(3, config.getMapping().size());
    }
}
