package com.networknt.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DefaultConfigLoaderIT {
    @Test
    public void testConfigServerHealth() throws Exception {
        DefaultConfigLoader configLoader = new DefaultConfigLoader();
        Assertions.assertEquals("OK", configLoader.getConfigServerHealth("https://localhost:8443", "/health/liveness/com.networknt.config-server-1.0.0"));
    }
}
