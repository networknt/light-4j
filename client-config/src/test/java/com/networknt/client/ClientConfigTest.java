package com.networknt.client;

import com.networknt.config.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;


import java.util.HashMap;
import java.util.Map;

import static com.networknt.client.ClientConfig.CONFIG_NAME;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * This is a test class that deal with different configuration values for client.yml
 *
 */
public class ClientConfigTest {

    @Test
    @Ignore
    public void shouldLoadNullConfig() {
        ClientConfig clientConfig = ClientConfig.get();
        assertEquals(ClientConfig.DEFAULT_ERROR_THRESHOLD, clientConfig.getErrorThreshold());
        assertEquals(ClientConfig.DEFAULT_RESET_TIMEOUT, clientConfig.getResetTimeout());
        assertEquals(ClientConfig.DEFAULT_TIMEOUT, clientConfig.getTimeout());
        assertFalse(clientConfig.isInjectOpenTracing());
        assertNull(clientConfig.getTokenConfig());
    }


    @Test
    @Ignore
    public void shouldLoadEmptyConfig() {
        ClientConfig clientConfig = ClientConfig.get();
        assertEquals(ClientConfig.DEFAULT_ERROR_THRESHOLD, clientConfig.getErrorThreshold());
        assertEquals(ClientConfig.DEFAULT_RESET_TIMEOUT, clientConfig.getResetTimeout());
        assertEquals(ClientConfig.DEFAULT_TIMEOUT, clientConfig.getTimeout());
        assertFalse(clientConfig.isInjectOpenTracing());
        assertNull(clientConfig.getTokenConfig());
    }

    @Test
    @Ignore
    public void shouldLoadCompleteConfig() {
        ClientConfig clientConfig = ClientConfig.get();
        assertEquals(3, clientConfig.getErrorThreshold());
        assertEquals(3600000, clientConfig.getResetTimeout());
        assertEquals(2000, clientConfig.getTimeout());
        assertTrue(clientConfig.isInjectOpenTracing());
        assertTrue(clientConfig.getTokenConfig() instanceof HashMap);
    }

    private HashMap<String, Object> getCompleteConfig() {
        HashMap<String, Object> oauthConfig = new HashMap<>();
        oauthConfig.put("token", new HashMap<String, Object>());

        HashMap<String, Object> configMap = new HashMap<>();
        configMap.put("errorThreshold", 3);
        configMap.put("timeout", 2000);
        configMap.put("resetTimeout", 3600000);
        configMap.put("injectOpenTracing", true);

        HashMap<String, Object> map = new HashMap<>();
        map.put("bufferSize", 1024);
        map.put("oauth", oauthConfig);
        map.put("request", configMap);
        return map;
    }

    @Test
    public void testServiceIdAuthServers() {
        ClientConfig clientConfig = ClientConfig.get();
        Map<String, Object> tokenConfig = clientConfig.getTokenConfig();
        Map<String, Object> ccConfig = (Map<String, Object>) tokenConfig.get(ClientConfig.CLIENT_CREDENTIALS);
        if (clientConfig.isMultipleAuthServers()) {
            // iterate all the configured auth server to get JWK.
            Object object = ccConfig.get(ClientConfig.SERVICE_ID_AUTH_SERVERS);
            Map<String, Object> serviceIdAuthServers = ClientConfig.getServiceIdAuthServers(object);
            assertEquals(2, serviceIdAuthServers.size());
        }
    }

}
