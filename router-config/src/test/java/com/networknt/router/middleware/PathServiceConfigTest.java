package com.networknt.router.middleware;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PathServiceConfigTest {
    @Test
    public void testLoadConfig() {
        PathServiceConfig config = PathServiceConfig.load(PathServiceConfig.CONFIG_NAME);
        Assertions.assertTrue(config.isEnabled());
        Assertions.assertNull(config.getMapping());
    }
}
