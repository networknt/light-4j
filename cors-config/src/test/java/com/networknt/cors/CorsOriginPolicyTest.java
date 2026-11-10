package com.networknt.cors;

import com.networknt.config.ConfigException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorsOriginPolicyTest {
    private final CorsOriginPolicy policy = CorsOriginPolicy.compile(List.of("https://*.example.com"), "test");

    @ParameterizedTest
    @ValueSource(strings = {"https://app.example.com", "https://team.app.example.com",
            "HTTPS://APP.EXAMPLE.COM", "https://app.example.com:443"})
    void matchesSubdomains(String origin) {
        assertTrue(policy.matches(origin));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com", "https://badexample.com", "https://example.com.attacker.net",
            "http://app.example.com", "https://app.example.com:80", "https://app.example.com:8443", "null",
            "https://app.example.com.", "https://app.example.com/", "https://app.example.com?q=1",
            "https://app.example.com#fragment", "https://user@app.example.com", "https://app.example.com:",
            "https://app.example.com:65536", "https://app.example.com:-1", "https://app.example.com:foo",
            "https://app.example.com,https://evil.net", "https://app.example.com https://evil.net",
            "https://a..example.com", "https://-a.example.com", "https://a_.example.com",
            "https://é.example.com", "https://a%2eexample.com", "", "https://app.example.com\r\nX: y"})
    void rejectsUntrustedOrMalformedOrigins(String origin) {
        assertFalse(policy.matches(origin));
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "*.example.com", "https://app*.example.com", "https://example.*.com",
            "https://*.*.example.com", "https://*.com", "https://*.net", "https://*.co.uk",
            "https://*.github.io", "https://*.blogspot.com", "https://*.foo.kawasaki.jp", "https://*.xn--55qx5d.cn",
            "https://*.example.internal", "https://*.127.0.0.1", "https://*.[::1]", "ftp://*.example.com",
            "https://*.example.com/", "https://*.example.com.", "https://*.example.com:*"})
    void rejectsInvalidConfiguration(String pattern) {
        ConfigException error = assertThrows(ConfigException.class,
                () -> CorsOriginPolicy.compile(List.of(pattern), "pathPrefixAllowed[/api].allowedOrigins"));
        assertTrue(error.getMessage().contains("pathPrefixAllowed[/api].allowedOrigins"));
        assertTrue(error.getMessage().contains(pattern));
    }

    @Test
    void supportsPslExceptionsAndTenantOwnedSuffixes() {
        assertTrue(CorsOriginPolicy.compile(List.of("https://*.city.kawasaki.jp"), "test")
                .matches("https://app.city.kawasaki.jp"));
        assertTrue(CorsOriginPolicy.compile(List.of("https://*.myorg.github.io"), "test")
                .matches("https://app.myorg.github.io"));
    }

    @Test
    void supportsExplicitOriginsPortsAndPunycode() {
        CorsOriginPolicy explicit = CorsOriginPolicy.compile(List.of("https://EXAMPLE.com:443",
                "http://localhost:80", "https://xn--bcher-kva.de", "http://127.0.0.1:7080",
                "http://[::1]:7080", "https://*.example.org:8443"), "test");
        for (String origin : List.of("https://example.com", "http://localhost", "https://xn--bcher-kva.de",
                "http://127.0.0.1:7080", "http://[0:0:0:0:0:0:0:1]:7080", "https://a.example.org:8443")) {
            assertTrue(explicit.matches(origin), origin);
        }
        assertFalse(explicit.matches("https://a.example.org"));
        assertFalse(explicit.matches("http://[::1]"));
        assertTrue(CorsOriginPolicy.compile(List.of("https://*.xn--bcher-kva.de"), "test")
                .matches("https://shop.xn--bcher-kva.de"));
    }

    @Test
    void nullRequiresExplicitOptInAndEmptyListsDeny() {
        assertTrue(CorsOriginPolicy.compile(List.of("null"), "test").matches("null"));
        assertFalse(CorsOriginPolicy.compile(List.of("null"), "test").matches("NULL"));
        assertFalse(CorsOriginPolicy.compile(null, "test").matches("https://example.com"));
        assertFalse(CorsOriginPolicy.compile(List.of(), "test").matches("null"));
    }
}
