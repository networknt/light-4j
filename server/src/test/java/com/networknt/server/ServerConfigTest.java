package com.networknt.server;

import com.networknt.config.Config;
import org.junit.Assert;
import org.junit.Test;

public class ServerConfigTest {

    public static final String CONFIG_NAME = "server";
    @Test
    public void testNullEnv() {
        // ensure that env is null if it is missing in the server.yml

        ServerConfig config = (ServerConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ServerConfig.class);
        Assert.assertNull(config.getEnvironment());
    }
}
