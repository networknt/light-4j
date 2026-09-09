package com.networknt.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMixedRepresentationTest {
    @Test
    void mixedReadsWorkInEitherOrder() throws Exception {
        for (boolean defaults : new boolean[]{false, true}) {
            for (boolean mapFirst : new boolean[]{false, true}) {
                Config config = freshConfig();
                Object first = mapFirst ? map(config, defaults) : object(config, defaults);
                TestConfig uncached = (TestConfig) config.getJsonObjectConfigNoCache("test", TestConfig.class);
                Map<String, Object> mapped = map(config, defaults);
                TestConfig typed = assertInstanceOf(TestConfig.class, object(config, defaults));
                assertEquals(uncached.getValue(), typed.getValue());
                assertEquals(uncached.getValue(), mapped.get("value"));
                assertSame(first, mapFirst ? map(config, defaults) : object(config, defaults));
                config.clearConfigCache("test");
                assertNotSame(first, mapFirst ? map(config, defaults) : object(config, defaults));
            }
        }
    }

    @Test
    void incompatibleInjectedValueDoesNotChangeRequestedType() throws Exception {
        Config config = freshConfig();
        Map<String, Object> injected = Map.of("value", "injected");
        config.putInConfigCache("test", injected);
        assertInstanceOf(TestConfig.class, object(config, false));
        assertSame(injected, map(config, false));
        config.clear();
        assertNotSame(injected, map(config, false));
    }

    private Config freshConfig() throws Exception {
        var constructor = Config.getInstance().getClass().getDeclaredConstructor();
        constructor.setAccessible(true);
        return (Config) constructor.newInstance();
    }

    private Map<String, Object> map(Config config, boolean defaults) {
        return defaults ? config.getDefaultJsonMapConfig("test") : config.getJsonMapConfig("test");
    }

    private Object object(Config config, boolean defaults) {
        return defaults ? config.getDefaultJsonObjectConfig("test", TestConfig.class)
                : config.getJsonObjectConfig("test", TestConfig.class);
    }
}
