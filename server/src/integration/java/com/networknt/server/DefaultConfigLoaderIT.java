package com.networknt.server;

import org.junit.Assert;
import org.junit.Test;

public class DefaultConfigLoaderIT {
    @Test
    public void testConfigServerHealth() throws Exception {
        DefaultConfigLoader configLoader = new DefaultConfigLoader();
        Assert.assertEquals("OK", configLoader.getConfigServerHealth("https://localhost:8443", "/health/liveness/com.networknt.config-server-1.0.0"));
    }
}
