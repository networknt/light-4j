package com.networknt.mcp;

import com.networknt.handler.HandlerContinuation;
import io.undertow.Undertow;
import io.undertow.server.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class McpContinuationTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void redispatchRetainsSelectedBoundary(boolean enabled) throws Exception {
        McpHandler handler = new McpHandler() {
            @Override public void handleRequest(HttpServerExchange exchange) throws Exception {
                if (!exchange.isInIoThread()) throw new IllegalStateException("resumed failure");
                super.handleRequest(exchange);
            }
        };
        HttpHandler root = enabled ? exchange -> HandlerContinuation.execute(exchange, (ex, task) -> {
            try { task.handleRequest(ex); }
            catch (IllegalStateException _) { ex.setStatusCode(409).getResponseSender().send("selected"); }
        }, handler) : handler;
        Undertow server = Undertow.builder().addHttpListener(0, "127.0.0.1").setHandler(root).build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            server.start();
            int port = ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort();
            var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/mcp"))
                    .timeout(Duration.ofSeconds(5)).POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(enabled ? 409 : 500, response.statusCode(), response.body());
            assertEquals(enabled, response.body().contains("selected"));
        } finally { server.stop(); }
    }
}
