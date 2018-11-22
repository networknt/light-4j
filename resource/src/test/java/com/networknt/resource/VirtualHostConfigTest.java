package com.networknt.resource;

import com.networknt.config.Config;
import org.junit.Assert;
import org.junit.Test;

public class VirtualHostConfigTest {
    @Test
    public void testLoadConfig() {
        VirtualHostConfig config = (VirtualHostConfig) Config.getInstance().getJsonObjectConfig(VirtualHostConfig.CONFIG_NAME, VirtualHostConfig.class);
        Assert.assertEquals(config.hosts.size(), 4);
    }
}
