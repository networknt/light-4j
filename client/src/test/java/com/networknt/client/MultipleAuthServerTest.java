package com.networknt.client;

import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.networknt.client.ClientConfig.CLIENT_CREDENTIALS;
import static com.networknt.client.ClientConfig.SERVICE_ID_AUTH_SERVERS;
import static junit.framework.TestCase.assertTrue;

/**
 * Test the client.yml with multiple auth servers in the configuration for
 * client credentials and key.
 */
public class MultipleAuthServerTest {
    public static final String CONFIG_NAME = "client-multiple";
    static ClientConfig config;

    @BeforeClass
    public static void beforeClass() throws IOException {
        config = ClientConfig.get(CONFIG_NAME);
    }

    @Test
    public void multipleAuthServerTrue() {
        assertTrue(config.isMultipleAuthServers());
    }
    @Test
    public void pathPrefixServiceExists() {
        Map<String, String> pathPrefixServices = config.getPathPrefixServices();
        assertTrue(pathPrefixServices.size() > 0);
    }

    @Test
    public void ccAuthServersMapExists() {
        Map<String, Object> tokenConfig = config.getTokenConfig();
        Map<String, Object> ccMap = (Map<String, Object>)tokenConfig.get(CLIENT_CREDENTIALS);
        Map<String, Object> serverMap = (Map<String, Object>)ccMap.get(SERVICE_ID_AUTH_SERVERS);
        assertTrue(serverMap.size() > 0);
    }

}
