package com.networknt.client;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;

public class SingleAuthServerTest {
    public static final String CONFIG_NAME = "client-single";
    static ClientConfig config;

    @BeforeClass
    public static void beforeClass() throws IOException {
        config = ClientConfig.get(CONFIG_NAME);
    }

    /**
     * Test the client.yml with single auth server in the configuration for
     * client credentials and key.
     */
    @Test
    public void multipleAuthServerTrue() {
        assertFalse(config.isMultipleAuthServers());
    }
}
