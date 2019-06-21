package com.networknt.server;

import com.networknt.config.Config;
import org.junit.Assert;
import org.junit.Test;

public class ServerConfigEnvTest {
    public static final String CONFIG_NAME = "server_env_0001";

    public static Config config = Config.getInstance();

    @Test
    public void testStringEnv() {
        config.clear();
        // ensure that env is "0001" instead of "1"
        ServerConfig serverConfig = (ServerConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ServerConfig.class);
        Assert.assertEquals("0001", serverConfig.getEnvironment());
    }
}
