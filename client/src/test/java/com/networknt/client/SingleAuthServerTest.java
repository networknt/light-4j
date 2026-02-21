package com.networknt.client;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class SingleAuthServerTest {
    public static final String CONFIG_NAME = "client-single";
    static ClientConfig config;

    @BeforeAll
    public static void beforeClass() throws IOException {
        config = ClientConfig.get(CONFIG_NAME);
    }

    /**
     * Test the client.yml with single auth server in the configuration for
     * client credentials and key.
     */
    @Test
    public void multipleAuthServerTrue() {
        assertFalse(config.getOAuth().isMultipleAuthServers());
    }
}
