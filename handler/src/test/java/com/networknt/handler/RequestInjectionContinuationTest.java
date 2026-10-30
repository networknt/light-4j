package com.networknt.handler;

import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HandlerWrapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(20)
class RequestInjectionContinuationTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void partialBodyRetainsBoundaryOrItsAbsence(boolean enabled) throws Exception {
        String config = "request-continuation-module-test";
        RequestInterceptor[] previous = SingletonServiceFactory.getBeans(RequestInterceptor.class);
        var interceptorMdc = new java.util.concurrent.atomic.AtomicReference<String>();
        var downstreamMdc = new java.util.concurrent.atomic.AtomicReference<String>();
        RequestInterceptor interceptor = new RequestInterceptor() {
            @Override public boolean isRequiredContent() { return true; }
            @Override public boolean isEnabled() { return true; }
            @Override public HttpHandler getNext() { return null; }
            @Override public MiddlewareHandler setNext(HttpHandler next) { return this; }
            @Override public void handleRequest(io.undertow.server.HttpServerExchange exchange) {
                interceptorMdc.set(org.slf4j.MDC.get("request"));
            }
        };
        SingletonServiceFactory.setBean(RequestInterceptor.class.getName(), new RequestInterceptor[]{interceptor});
        Config.getInstance().putInConfigCache(config, Map.of("enabled", true, "maxBuffers", 4,
                "maxBodyBytes", 1024, "appliedBodyInjectionPathPrefixes", List.of("/")));
        RequestInterceptorInjectionHandler reader = new RequestInterceptorInjectionHandler(config);
        AtomicBoolean onIo = new AtomicBoolean();
        reader.setNext(exchange -> {
            onIo.set(exchange.isInIoThread());
            downstreamMdc.set(org.slf4j.MDC.get("request"));
            throw new IOException("downstream");
        });
        CountDownLatch suspended = new CountDownLatch(1);
        HandlerWrapper boundary = task -> exchange -> {
            try { task.handleRequest(exchange); }
            catch (IOException _) { exchange.setStatusCode(409).getResponseSender().send("selected boundary"); }
        };
        Undertow server = Undertow.builder().addHttpListener(0, "127.0.0.1").setIoThreads(1)
                .setHandler(exchange -> {
                    if (exchange.getRequestPath().equals("/probe")) {
                        exchange.getResponseSender().send(org.slf4j.MDC.get("worker") + ":" + org.slf4j.MDC.get("request"));
                        org.slf4j.MDC.clear();
                        return;
                    }
                    exchange.putAttachment(com.networknt.httpstring.AttachmentConstants.MDC_CONTEXT,
                            Map.of("request", "canonical"));
                    org.slf4j.MDC.put("worker", "prior-io");
                    if (enabled) HandlerContinuation.execute(exchange, boundary, reader);
                    else reader.handleRequest(exchange);
                    suspended.countDown();
                }).build();
        try {
            server.start();
            int port = ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort();
            try (Socket socket = new Socket("127.0.0.1", port)) {
                socket.setSoTimeout(5000);
                socket.getOutputStream().write(("""
                        POST / HTTP/1.1\r
                        Host: localhost\r
                        Content-Type: application/json\r
                        Transfer-Encoding: chunked\r
                        Connection: close\r
                        \r
                        1\r
                        {\r
                        """).getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();
                assertTrue(suspended.await(5, TimeUnit.SECONDS));
                socket.getOutputStream().write("1\r\n}\r\n0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();
                String response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(response.startsWith(enabled ? "HTTP/1.1 409" : "HTTP/1.1 500"), response);
                assertEquals(enabled, response.contains("selected boundary"), response);
                assertTrue(onIo.get(), "Exercise the actual channel-listener continuation");
                assertEquals("canonical", interceptorMdc.get());
                assertEquals("canonical", downstreamMdc.get());
                try (var client = java.net.http.HttpClient.newHttpClient()) {
                    var probe = client.send(java.net.http.HttpRequest.newBuilder(
                            java.net.URI.create("http://127.0.0.1:" + port + "/probe")).build(),
                            java.net.http.HttpResponse.BodyHandlers.ofString());
                    assertEquals("prior-io:null", probe.body(), "Restore the I/O thread's context after the callback");
                }
            }
        } finally {
            server.stop();
            SingletonServiceFactory.setBean(RequestInterceptor.class.getName(), previous);
            Config.getInstance().clearConfigCache(config);
        }
    }
}
