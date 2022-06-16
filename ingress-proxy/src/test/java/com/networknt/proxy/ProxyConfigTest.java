package com.networknt.proxy;

import org.junit.Assert;
import org.junit.Test;

public class ProxyConfigTest {
    @Test
    public void testLoadConfig() {
        ProxyConfig config = ProxyConfig.load();
        Assert.assertNotNull(config.getHosts());
    }
}
