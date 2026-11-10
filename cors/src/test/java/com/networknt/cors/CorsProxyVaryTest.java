package com.networknt.cors;

import com.networknt.config.Config;
import com.networknt.handler.ProxyHandler;
import io.undertow.Undertow;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.util.Headers;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CorsProxyVaryTest {
    @Test
    void preservesBackendVaryForCorsAndNonCorsProxyResponses() throws Exception {
        String name = "cors-proxy-vary";
        Config.getInstance().putInConfigCache(name, Map.of("allowedOrigins", List.of("https://*.example.com"),
                "allowedMethods", List.of("GET")));
        Undertow backend = Undertow.builder().addHttpListener(0, "127.0.0.1").setIoThreads(1).setHandler(exchange -> {
            if (exchange.getRequestPath().equals("/star")) {
                exchange.getResponseHeaders().put(Headers.VARY, "*");
            } else {
                exchange.getResponseHeaders().put(Headers.VARY, "Accept-Encoding, Authorization");
                exchange.getResponseHeaders().add(Headers.VARY, "Accept-Language");
                if (exchange.getRequestPath().equals("/origin")) exchange.getResponseHeaders().add(Headers.VARY, "origin");
            }
            exchange.getResponseSender().send("backend");
        }).build();
        Undertow frontend = null;
        try (HttpClient client = HttpClient.newHttpClient()) {
            backend.start();
            LoadBalancingProxyClient upstream = new LoadBalancingProxyClient().addHost(base(backend));
            ProxyHandler proxy = ProxyHandler.builder().setProxyClient(upstream).setMaxConnectionRetries(0).build();
            CorsHttpHandler cors = new CorsHttpHandler(name);
            cors.setNext(proxy);
            frontend = Undertow.builder().addHttpListener(0, "127.0.0.1").setIoThreads(1).setHandler(cors).build();
            frontend.start();
            for (String path : List.of("/vary", "/origin", "/star")) {
                for (boolean withOrigin : List.of(false, true)) {
                    HttpRequest.Builder request = HttpRequest.newBuilder(base(frontend).resolve(path));
                    if (withOrigin) request.header("Origin", "https://app.example.com");
                    HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
                    assertEquals(200, response.statusCode());
                    assertEquals("backend", response.body());
                    List<String> vary = response.headers().allValues("Vary").stream()
                            .flatMap(value -> Arrays.stream(value.split(",")))
                            .map(value -> value.trim().toLowerCase(Locale.ROOT)).toList();
                    if (path.equals("/star")) {
                        assertEquals(List.of("*"), vary);
                    } else {
                        assertTrue(vary.containsAll(List.of("accept-encoding", "authorization", "accept-language")), vary.toString());
                        assertEquals(1, vary.stream().filter("origin"::equals).count());
                    }
                }
            }
        } finally {
            if (frontend != null) frontend.stop();
            backend.stop();
            Config.getInstance().clearConfigCache(name);
        }
    }

    private static URI base(Undertow server) {
        int port = ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort();
        return URI.create("http://127.0.0.1:" + port);
    }
}
