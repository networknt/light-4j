package com.networknt.cors;

import com.networknt.config.Config;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.utility.Constants;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorsLegacyMatcherTest {
    @Test
    void malformedLegacyEntriesDoNotThrowAndDeniedRequestsStillAudit() throws Exception {
        String name = "cors-legacy-match";
        Config.getInstance().putInConfigCache(name, Map.of("allowedMethods", List.of("GET")));
        try {
            CorsHttpHandler handler = new CorsHttpHandler(name);
            List<String> origins = Arrays.asList("not an origin", null, "https://trusted.example.com");
            HttpServerExchange denied = exchange("https://evil.net", "localhost", "http");
            assertNull(CorsHttpHandler.matchOrigin(denied, origins, handler));
            assertEquals(403, denied.getStatusCode());
            assertNotNull(denied.getAttachment(AttachmentConstants.AUDIT_INFO).get(Constants.STATUS));
            assertEquals("https://trusted.example.com", CorsHttpHandler.matchOrigin(
                    exchange("https://trusted.example.com", "localhost", "http"), origins, handler));
        } finally {
            Config.getInstance().clearConfigCache(name);
        }
    }

    @Test
    void legacyMatcherReusesCompiledRulesAndNoticesCollectionChanges() throws Exception {
        String name = "cors-legacy-cache";
        Config.getInstance().putInConfigCache(name, Map.of("allowedMethods", List.of("GET")));
        try {
            CorsHttpHandler handler = new CorsHttpHandler(name);
            List<String> origins = new ArrayList<>(List.of("https://*.example.com"));
            try (MockedStatic<CorsOriginPolicy> compiler = mockStatic(CorsOriginPolicy.class, CALLS_REAL_METHODS)) {
                for (int i = 0; i < 3; i++) {
                    assertEquals("https://app.example.com", CorsHttpHandler.matchOrigin(
                            exchange("https://app.example.com", "localhost", "http"), origins, handler));
                }
                compiler.verify(() -> CorsOriginPolicy.compileLegacy(anyCollection()), times(1));
                origins.add("https://example.org");
                assertEquals("https://example.org", CorsHttpHandler.matchOrigin(
                        exchange("https://example.org", "localhost", "http"), origins, handler));
                compiler.verify(() -> CorsOriginPolicy.compileLegacy(anyCollection()), times(2));
            }
        } finally {
            Config.getInstance().clearConfigCache(name);
        }
    }

    @Test
    void serverFallbackPreservesInternalHostsAndTrailingDots() throws Exception {
        for (String host : List.of("my_service:8080", "example.com.:8080")) {
            String origin = "http://" + host;
            assertEquals(origin, CorsHttpHandler.matchOrigin(exchange(origin, host, "http"), List.of(), null));
        }
    }

    private static HttpServerExchange exchange(String origin, String host, String scheme) {
        HeaderMap headers = new HeaderMap();
        headers.put(Headers.HOST, host);
        headers.put(Headers.ORIGIN, origin);
        return new HttpServerExchange(null, headers, new HeaderMap(), -1)
                .setRequestScheme(scheme).setRequestMethod(Methods.GET).setRequestPath("/");
    }
}
