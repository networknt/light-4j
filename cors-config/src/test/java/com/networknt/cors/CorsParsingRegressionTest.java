package com.networknt.cors;

import com.networknt.config.ConfigException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CorsParsingRegressionTest {
    @Test
    void fallbackIsLazyAndOnlyUsedForAValidUnmatchedOrigin() {
        CorsOriginPolicy policy = CorsOriginPolicy.compile(List.of("https://*.example.com"), "test");
        AtomicInteger fallbackCalls = new AtomicInteger();
        assertTrue(policy.matches("https://app.example.com", () -> {
            throw new AssertionError("Allowed requests must not compute the server origin");
        }));
        assertFalse(policy.matches("https://user@example.com/", () -> {
            throw new AssertionError("Malformed requests must not compute the server origin");
        }));
        assertTrue(policy.matches("http://my_service:8080", () -> {
            fallbackCalls.incrementAndGet();
            return "http://my_service:8080";
        }));
        assertEquals(1, fallbackCalls.get());
    }

    @Test
    void exactCompatibilityDoesNotWidenWildcardSyntaxOrMatching() {
        CorsOriginPolicy policy = CorsOriginPolicy.compile(List.of("https://*.example.com"), "test");
        for (String origin : List.of("https://my_service.example.com", "https://app.example.com.",
                "capacitor://app.example.com", "https://user@app.example.com", "https://app.example.com/path")) {
            assertFalse(policy.matches(origin), origin);
        }
        for (String pattern : List.of("capacitor://*.example.com", "https://*.my_service.com", "https://*.example.com.")) {
            List<String> patterns = List.of(pattern);
            assertThrows(ConfigException.class, () -> CorsOriginPolicy.compile(patterns, "test"));
        }
    }

    @Test
    void rejectsMalformedAuthorityAndPortsWithoutLeakingParserExceptions() {
        for (String origin : List.of("http://[127.0.0.1]", "http://[::1]extra", "http://[::1]:",
                "http://[fe80::1%eth0]", "http://::1", "http://host:9999999999999999999", "http://host:+80",
                "http://host:-0", "http://host:-000", "http://host:80/path", "http://a..b", "http://host\\path", "http://host?x", "http://host#x")) {
            List<String> origins = List.of(origin);
            assertThrows(ConfigException.class, () -> CorsOriginPolicy.compile(origins, "test"), origin);
            assertEquals(origin, CorsUtil.sanitizeDefaultPort(origin));
        }
        assertEquals("HTTP://my_service", CorsUtil.sanitizeDefaultPort("HTTP://my_service:080"));
        assertEquals("capacitor://localhost:80", CorsUtil.sanitizeDefaultPort("capacitor://localhost:80"));
    }
}
