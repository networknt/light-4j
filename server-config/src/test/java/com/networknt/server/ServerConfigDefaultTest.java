package com.networknt.server;

import com.networknt.config.Config;
import org.junit.Assert;
import org.junit.Test;

public class ServerConfigDefaultTest {

    public static Config config = Config.getInstance();

    @Test
    public void testDefaultServerOptions() {
        config.clear();
        ServerConfig serverConfig = ServerConfig.load("default");
        Assert.assertNull(serverConfig.getShutdownTimeout());
    }

}
