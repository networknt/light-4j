package com.networknt.router.middleware;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServiceDictConfigTest {
    @Test
    public void testLoadConfig() {
        ServiceDictConfig config = ServiceDictConfig.load(ServiceDictConfig.CONFIG_NAME);
        Assertions.assertTrue(config.isEnabled());
        Assertions.assertNull(config.getMapping());
    }
}
