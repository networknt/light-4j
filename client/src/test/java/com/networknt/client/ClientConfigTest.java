package com.networknt.client;

import com.networknt.config.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


import java.util.HashMap;

import static com.networknt.client.ClientConfig.CONFIG_NAME;
import static com.networknt.client.ClientConfig.CONFIG_SECRET;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = {"com.networknt.config.Config", "org.slf4j.LoggerFactory"})
public class ClientConfigTest {

    private Config config;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Config.class);
        config = Mockito.mock(Config.class);
        Mockito.when(Config.getInstance()).thenReturn(config);
    }

    @After
    public void cleanUp() {
        ClientConfig.reset();
    }

    @Test
    public void shouldLoadNullConfig() {
        Mockito.when(config.getJsonMapConfig(CONFIG_NAME)).thenReturn(null);

        ClientConfig clientConfig = ClientConfig.get();

        assertEquals(ClientConfig.DEFAULT_ERROR_THRESHOLD, clientConfig.getErrorThreshold());
        assertEquals(ClientConfig.DEFAULT_RESET_TIMEOUT, clientConfig.getResetTimeout());
        assertEquals(ClientConfig.DEFAULT_TIMEOUT, clientConfig.getTimeout());
        assertEquals(ClientConfig.DEFAULT_BUFFER_SIZE, clientConfig.getBufferSize());
        assertFalse(clientConfig.isInjectOpenTracing());
        assertNull(clientConfig.getSecretConfig());
        assertNull(clientConfig.getTokenConfig());
    }


    @Test
    public void shouldLoadEmptyConfig() {
        Mockito.when(config.getJsonMapConfig(CONFIG_NAME)).thenReturn(new HashMap<>());
        Mockito.when(config.getJsonMapConfig(CONFIG_SECRET)).thenReturn(new HashMap<>());

        ClientConfig clientConfig = ClientConfig.get();

        assertEquals(ClientConfig.DEFAULT_ERROR_THRESHOLD, clientConfig.getErrorThreshold());
        assertEquals(ClientConfig.DEFAULT_RESET_TIMEOUT, clientConfig.getResetTimeout());
        assertEquals(ClientConfig.DEFAULT_TIMEOUT, clientConfig.getTimeout());
        assertEquals(ClientConfig.DEFAULT_BUFFER_SIZE, clientConfig.getBufferSize());
        assertFalse(clientConfig.isInjectOpenTracing());
        assertTrue(clientConfig.getSecretConfig() instanceof HashMap);
        assertNull(clientConfig.getTokenConfig());
    }

    @Test
    public void shouldLoadCompleteConfig() {
        Mockito.when(config.getJsonMapConfig(CONFIG_NAME)).thenReturn(getCompleteConfig());

        ClientConfig clientConfig = ClientConfig.get();

        assertEquals(3, clientConfig.getErrorThreshold());
        assertEquals(3600000, clientConfig.getResetTimeout());
        assertEquals(2000, clientConfig.getTimeout());
        assertEquals(1024, clientConfig.getBufferSize());
        assertTrue(clientConfig.isInjectOpenTracing());
        assertTrue(clientConfig.getSecretConfig() instanceof HashMap);
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
}
