package com.networknt.cors;

import com.google.common.net.InternetDomainName;
import com.networknt.config.Config;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorsPublicMatcherContractTest {
    @Test
    void rawCollectionsRemainExactOnlyAndReturnConfiguredEntry() throws Exception {
        String name = "cors-public-contract";
        Config.getInstance().putInConfigCache(name, Map.of("enabled", true, "allowedMethods", List.of("GET")));
        try {
            CorsHttpHandler handler = new CorsHttpHandler(name);
            assertNull(CorsHttpHandler.matchOrigin(exchange("https://app.example.com"), List.of("https://*.example.com"), handler));
            assertEquals("https://APP.Example.com", CorsHttpHandler.matchOrigin(exchange("https://app.example.com:443"),
                    List.of("https://APP.Example.com"), handler));
            // The compatibility helper normalizes the request port, not a caller's configured string.
            assertNull(CorsHttpHandler.matchOrigin(exchange("https://app.example.com"), List.of("https://app.example.com:443"), handler));
        } finally {
            Config.getInstance().clearConfigCache(name);
        }
    }

    @Test
    void alternatingCollectionsAndNullHandlersDoNotUsePsl() throws Exception {
        try (MockedStatic<InternetDomainName> psl = mockStatic(InternetDomainName.class)) {
            for (int i = 0; i < 4; i++) {
                String configured = i % 2 == 0 ? "https://FIRST.example.com" : "https://SECOND.example.com";
                List<String> origins = List.of("https://*.example.com", "invalid origin", configured);
                assertEquals(configured, CorsHttpHandler.matchOrigin(exchange(configured.toLowerCase(java.util.Locale.ROOT)), origins, null));
            }
            psl.verifyNoInteractions();
        }
    }

    private static HttpServerExchange exchange(String origin) {
        HeaderMap request = new HeaderMap();
        request.put(Headers.HOST, "localhost");
        request.put(Headers.ORIGIN, origin);
        return new HttpServerExchange(null, request, new HeaderMap(), -1)
                .setRequestScheme("http").setRequestMethod(Methods.GET).setRequestPath("/");
    }
}
