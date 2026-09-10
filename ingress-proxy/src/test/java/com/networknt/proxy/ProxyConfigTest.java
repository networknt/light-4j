package com.networknt.proxy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProxyConfigTest {
    @Test
    public void testLoadConfig() {
        ProxyConfig config = ProxyConfig.load();
        Assertions.assertNotNull(config.getHosts());
        Assertions.assertEquals(config.getMaxQueueSize(), 0);
        Assertions.assertEquals("text/event-stream", config.getStreamResponseContentTypes().get(0));
        Assertions.assertEquals("text/event-stream", config.getStreamRequestAcceptTypes().get(0));
        Assertions.assertTrue(config.getStreamPathPrefixes().isEmpty());
        Assertions.assertEquals(0, config.getStreamMaxRequestTime());
        Assertions.assertEquals(0, config.getStreamIdleTimeout());
        Assertions.assertTrue(config.getStreamResponseHeaderOverwrite().contains("Content-Type"));
    }

    @Test
    public void testOnlyDefaultConfigNameIsCached() {
        ProxyConfig defaultConfig = ProxyConfig.load();
        // A handler constructed with a custom config name must not take over the shared instance.
        ProxyConfig namedConfig = ProxyConfig.load(ExternalServiceConfig.CONFIG_NAME);

        Assertions.assertNotSame(defaultConfig, namedConfig);
        Assertions.assertNotSame(namedConfig, ProxyConfig.load(ExternalServiceConfig.CONFIG_NAME));
        Assertions.assertSame(defaultConfig, ProxyConfig.load());
    }
}
