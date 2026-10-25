package com.networknt.proxy;

import com.networknt.config.Config;
import com.networknt.exception.ExceptionHandler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.RequestInterceptor;
import com.networknt.handler.RequestInterceptorInjectionHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.Undertow;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class ExternalServiceWorkerDispatchTest {
    private static final String EXTERNAL_CONFIG = "external-service-worker-test";
    private static final String INJECTION_CONFIG = "external-worker-request-injection-test";
    private static final String TLS_PROPERTY = "jdk.internal.httpclient.disableHostnameVerification";
    private RequestInterceptor[] previousInterceptors;
    private String previousTlsProperty;

    @BeforeEach
    void rememberSharedState() {
        previousInterceptors = SingletonServiceFactory.getBeans(RequestInterceptor.class);
        previousTlsProperty = System.getProperty(TLS_PROPERTY);
    }

    @AfterEach
    void restoreSharedState() {
        SingletonServiceFactory.setBean(RequestInterceptor.class.getName(), previousInterceptors);
        Config.getInstance().clearConfigCache(EXTERNAL_CONFIG);
        Config.getInstance().clearConfigCache(INJECTION_CONFIG);
        if (previousTlsProperty == null) System.clearProperty(TLS_PROPERTY);
        else System.setProperty(TLS_PROPERTY, previousTlsProperty);
    }

    @Test
    void streamedBodyContinuationCannotBlockTheFrontEndIoThread() throws Exception {
        BodyCaptureInterceptor interceptor = new BodyCaptureInterceptor();
        SingletonServiceFactory.setBean(RequestInterceptor.class.getName(),
                new RequestInterceptor[]{interceptor});
        Config.getInstance().putInConfigCache(INJECTION_CONFIG, Map.of(
                "enabled", true, "maxBuffers", 4, "maxBodyBytes", 1024,
                "appliedBodyInjectionPathPrefixes", List.of("/proxy")));

        CountDownLatch initialReadReturned = new CountDownLatch(1);
        try (ProxyFixture fixture = new ProxyFixture(injection -> {
            RequestInterceptorInjectionHandler reader =
                    new RequestInterceptorInjectionHandler(INJECTION_CONFIG);
            reader.setNext(injection);
            return new ExceptionHandler().setNext(exchange -> {
                reader.handleRequest(exchange);
                initialReadReturned.countDown();
            });
        }); Socket socket = new Socket("localhost", fixture.port())) {
            socket.setSoTimeout(5000);
            String body = "{\"message\":\"streamed body\"}";
            String first = body.substring(0, 5);
            String remaining = body.substring(5);
            write(socket, "POST /proxy/resource HTTP/1.1\r\nHost: localhost\r\n"
                    + "Content-Type: application/json\r\nTransfer-Encoding: chunked\r\n"
                    + "Connection: close\r\n\r\n"
                    + Integer.toHexString(first.length()) + "\r\n" + first + "\r\n");
            assertTrue(initialReadReturned.await(5, TimeUnit.SECONDS),
                    "The partial body must install its asynchronous read continuation");
            assertFalse(interceptor.called.get(), "The incomplete body must not reach the interceptor");
            write(socket, Integer.toHexString(remaining.length()) + "\r\n"
                    + remaining + "\r\n0\r\n\r\n");
            assertTrue(fixture.upstreamEntered.await(5, TimeUnit.SECONDS),
                    "The completed request must reach the upstream");
            assertTrue(interceptor.onIoThread.get(),
                    "The test must exercise actual I/O-thread body-read continuation");
            fixture.assertProbeCompletesBeforeUpstreamRelease();
            fixture.releaseUpstream.countDown();
            String response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(response.startsWith("HTTP/1.1 200"), response);
            assertTrue(response.endsWith("accepted"), response);
            assertEquals(1, fixture.handlerEntries.get(), "Resolve configuration and routing only once");
            assertEquals(body, fixture.body.get());
            assertEquals("/backend/resource", fixture.path.get(), "Rewrite the path exactly once");
        }
    }

    @Test
    void directGetCannotBlockTheFrontEndIoThread() throws Exception {
        try (ProxyFixture fixture = new ProxyFixture(handler -> handler);
             HttpClient client = HttpClient.newHttpClient()) {
            var response = client.sendAsync(HttpRequest.newBuilder(fixture.uri("/proxy/get"))
                            .timeout(Duration.ofSeconds(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            try {
                assertTrue(fixture.upstreamEntered.await(5, TimeUnit.SECONDS));
                fixture.assertProbeCompletesBeforeUpstreamRelease();
                fixture.releaseUpstream.countDown();
                HttpResponse<String> completed = response.get(5, TimeUnit.SECONDS);
                assertEquals(200, completed.statusCode());
                assertEquals("accepted", completed.body());
                assertEquals("/backend/get", fixture.path.get());
                assertEquals(1, fixture.handlerEntries.get(), "Dispatch forwarding without re-entering routing");
            } finally {
                fixture.releaseUpstream.countDown();
            }
        }
    }

    @Test
    void unmatchedRequestRetainsItsOriginalIoThread() throws Exception {
        AtomicBoolean nextOnIoThread = new AtomicBoolean();
        try (ProxyFixture fixture = new ProxyFixture(handler -> {
            handler.setNext(exchange -> {
                nextOnIoThread.set(exchange.isInIoThread());
                exchange.getResponseSender().send("unmatched");
            });
            return handler;
        }); HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(fixture.uri("/local")).timeout(Duration.ofSeconds(5)).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("unmatched", response.body());
            assertTrue(nextOnIoThread.get(), "Unmatched paths must not gain an unnecessary dispatch");
            assertEquals(1, fixture.upstreamEntered.getCount(), "Do not contact the upstream");
        }
    }

    private static void write(Socket socket, String text) throws Exception {
        socket.getOutputStream().write(text.getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().flush();
    }

    private interface Chain {
        HttpHandler wrap(ExternalServiceHandler handler);
    }

    private static final class ProxyFixture implements AutoCloseable {
        final AtomicInteger handlerEntries = new AtomicInteger();
        final CountDownLatch upstreamEntered = new CountDownLatch(1);
        final CountDownLatch releaseUpstream = new CountDownLatch(1);
        final AtomicReference<String> body = new AtomicReference<>();
        final AtomicReference<String> path = new AtomicReference<>();
        private final Undertow upstream;
        private final Undertow front;

        ProxyFixture(Chain chain) {
            upstream = Undertow.builder().addHttpListener(0, "localhost").setIoThreads(1)
                    .setHandler(new BlockingHandler(exchange -> {
                        path.set(exchange.getRequestPath());
                        body.set(new String(exchange.getInputStream().readAllBytes(),
                                StandardCharsets.UTF_8));
                        upstreamEntered.countDown();
                        assertTrue(releaseUpstream.await(15, TimeUnit.SECONDS),
                                "The test must release the blocked upstream");
                        exchange.getResponseSender().send("accepted");
                    })).build();
            upstream.start();
            Config.getInstance().putInConfigCache(EXTERNAL_CONFIG, Map.of(
                    "enabled", true, "timeout", 20000, "connectTimeout", 5000,
                    "maxConnectionRetries", 1, "metricsInjection", false,
                    "pathHostMappings", List.of("/proxy http://localhost:" + port(upstream)),
                    "urlRewriteRules", List.of("/proxy(.*) /backend$1")));
            ExternalServiceHandler external = new ExternalServiceHandler(EXTERNAL_CONFIG) {
                @Override
                public void handleRequest(HttpServerExchange exchange) throws Exception {
                    handlerEntries.incrementAndGet();
                    super.handleRequest(exchange);
                }
            };
            external.setNext(exchange -> exchange.setStatusCode(404).endExchange());
            HttpHandler proxyChain = chain.wrap(external);
            front = Undertow.builder().addHttpListener(0, "localhost").setIoThreads(1)
                    .setHandler(exchange -> {
                        if (exchange.getRequestPath().equals("/probe"))
                            exchange.getResponseSender().send("probe");
                        else proxyChain.handleRequest(exchange);
                    }).build();
            front.start();
        }

        int port() { return port(front); }
        URI uri(String path) { return URI.create("http://localhost:" + port() + path); }

        void assertProbeCompletesBeforeUpstreamRelease() throws Exception {
            try (HttpClient client = HttpClient.newHttpClient()) {
                HttpResponse<String> response = client.sendAsync(
                                HttpRequest.newBuilder(uri("/probe"))
                                        .timeout(Duration.ofSeconds(3)).build(),
                                HttpResponse.BodyHandlers.ofString())
                        .get(4, TimeUnit.SECONDS);
                assertEquals(200, response.statusCode());
                assertEquals("probe", response.body());
                assertEquals(1, releaseUpstream.getCount(),
                        "The unrelated request must finish while the upstream is still blocked");
            }
        }

        @Override
        public void close() {
            releaseUpstream.countDown();
            front.stop();
            upstream.stop();
        }

        private static int port(Undertow server) {
            return ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();
        }
    }

    private static final class BodyCaptureInterceptor implements RequestInterceptor {
        final AtomicBoolean called = new AtomicBoolean();
        final AtomicBoolean onIoThread = new AtomicBoolean();
        private HttpHandler next;

        @Override public boolean isRequiredContent() { return true; }
        @Override public boolean isEnabled() { return true; }
        @Override public HttpHandler getNext() { return next; }
        @Override public MiddlewareHandler setNext(HttpHandler next) { this.next = next; return this; }

        @Override
        public void handleRequest(HttpServerExchange exchange) {
            onIoThread.set(exchange.isInIoThread());
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            for (PooledByteBuffer pooled :
                    exchange.getAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY)) {
                if (pooled != null) {
                    ByteBuffer buffer = pooled.getBuffer().duplicate();
                    byte[] chunk = new byte[buffer.remaining()];
                    buffer.get(chunk);
                    bytes.writeBytes(chunk);
                }
            }
            exchange.putAttachment(AttachmentConstants.REQUEST_BODY_STRING,
                    bytes.toString(StandardCharsets.UTF_8));
            called.set(true);
        }
    }
}
