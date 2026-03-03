package com.networknt.proxy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProxyConfigTest {
    @Test
    public void testLoadConfig() {
        ProxyConfig config = ProxyConfig.load();
        Assertions.assertNotNull(config.getHosts());
        Assertions.assertEquals(config.getMaxQueueSize(), 0);
    }
}
