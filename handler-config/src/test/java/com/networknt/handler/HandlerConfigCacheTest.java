package com.networknt.handler;

import com.networknt.config.Config;
import com.networknt.handler.config.HandlerConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HandlerConfigCacheTest {
    // A second supported view of handler.yml; production HandlerConfig.load() still uses its map.
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class HandlerView {
        public boolean enabled;
    }

    @Test
    void mixedReadsPreserveSingletonUntilExplicitReload() {
        Config config = Config.getInstance();
        String name = HandlerConfig.CONFIG_NAME;
        try {
            for (boolean typedFirst : new boolean[]{true, false}) {
                config.clearConfigCache(name);
                if (typedFirst) config.getJsonObjectConfig(name, HandlerView.class);
                HandlerConfig first = HandlerConfig.load();
                Object typed = config.getJsonObjectConfig(name, HandlerView.class);
                assertSame(first, HandlerConfig.load());
                assertSame(first.getMappedConfig(), config.getJsonMapConfig(name));
                assertSame(typed, config.getJsonObjectConfig(name, HandlerView.class));
                config.clearConfigCache(name);
                HandlerConfig second = HandlerConfig.load();
                assertNotSame(first, second);
                assertNotSame(first.getMappedConfig(), second.getMappedConfig());
                assertSame(second, HandlerConfig.load());
                assertNotSame(typed, config.getJsonObjectConfig(name, HandlerView.class));
            }
        } finally {
            config.clearConfigCache(name);
        }
    }
}
