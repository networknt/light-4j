package com.networknt.client;

import com.networknt.client.oauth.TokenKeyRequest;
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

    @Test
    public void testKeyConfig() {
        ClientConfig clientConfig = ClientConfig.get();
        if (clientConfig.isMultipleAuthServers()) {
            // iterate all the configured auth server to get JWK.
            Map<String, Object> tokenConfig = clientConfig.getTokenConfig();
            Map<String, Object> keyConfig = (Map<String, Object>) tokenConfig.get(ClientConfig.KEY);
            Map<String, Object> serviceIdAuthServers = (Map<String, Object>) keyConfig.get(ClientConfig.SERVICE_ID_AUTH_SERVERS);
            if (serviceIdAuthServers == null) {
                throw new RuntimeException("serviceIdAuthServers property is missing in the token key configuration");
            }
            for (Map.Entry<String, Object> entry : serviceIdAuthServers.entrySet()) {
                Map<String, Object> authServerConfig = (Map<String, Object>) entry.getValue();
                TokenKeyRequest keyRequest = new TokenKeyRequest(null, true, authServerConfig);
                System.out.println("keyRequest = " + JsonMapper.toJson(keyRequest));
            }
        }
    }
}
