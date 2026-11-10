package com.networknt.cors;

import com.networknt.config.Config;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.util.Headers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in real-browser check; see cors/README.md for local prerequisites. */
@EnabledIfSystemProperty(named = "cors.browserSmoke", matches = "true")
class CorsBrowserSmokeTest {
    @Test
    void credentialedRequestsUseScopedWildcardsInChromium() throws Exception {
        String name = "browser-smoke-cors";
        AtomicReference<HttpHandler> handler = new AtomicReference<>();
        Undertow server = Undertow.builder().addHttpListener(0, "127.0.0.1").setIoThreads(2)
                .setHandler(exchange -> handler.get().handleRequest(exchange)).build();
        Path output = Files.createTempFile("cors-browser-smoke-", ".log");
        Process process = null;
        try {
            server.start();
            int port = ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort();
            Config.getInstance().putInConfigCache(name, Map.of("allowedOrigins", List.of("http://*.example.com:" + port),
                    "allowedMethods", List.of("GET", "PUT")));
            CorsHttpHandler cors = new CorsHttpHandler(name);
            cors.setNext(exchange -> {
                if (exchange.getRequestPath().equals("/browser")) {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
                    exchange.getResponseSender().send("<!doctype html><title>CORS smoke test</title>");
                } else {
                    String cookie = exchange.getRequestHeaders().getFirst(Headers.COOKIE);
                    boolean authenticated = cookie != null && cookie.contains("cors-smoke=present");
                    exchange.setStatusCode(authenticated ? 200 : 401);
                    exchange.getResponseSender().send(authenticated ? "credential-ok" : "missing-credential");
                }
            });
            handler.set(cors);
            process = new ProcessBuilder("node", "src/test/browser/credentialed-cors.cjs", Integer.toString(port))
                    .redirectErrorStream(true).redirectOutput(output.toFile()).start();
            assertTrue(process.waitFor(45, TimeUnit.SECONDS), "Browser smoke test timed out: " + output);
            assertEquals(0, process.exitValue(), () -> {
                try { return Files.readString(output); }
                catch (Exception e) { return output.toString(); }
            });
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            server.stop();
            Config.getInstance().clearConfigCache(name);
            Files.deleteIfExists(output);
        }
    }
}
