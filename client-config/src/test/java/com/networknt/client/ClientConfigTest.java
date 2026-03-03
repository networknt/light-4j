package com.networknt.client;

import com.networknt.config.Config;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is a test class that deal with different configuration values for client.yml
 *
 */
public class ClientConfigTest {

    @Test
    @Disabled
    public void shouldLoadNullConfig() {
        ClientConfig clientConfig = ClientConfig.get();
        assertEquals(ClientConfig.DEFAULT_ERROR_THRESHOLD, clientConfig.getErrorThreshold());
        assertEquals(ClientConfig.DEFAULT_RESET_TIMEOUT, clientConfig.getResetTimeout());
        assertEquals(ClientConfig.DEFAULT_TIMEOUT, (int)clientConfig.getRequest().getTimeout());
        assertEquals(ClientConfig.DEFAULT_CONNECT_TIMEOUT, (int)clientConfig.getRequest().getConnectTimeout());
        assertFalse(clientConfig.isInjectOpenTracing());
        assertNotNull(clientConfig.getOAuth().getToken());
    }


    @Test
    @Disabled
    public void shouldLoadEmptyConfig() {
        ClientConfig clientConfig = ClientConfig.get();
        assertEquals(ClientConfig.DEFAULT_ERROR_THRESHOLD, (int)clientConfig.getRequest().getErrorThreshold());
        assertEquals(ClientConfig.DEFAULT_RESET_TIMEOUT, clientConfig.getResetTimeout());
        assertEquals(ClientConfig.DEFAULT_TIMEOUT, (int)clientConfig.getRequest().getTimeout());
        assertEquals(ClientConfig.DEFAULT_CONNECT_TIMEOUT, (int)clientConfig.getRequest().getConnectTimeout());
        assertFalse(clientConfig.isInjectOpenTracing());
        assertTrue(clientConfig.getOAuth().getToken() != null);
        assertTrue(clientConfig.getOAuth().getToken() instanceof OAuthTokenConfig);
    }

    @Test
    @Disabled
    public void shouldLoadCompleteConfig() {
        ClientConfig clientConfig = ClientConfig.get();
        assertEquals(2, clientConfig.getErrorThreshold());
        assertEquals(7000, clientConfig.getResetTimeout());
        assertEquals(3000, (int)clientConfig.getRequest().getTimeout());
        assertEquals(2000, (int)clientConfig.getRequest().getConnectTimeout());
        assertFalse(clientConfig.isInjectOpenTracing());
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
        OAuthTokenConfig tokenConfig = clientConfig.getOAuth().getToken();
        OAuthTokenClientCredentialConfig ccConfig = tokenConfig.getClientCredentials();
        if (clientConfig.getOAuth().isMultipleAuthServers()) {
            // iterate all the configured auth server to get JWK.
            Map<String, AuthServerConfig> serviceIdAuthServers = ccConfig.getServiceIdAuthServers();
            assertEquals(2, serviceIdAuthServers.size());
        }
    }

    private static class LoadConfigTask implements Runnable {

        private final CyclicBarrier barrier;
        private final AtomicBoolean loadingIssue;

        public LoadConfigTask(CyclicBarrier barrier, AtomicBoolean failedState) {
            this.barrier = barrier;
            this.loadingIssue = failedState;
        }

        @Override
        public void run() {
            try {
                barrier.await();
                var config = Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(ClientConfig.CONFIG_NAME);
                assert config != null;
            } catch (Exception e) {
                e.printStackTrace();
                this.loadingIssue.compareAndSet(false, true);
            }
        }
    }

    /**
     * This test is to simulate multiple threads accessing the config at the same time. This is possible in a server
     * environment when there are multiple requests coming in and each request will create a new client to call
     * another service. If there is any issue with multiple thread access, it will be captured by the failedState.
     * Before the fix, this test would fail intermittently. This test should ALWAYS pass.
     */
    @Test
    public void testMultipleThreadAccess() {
        final var threadCount = 100;
        final var failedState = new AtomicBoolean(false);
        final var barrier = new CyclicBarrier(threadCount);
        final var threads = new ArrayList<Thread>(threadCount);
        for (int i = 0; i < threadCount; i++) {
            threads.add(new Thread(new LoadConfigTask(barrier, failedState)));
        }
        for (var thread : threads) {
            thread.start();
        }
        for (var thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                // do nothing
            }
        }
        assertFalse(failedState.get());
    }

}
