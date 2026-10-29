package com.networknt.handler;

import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
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
        RequestInterceptor interceptor = new RequestInterceptor() {
            @Override public boolean isRequiredContent() { return true; }
            @Override public boolean isEnabled() { return true; }
            @Override public HttpHandler getNext() { return null; }
            @Override public MiddlewareHandler setNext(HttpHandler next) { return this; }
            @Override public void handleRequest(io.undertow.server.HttpServerExchange exchange) { }
        };
        SingletonServiceFactory.setBean(RequestInterceptor.class.getName(), new RequestInterceptor[]{interceptor});
        Config.getInstance().putInConfigCache(config, Map.of("enabled", true, "maxBuffers", 4,
                "maxBodyBytes", 1024, "appliedBodyInjectionPathPrefixes", List.of("/")));
        RequestInterceptorInjectionHandler reader = new RequestInterceptorInjectionHandler(config);
        AtomicBoolean onIo = new AtomicBoolean();
        reader.setNext(exchange -> {
            onIo.set(exchange.isInIoThread());
            throw new IOException("downstream");
        });
        CountDownLatch suspended = new CountDownLatch(1);
        HandlerContinuation.Boundary boundary = (exchange, task) -> {
            try { task.handleRequest(exchange); }
            catch (IOException e) { exchange.setStatusCode(409).getResponseSender().send("selected boundary"); }
        };
        Undertow server = Undertow.builder().addHttpListener(0, "127.0.0.1").setIoThreads(1)
                .setHandler(exchange -> {
                    if (enabled) HandlerContinuation.execute(exchange, boundary, reader);
                    else reader.handleRequest(exchange);
                    suspended.countDown();
                }).build();
        try {
            server.start();
            int port = ((InetSocketAddress) server.getListenerInfo().getFirst().getAddress()).getPort();
            try (Socket socket = new Socket("127.0.0.1", port)) {
                socket.setSoTimeout(5000);
                socket.getOutputStream().write(("POST / HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/json\r\n"
                        + "Transfer-Encoding: chunked\r\nConnection: close\r\n\r\n1\r\n{\r\n").getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();
                assertTrue(suspended.await(5, TimeUnit.SECONDS));
                socket.getOutputStream().write("1\r\n}\r\n0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();
                String response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(response.startsWith(enabled ? "HTTP/1.1 409" : "HTTP/1.1 500"), response);
                assertEquals(enabled, response.contains("selected boundary"), response);
                assertTrue(onIo.get(), "Exercise the actual channel-listener continuation");
            }
        } finally {
            server.stop();
            SingletonServiceFactory.setBean(RequestInterceptor.class.getName(), previous);
            Config.getInstance().clearConfigCache(config);
        }
    }
}
