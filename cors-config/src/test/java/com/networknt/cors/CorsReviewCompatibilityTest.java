package com.networknt.cors;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CorsReviewCompatibilityTest {
    @Test
    void exactOriginsRetainInternalHostsAndCustomSchemes() {
        for (String origin : List.of("http://my_service:8080", "https://example.com.", "capacitor://localhost",
                "ionic://localhost", "chrome-extension://abcdefghijklmnop", "tauri://localhost")) {
            CorsOriginPolicy policy = CorsOriginPolicy.compile(List.of(origin), "allowedOrigins");
            assertTrue(policy.matches(origin), origin);
            assertTrue(CorsOriginPolicy.sameOrigin(origin, origin), origin);
        }
    }

    @Test
    void disabledCorsDoesNotValidateUnusedWildcardsUntilEnabled() {
        String name = "cors-disabled-" + UUID.randomUUID();
        try {
            Config.getInstance().putInConfigCache(name, Map.of("enabled", false, "allowedOrigins", List.of("*"),
                    "pathPrefixAllowed", Map.of("/api", Map.of("allowedOrigins", List.of("https://*.co.uk")))));
            CorsConfig config = CorsConfig.load(name);
            assertFalse(config.isEnabled());
            assertThrows(ConfigException.class, () -> config.setEnabled(true));
            assertFalse(config.isEnabled());
            config.setAllowedOrigins(List.of("https://example.com"));
            config.setPathPrefixAllowed(Map.of("/api", Map.of("allowedOrigins", List.of("https://*.example.org"))));
            config.setEnabled(true);
            assertTrue(config.isEnabled());
            assertTrue(config.policyForPath("/api").origins().matches("https://app.example.org"));
        } finally {
            Config.getInstance().clearConfigCache(name);
        }
    }

    @Test
    void startupElementErrorsHaveTheSameDiagnosticsAsSetters() {
        String name = "cors-startup-elements-" + UUID.randomUUID();
        try {
            for (String property : List.of("allowedOrigins", "allowedMethods")) {
                String valid = property.equals("allowedOrigins") ? "https://example.com" : "GET";
                Config.getInstance().putInConfigCache(name, Map.of(property, List.of(valid, 42)));
                ConfigException error = assertThrows(ConfigException.class, () -> CorsConfig.load(name));
                assertTrue(error.getMessage().contains(property + "[1]"), error.getMessage());
            }
        } finally {
            Config.getInstance().clearConfigCache(name);
        }
    }

    @Test
    void emptyCommaSeparatedItemsDoNotBreakValidOrigins() {
        String name = "cors-commas-" + UUID.randomUUID();
        try {
            Config.getInstance().putInConfigCache(name, Map.of("allowedOrigins", ",https://example.com,, https://*.example.org,",
                    "allowedMethods", ",GET,,PUT,"));
            CorsConfig config = CorsConfig.load(name);
            assertTrue(config.policyForPath("/").origins().matches("https://example.com"));
            assertTrue(config.policyForPath("/").origins().matches("https://app.example.org"));
            assertEquals(List.of("GET", "PUT"), config.policyForPath("/").methods());
        } finally {
            Config.getInstance().clearConfigCache(name);
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void invalidElementTypesReportPropertyAndIndexInsteadOfJdkExceptions() {
        String name = "cors-elements-" + UUID.randomUUID();
        try {
            Config.getInstance().putInConfigCache(name, Map.of("allowedOrigins", List.of("https://example.com")));
            CorsConfig config = CorsConfig.load(name);
            ConfigException methods = assertThrows(ConfigException.class,
                    () -> config.setAllowedMethods(Arrays.asList("GET", null)));
            assertTrue(methods.getMessage().contains("allowedMethods[1]"), methods.getMessage());
            ConfigException origins = assertThrows(ConfigException.class,
                    () -> config.setAllowedOrigins((List) List.of("https://example.com", 42)));
            assertTrue(origins.getMessage().contains("allowedOrigins[1]"), origins.getMessage());
            assertTrue(config.policyForPath("/").origins().matches("https://example.com"));
        } finally {
            Config.getInstance().clearConfigCache(name);
        }
    }
}
