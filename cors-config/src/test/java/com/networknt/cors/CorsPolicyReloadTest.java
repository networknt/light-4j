package com.networknt.cors;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CorsPolicyReloadTest {
    @Test
    void supportsMissingEmptyAndExternalizedListRepresentations() {
        String name = "cors-list-" + UUID.randomUUID();
        try {
            for (Map<String, Object> input : List.<Map<String, Object>>of(Map.of(),
                    Map.of("allowedOrigins", List.of()), Map.of("allowedOrigins", "  "))) {
                Config.getInstance().putInConfigCache(name, input);
                assertFalse(CorsConfig.load(name).policyForPath("/").origins().matches("https://app.example.com"));
            }
            for (String input : List.of("https://*.example.com, https://example.com",
                    "[\"https://*.example.com\", \"https://example.com\"]")) {
                Config.getInstance().putInConfigCache(name, Map.of("allowedOrigins", input));
                assertTrue(CorsConfig.load(name).policyForPath("/").origins().matches("https://app.example.com"));
                assertTrue(CorsConfig.load(name).policyForPath("/").origins().matches("https://example.com"));
            }
        } finally {
            Config.getInstance().clearConfigCache(name);
        }
    }

    @Test
    void validatesStartupAndAtomicallyRetainsLastValidPolicyOnRefresh() {
        String name = "cors-policy-" + UUID.randomUUID();
        Config source = Config.getInstance();
        try {
            source.putInConfigCache(name, Map.of("allowedOrigins", List.of("https://*.co.uk")));
            assertThrows(ConfigException.class, () -> CorsConfig.load(name));
            source.putInConfigCache(name, Map.of("allowedOrigins", List.of("https://*.example.com")));
            CorsConfig first = CorsConfig.load(name);
            assertTrue(first.policyForPath("/").origins().matches("https://app.example.com"));
            assertSame(first, CorsConfig.load(name));
            source.putInConfigCache(name, Map.of("allowedOrigins", List.of("https://*.example.org"),
                    "pathPrefixAllowed", Map.of("/api", Map.of("allowedOrigins", List.of("*")))));
            assertSame(first, CorsConfig.load(name));
            assertSame(first, CorsConfig.load(name));
            assertFalse(CorsConfig.load(name).policyForPath("/").origins().matches("https://app.example.org"));
            source.putInConfigCache(name, Map.of("allowedOrigins", List.of("https://*.example.org")));
            CorsConfig second = CorsConfig.load(name);
            assertNotSame(first, second);
            assertTrue(second.policyForPath("/").origins().matches("https://app.example.org"));
            assertFalse(second.policyForPath("/").origins().matches("https://app.example.com"));
        } finally {
            source.clearConfigCache(name);
        }
    }

    @Test
    void preservesFirstRawPrefixMatchReplacementAndGlobalFallback() {
        String name = "cors-paths-" + UUID.randomUUID();
        Map<String, Object> paths = new LinkedHashMap<>();
        paths.put("/api", Map.of("allowedOrigins", List.of("https://trusted.example.com"), "allowedMethods", List.of("GET")));
        paths.put("/api/admin", Map.of("allowedOrigins", List.of("https://admin.example.com")));
        try {
            Config.getInstance().putInConfigCache(name, Map.of("allowedOrigins", List.of("https://*.example.com"),
                    "allowedMethods", List.of("PUT"), "pathPrefixAllowed", paths));
            CorsConfig config = CorsConfig.load(name);
            for (String path : List.of("/api", "/api/1", "/api/admin", "/api-other")) {
                CorsConfig.Policy selected = config.policyForPath(path);
                assertTrue(selected.origins().matches("https://trusted.example.com"));
                assertFalse(selected.origins().matches("https://arbitrary.example.com"));
                assertEquals(List.of("GET"), selected.methods());
            }
            assertTrue(config.policyForPath("/other").origins().matches("https://arbitrary.example.com"));
            assertEquals(List.of("PUT"), config.policyForPath("/other").methods());
            config.setPathPrefixAllowed(Map.of("/api", Map.of()));
            assertFalse(config.policyForPath("/api").origins().matches("https://arbitrary.example.com"));
            assertTrue(config.policyForPath("/api").methods().isEmpty());
            assertThrows(ConfigException.class, () -> config.setAllowedOrigins(List.of("*")));
            assertTrue(config.policyForPath("/other").origins().matches("https://arbitrary.example.com"));
        } finally {
            Config.getInstance().clearConfigCache(name);
        }
    }
}
