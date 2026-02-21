package com.networknt.server;

import com.networknt.config.Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServerConfigDefaultTest {

    public static Config config = Config.getInstance();

    @Test
    public void testDefaultServerOptions() {
        config.clear();
        ServerConfig serverConfig = ServerConfig.load("default");
        Assertions.assertNull(serverConfig.getShutdownTimeout());
    }

}
