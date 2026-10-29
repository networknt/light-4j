package com.networknt.body;

import com.networknt.handler.HandlerContinuation;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class BodyContinuationTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void redispatchRetainsBoundary(boolean enabled) throws Exception {
        BodyHandler body = new BodyHandler();
        body.setNext(exchange -> {
            assertFalse(exchange.isInIoThread());
            throw new IllegalStateException("downstream failure");
        });
        HttpHandler root = enabled ? exchange -> HandlerContinuation.execute(exchange, (ex, task) -> {
            try { task.handleRequest(ex); }
            catch (IllegalStateException e) { ex.setStatusCode(409).getResponseSender().send("selected"); }
        }, body) : body;
        Undertow server = Undertow.builder().addHttpListener(0, "127.0.0.1").setHandler(root).build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            server.start();
            int port = ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort();
            var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/"))
                    .timeout(Duration.ofSeconds(5)).header("Content-Type", "text/plain")
                    .POST(HttpRequest.BodyPublishers.ofString("body")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(enabled ? 409 : 500, response.statusCode(), response.body());
            assertEquals(enabled, response.body().contains("selected"));
        } finally { server.stop(); }
    }
}
