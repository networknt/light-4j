package com.networknt.server;

import org.junit.Assert;
import org.junit.Test;

public class DefaultConfigLoaderIT {
    @Test
    public void testConfigServerHealth() throws Exception {
        Assert.assertEquals("OK", DefaultConfigLoader.getConfigServerHealth("https://localhost:8443", "/config-server/health"));
    }
}
