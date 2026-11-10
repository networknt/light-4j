package com.networknt.cors;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CorsContractRegressionTest {
    @Test
    void missingConfigAndMissingEnabledRemainDisabled() {
        String name = "missing-cors-" + UUID.randomUUID();
        assertFalse(CorsConfig.load(name).isEnabled());
        assertNull(CorsConfig.load(name).getAllowedOrigins());
        assertNull(CorsConfig.load(name).getAllowedMethods());
        try {
            Config.getInstance().putInConfigCache(name, Map.of("allowedOrigins", List.of("*")));
            assertFalse(CorsConfig.load(name).isEnabled());
            assertNull(CorsConfig.load(name).getAllowedMethods());
        } finally {
            Config.getInstance().clearConfigCache(name);
        }
    }

    @Test
    void namedStartupDiagnosticsIdentifyConfigAndExactPathKey() {
        String name = "named-cors-" + UUID.randomUUID();
        try {
            Config.getInstance().putInConfigCache(name, Map.of("enabled", true, "allowedOrigins", List.of("https://*.co.uk")));
            ConfigException global = assertThrows(ConfigException.class, () -> CorsConfig.load(name));
            assertTrue(global.getMessage().contains(name + ".allowedOrigins[0]"), global.getMessage());
            Config.getInstance().putInConfigCache(name, Map.of("enabled", true, "pathPrefixAllowed",
                    Map.of("/allowedOrigins-v1", Map.of("allowedMethods", Arrays.asList("GET", null)))));
            ConfigException path = assertThrows(ConfigException.class, () -> CorsConfig.load(name));
            assertTrue(path.getMessage().contains(name + ".pathPrefixAllowed[/allowedOrigins-v1].allowedMethods[1]"), path.getMessage());
        } finally {
            Config.getInstance().clearConfigCache(name);
        }
    }
}
