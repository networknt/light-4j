package com.networknt.proxy;

import com.networknt.config.Config;
import com.networknt.exception.ExceptionHandler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.Handler;
import com.networknt.metrics.AbstractMetricsHandler;
import com.networknt.metrics.MetricsConfig;
import com.networknt.metrics.TimeSeriesDbSender;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
import java.util.HashSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
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
    private HttpHandler previousMetrics;

    @BeforeEach
    void rememberSharedState() {
        previousInterceptors = SingletonServiceFactory.getBeans(RequestInterceptor.class);
        previousTlsProperty = System.getProperty(TLS_PROPERTY);
        previousMetrics = Handler.getHandlers().get(MetricsConfig.CONFIG_NAME);
    }

    @AfterEach
    void restoreSharedState() {
        SingletonServiceFactory.setBean(RequestInterceptor.class.getName(), previousInterceptors);
        if (previousMetrics == null) Handler.getHandlers().remove(MetricsConfig.CONFIG_NAME);
        else Handler.getHandlers().put(MetricsConfig.CONFIG_NAME, previousMetrics);
        Config.getInstance().clearConfigCache(EXTERNAL_CONFIG);
        Config.getInstance().clearConfigCache(INJECTION_CONFIG);
        if (previousTlsProperty == null) System.clearProperty(TLS_PROPERTY);
        else System.setProperty(TLS_PROPERTY, previousTlsProperty);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void streamedBodyContinuationCannotBlockTheFrontEndIoThread(boolean upstreamFailure) throws Exception {
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
            fixture.failUpstream.set(upstreamFailure);
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
            if (upstreamFailure) {
                assertTrue(response.startsWith("HTTP/1.1 400"), response);
                assertTrue(response.contains("application/json"), response);
                assertTrue(response.contains("ERR10011"), "Use framework structured errors: " + response);
            } else {
                assertTrue(response.startsWith("HTTP/1.1 200"), response);
                assertTrue(response.endsWith("accepted"), response);
            }
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

    @Test
    void workerThreadFailureUsesTheSameFrameworkError() throws Exception {
        try (ProxyFixture fixture = new ProxyFixture(handler -> new ExceptionHandler().setNext(handler));
             HttpClient client = HttpClient.newHttpClient()) {
            fixture.failUpstream.set(true);
            fixture.releaseUpstream.countDown();
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(fixture.uri("/proxy/failure"))
                            .timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(400, response.statusCode(), "Preserve the framework ERR10011 status");
            assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
            assertTrue(response.body().contains("ERR10011"), response.body());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "OPTIONS"})
    void forwardingAndMethodRejectionInjectMetrics(String method) throws Exception {
        RecordingMetrics metrics = new RecordingMetrics();
        Handler.getHandlers().put(MetricsConfig.CONFIG_NAME, metrics);
        try (ProxyFixture fixture = new ProxyFixture(handler -> handler, true);
             HttpClient client = HttpClient.newHttpClient()) {
            fixture.releaseUpstream.countDown();
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(fixture.uri("/proxy/metrics"))
                            .timeout(Duration.ofSeconds(5))
                            .method(method, HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.ofString());
            int expectedStatus = method.equals("GET") ? 200 : 405;
            assertEquals(expectedStatus, response.statusCode(), response.body());
            assertTrue(metrics.injected.await(5, TimeUnit.SECONDS), "Metrics callback must complete");
            assertEquals(1, metrics.calls.get());
            assertEquals(expectedStatus, metrics.status.get());
            assertEquals("/proxy@" + method.toLowerCase(), metrics.endpoint.get());
            assertEquals("external-worker-metrics", metrics.name.get());
            assertFalse(metrics.onIoThread.get(), "Inject metrics on the forwarding worker");
            if (method.equals("GET")) {
                assertEquals("/backend/metrics", fixture.path.get());
                assertEquals("accepted", response.body());
            } else {
                assertTrue(response.body().contains("ERR10008"), response.body());
                assertEquals(1, fixture.upstreamEntered.getCount(), "Reject before sending upstream");
            }
        }
    }

    @Test
    void concurrentInitializationSharesOneHttpClient() throws Exception {
        try (ProxyFixture fixture = new ProxyFixture(handler -> handler);
             var executor = Executors.newFixedThreadPool(8)) {
            ExternalServiceHandler handler = new ExternalServiceHandler(EXTERNAL_CONFIG);
            var getter = ExternalServiceHandler.class.getDeclaredMethod("getOrCreateClient",
                    ExternalServiceConfig.class, int.class);
            getter.setAccessible(true);
            CyclicBarrier start = new CyclicBarrier(8);
            var futures = IntStream.range(0, 8).mapToObj(i -> executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return (HttpClient) getter.invoke(handler, ExternalServiceConfig.load(EXTERNAL_CONFIG), 5000);
            })).toList();
            var clients = new HashSet<HttpClient>();
            try {
                for (var future : futures) clients.add(future.get(10, TimeUnit.SECONDS));
                assertEquals(1, clients.size(), "Concurrent first requests must share one safely published client");
            } finally {
                clients.forEach(HttpClient::close);
            }
        }
    }

    private static final class RecordingMetrics extends AbstractMetricsHandler {
        final CountDownLatch injected = new CountDownLatch(1);
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger status = new AtomicInteger();
        final AtomicReference<String> endpoint = new AtomicReference<>();
        final AtomicReference<String> name = new AtomicReference<>();
        final AtomicBoolean onIoThread = new AtomicBoolean();

        @Override
        public void injectMetrics(HttpServerExchange exchange, long startTime, String metricsName, String endpoint) {
            calls.incrementAndGet();
            status.set(exchange.getStatusCode());
            this.endpoint.set(endpoint);
            name.set(metricsName);
            onIoThread.set(exchange.isInIoThread());
            injected.countDown();
        }

        @Override protected void createMetricsReporter(TimeSeriesDbSender sender, MetricsConfig config) { }
        @Override public void handleRequest(HttpServerExchange exchange) { }
        @Override public HttpHandler getNext() { return null; }
        @Override public MiddlewareHandler setNext(HttpHandler next) { return this; }
    }

    private static void write(Socket socket, String text) throws Exception {
        socket.getOutputStream().write(text.getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().flush();
    }

    private interface Chain {
        HttpHandler wrap(ExternalServiceHandler handler);
    }

    private static final class ProxyFixture implements AutoCloseable {
        final AtomicBoolean failUpstream = new AtomicBoolean();
        final AtomicInteger handlerEntries = new AtomicInteger();
        final CountDownLatch upstreamEntered = new CountDownLatch(1);
        final CountDownLatch releaseUpstream = new CountDownLatch(1);
        final AtomicReference<String> body = new AtomicReference<>();
        final AtomicReference<String> path = new AtomicReference<>();
        private final Undertow upstream;
        private final Undertow front;

        ProxyFixture(Chain chain) {
            this(chain, false);
        }

        ProxyFixture(Chain chain, boolean metricsInjection) {
            upstream = Undertow.builder().addHttpListener(0, "localhost").setIoThreads(1)
                    .setHandler(new BlockingHandler(exchange -> {
                        path.set(exchange.getRequestPath());
                        body.set(new String(exchange.getInputStream().readAllBytes(),
                                StandardCharsets.UTF_8));
                        upstreamEntered.countDown();
                        assertTrue(releaseUpstream.await(15, TimeUnit.SECONDS),
                                "The test must release the blocked upstream");
                        if (failUpstream.get()) exchange.getConnection().close();
                        else exchange.getResponseSender().send("accepted");
                    })).build();
            upstream.start();
            Config.getInstance().putInConfigCache(EXTERNAL_CONFIG, Map.of(
                    "enabled", true, "timeout", 20000, "connectTimeout", 5000,
                    "maxConnectionRetries", 1, "metricsInjection", metricsInjection,
                    "metricsName", "external-worker-metrics",
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
