package com.networknt.resource;

import com.networknt.config.Config;
import org.junit.Assert;
import org.junit.Test;

public class PathResourceConfigTest {
    @Test
    public void testLoadConfig() {
        PathResourceConfig config = (PathResourceConfig) Config.getInstance().getJsonObjectConfig(PathResourceConfig.CONFIG_NAME, PathResourceConfig.class);
        Assert.assertEquals(config.directoryListingEnabled, false);
    }
}
