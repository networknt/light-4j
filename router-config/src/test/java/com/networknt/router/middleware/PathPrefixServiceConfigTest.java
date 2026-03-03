package com.networknt.router.middleware;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PathPrefixServiceConfigTest {
    @Test
    public void testLoadConfig() {
        PathPrefixServiceConfig config = PathPrefixServiceConfig.load(PathPrefixServiceConfig.CONFIG_NAME);
        Assertions.assertTrue(config.isEnabled());
        Assertions.assertEquals(3, config.getMapping().size());
    }
    @Test
    public void testJsonMapping() {
        PathPrefixServiceConfig config = PathPrefixServiceConfig.load(PathPrefixServiceConfig.CONFIG_NAME);
        Assertions.assertTrue(config.isEnabled());
        Assertions.assertEquals(3, config.getMapping().size());
        Assertions.assertEquals("party.address-2.0.0", config.getMapping().get("/v2/address"));
    }
}
