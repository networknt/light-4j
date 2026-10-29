package com.networknt.handler;

import io.undertow.Undertow;
import io.undertow.server.*;
import io.undertow.server.handlers.proxy.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class ProxyContinuationTest {
    @ParameterizedTest
    @CsvSource({"true,false", "false,false", "true,true", "false,true"})
    void connectionDispatchRetainsBoundary(boolean enabled, boolean delayedCompletion) throws Exception {
        var completion = new java.util.concurrent.CompletableFuture<Runnable>();
        ProxyClient client = new ProxyClient() {
            @Override public ProxyTarget findTarget(HttpServerExchange exchange) { return new ProxyTarget() {}; }
            @Override public void getConnection(ProxyTarget target, HttpServerExchange exchange,
                    ProxyCallback<ProxyConnection> callback, long timeout, TimeUnit unit) {
                if (delayedCompletion) completion.complete(() -> callback.completed(exchange, new ProxyConnection(null, "/")));
                else throw new IllegalStateException("connection callback failed");
            }
        };
        ProxyHandler proxy = ProxyHandler.builder().setProxyClient(client).build();
        HttpHandler root = enabled ? exchange -> HandlerContinuation.execute(exchange, (ex, task) -> {
            try { task.handleRequest(ex); }
            catch (RuntimeException e) { ex.setStatusCode(409).getResponseSender().send("selected"); }
        }, proxy) : proxy;
        Undertow server = Undertow.builder().addHttpListener(0, "127.0.0.1").setHandler(root).build();
        try (HttpClient http = HttpClient.newHttpClient()) {
            server.start();
            int port = ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort();
            var pending = http.sendAsync(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/"))
                    .timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
            if (delayedCompletion) {
                assertFalse(pending.isDone(), "Connection acquisition must not end a pending exchange");
                completion.get(5, TimeUnit.SECONDS).run();
            }
            var response = pending.get(5, TimeUnit.SECONDS);
            assertEquals(enabled ? 409 : 500, response.statusCode(), response.body());
            assertEquals(enabled, response.body().contains("selected"));
        } finally { server.stop(); }
    }
}
