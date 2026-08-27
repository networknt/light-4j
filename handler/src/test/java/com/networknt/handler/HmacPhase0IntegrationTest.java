package com.networknt.handler;

import com.networknt.httpstring.AttachmentConstants;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class HmacPhase0IntegrationTest {
    private static final String STRICT_CONFIG = "phase0-strict-request-injection";
    private static final String DEFAULT_HMAC_BOUNDARY_CONFIG = "phase0-default-hmac-boundary-request-injection";
    private static final String LEGACY_CONFIG = "phase0-legacy-request-injection";
    private static final AttachmentKey<byte[]> BODY_FINGERPRINT = AttachmentKey.create(byte[].class);

    private RequestInterceptor[] previousInterceptors;

    @AfterEach
    void restoreInterceptors() {
        SingletonServiceFactory.setBean(RequestInterceptor.class.getName(), previousInterceptors);
    }

    @Test
    void strictReaderPreservesExactBytesAndRejectsFirstBytePastFullBuffer() throws Exception {
        byte[] exactBody = body(1024);
        ExactBytesInterceptor first = new ExactBytesInterceptor();
        ExactBytesInterceptor second = new ExactBytesInterceptor();
        AtomicInteger downstreamCalls = new AtomicInteger();

        try (RunningServer server = start(STRICT_CONFIG, new RequestInterceptor[]{first, second}, exchange -> {
            downstreamCalls.incrementAndGet();
            exchange.getResponseSender().send("accepted");
        })) {
            HttpResponse<byte[]> accepted = server.postChunked(exactBody);
            Assertions.assertEquals(200, accepted.statusCode());
            Assertions.assertArrayEquals(exactBody, first.observed.get());
            Assertions.assertArrayEquals(exactBody, second.observed.get());
            Assertions.assertTrue(first.positionsPreserved.get());
            Assertions.assertTrue(second.positionsPreserved.get());
            Assertions.assertEquals(1, downstreamCalls.get());

            HttpResponse<byte[]> rejected = server.postChunked(body(1025));
            Assertions.assertEquals(413, rejected.statusCode());
            Assertions.assertEquals(1, downstreamCalls.get());
        }
    }

    @Test
    void acceptsExactlyDefault16MiBLimitAndRejectsTheFirstExcessByte() throws Exception {
        int defaultLimit = 16 * 1024 * 1024;
        AtomicInteger downstreamCalls = new AtomicInteger();
        try (RunningServer server = start(DEFAULT_HMAC_BOUNDARY_CONFIG,
                new RequestInterceptor[]{new ExactBytesInterceptor()}, exchange -> {
                    downstreamCalls.incrementAndGet();
                    exchange.getResponseSender().send("accepted");
                })) {
            Assertions.assertEquals(200, server.postChunked(body(defaultLimit)).statusCode());
            Assertions.assertEquals(1, downstreamCalls.get());
            Assertions.assertEquals(413, server.postChunked(body(defaultLimit + 1)).statusCode());
            Assertions.assertEquals(1, downstreamCalls.get());
        }
    }

    @Test
    void zeroLimitPreservesLegacyFullBufferContinuation() throws Exception {
        AtomicInteger downstreamCalls = new AtomicInteger();
        try (RunningServer server = start(LEGACY_CONFIG, new RequestInterceptor[]{new ExactBytesInterceptor()}, exchange -> {
            downstreamCalls.incrementAndGet();
            exchange.getResponseSender().send("legacy");
        })) {
            HttpResponse<byte[]> response = server.postChunked(body(1025));
            Assertions.assertEquals(200, response.statusCode());
            Assertions.assertEquals(1, downstreamCalls.get());
        }
    }

    @Test
    void replayProofAwaitsAsynchronouslyAndCompletionObservesFailureStatus() throws Exception {
        CompletableFuture<Void> reservation = new CompletableFuture<>();
        CountDownLatch reservationStarted = new CountDownLatch(1);
        CountDownLatch completionObserved = new CountDownLatch(1);
        AtomicInteger releases = new AtomicInteger();
        AtomicInteger observedStatus = new AtomicInteger();
        AtomicBoolean enteredOnIoThread = new AtomicBoolean();
        AtomicBoolean resumedOnIoThread = new AtomicBoolean(true);
        AtomicInteger downstreamCalls = new AtomicInteger();

        Phase0ReplayHandler replay = new Phase0ReplayHandler(
                reservation,
                reservationStarted,
                completionObserved,
                releases,
                observedStatus,
                enteredOnIoThread,
                resumedOnIoThread,
                exchange -> {
                    downstreamCalls.incrementAndGet();
                    exchange.setStatusCode(502);
                    exchange.getResponseSender().send("downstream failed");
                }
        );

        try (RunningServer server = start(STRICT_CONFIG, new RequestInterceptor[]{new FingerprintInterceptor()}, replay)) {
            CompletableFuture<HttpResponse<byte[]>> response = server.postChunkedAsync(body(64));
            Assertions.assertTrue(reservationStarted.await(5, TimeUnit.SECONDS));
            Assertions.assertFalse(response.isDone());
            reservation.complete(null);

            Assertions.assertEquals(502, response.get(5, TimeUnit.SECONDS).statusCode());
            Assertions.assertTrue(completionObserved.await(5, TimeUnit.SECONDS));
            Assertions.assertTrue(enteredOnIoThread.get());
            Assertions.assertFalse(resumedOnIoThread.get());
            Assertions.assertEquals(1, downstreamCalls.get());
            Assertions.assertEquals(502, observedStatus.get());
            Assertions.assertEquals(1, releases.get());
        }
    }

    @Test
    void replayProofRejectsTransformerStyleBodyMutationBeforeReservation() throws Exception {
        CompletableFuture<Void> reservation = new CompletableFuture<>();
        CountDownLatch reservationStarted = new CountDownLatch(1);
        AtomicInteger downstreamCalls = new AtomicInteger();
        Phase0ReplayHandler replay = new Phase0ReplayHandler(
                reservation,
                reservationStarted,
                new CountDownLatch(1),
                new AtomicInteger(),
                new AtomicInteger(),
                new AtomicBoolean(),
                new AtomicBoolean(),
                exchange -> {
                    downstreamCalls.incrementAndGet();
                    exchange.getResponseSender().send("should not run");
                }
        );

        RequestInterceptor[] interceptors = {
                new FingerprintInterceptor(),
                new TransformerStyleMutatingInterceptor()
        };
        try (RunningServer server = start(STRICT_CONFIG, interceptors, replay)) {
            HttpResponse<byte[]> response = server.postChunked(body(64));
            Assertions.assertEquals(500, response.statusCode());
            Assertions.assertEquals(1, reservationStarted.getCount());
            Assertions.assertEquals(0, downstreamCalls.get());
        }
    }

    private RunningServer start(String configName, RequestInterceptor[] interceptors, HttpHandler next) {
        previousInterceptors = SingletonServiceFactory.getBeans(RequestInterceptor.class);
        SingletonServiceFactory.setBean(RequestInterceptor.class.getName(), interceptors);
        RequestInterceptorInjectionHandler injection = new RequestInterceptorInjectionHandler(configName);
        injection.setNext(next);
        return new RunningServer(injection);
    }

    private static byte[] body(int length) {
        byte[] body = new byte[length];
        for (int i = 0; i < length; i++)
            body[i] = (byte) (i % 251);
        return body;
    }

    private static byte[] fingerprint(HttpServerExchange exchange) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        PooledByteBuffer[] buffers = exchange.getAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY);
        if (buffers != null) {
            for (PooledByteBuffer pooled : buffers) {
                if (pooled != null)
                    digest.update(pooled.getBuffer().duplicate());
            }
        }
        return digest.digest();
    }

    private abstract static class ProofInterceptor implements RequestInterceptor {
        private HttpHandler next;

        @Override
        public HttpHandler getNext() {
            return next;
        }

        @Override
        public MiddlewareHandler setNext(HttpHandler next) {
            this.next = next;
            return this;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }

    private static class ExactBytesInterceptor extends ProofInterceptor {
        private final AtomicReference<byte[]> observed = new AtomicReference<>();
        private final AtomicBoolean positionsPreserved = new AtomicBoolean();

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            PooledByteBuffer[] buffers = exchange.getAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY);
            int[] positions = Arrays.stream(buffers)
                    .filter(buffer -> buffer != null)
                    .mapToInt(buffer -> buffer.getBuffer().position())
                    .toArray();
            int size = Arrays.stream(buffers)
                    .filter(buffer -> buffer != null)
                    .mapToInt(buffer -> buffer.getBuffer().remaining())
                    .sum();
            byte[] bytes = new byte[size];
            int offset = 0;
            for (PooledByteBuffer pooled : buffers) {
                if (pooled != null) {
                    ByteBuffer duplicate = pooled.getBuffer().duplicate();
                    int remaining = duplicate.remaining();
                    duplicate.get(bytes, offset, remaining);
                    offset += remaining;
                }
            }
            int[] after = Arrays.stream(buffers)
                    .filter(buffer -> buffer != null)
                    .mapToInt(buffer -> buffer.getBuffer().position())
                    .toArray();
            observed.set(bytes);
            positionsPreserved.set(Arrays.equals(positions, after));
        }

        @Override
        public boolean isRequiredContent() {
            return true;
        }
    }

    private static class FingerprintInterceptor extends ProofInterceptor {
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            exchange.putAttachment(BODY_FINGERPRINT, fingerprint(exchange));
        }

        @Override
        public boolean isRequiredContent() {
            return true;
        }
    }

    private static class TransformerStyleMutatingInterceptor extends ProofInterceptor {
        @Override
        public void handleRequest(HttpServerExchange exchange) {
            PooledByteBuffer[] buffers = exchange.getAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY);
            for (PooledByteBuffer pooled : buffers) {
                if (pooled != null && pooled.getBuffer().hasRemaining()) {
                    ByteBuffer buffer = pooled.getBuffer();
                    buffer.put(buffer.position(), (byte) (buffer.get(buffer.position()) ^ 0x01));
                    return;
                }
            }
        }

        @Override
        public boolean isRequiredContent() {
            return true;
        }
    }

    private static class Phase0ReplayHandler implements HttpHandler {
        private final CompletableFuture<Void> reservation;
        private final CountDownLatch reservationStarted;
        private final CountDownLatch completionObserved;
        private final AtomicInteger releases;
        private final AtomicInteger observedStatus;
        private final AtomicBoolean enteredOnIoThread;
        private final AtomicBoolean resumedOnIoThread;
        private final HttpHandler next;

        private Phase0ReplayHandler(CompletableFuture<Void> reservation,
                                    CountDownLatch reservationStarted,
                                    CountDownLatch completionObserved,
                                    AtomicInteger releases,
                                    AtomicInteger observedStatus,
                                    AtomicBoolean enteredOnIoThread,
                                    AtomicBoolean resumedOnIoThread,
                                    HttpHandler next) {
            this.reservation = reservation;
            this.reservationStarted = reservationStarted;
            this.completionObserved = completionObserved;
            this.releases = releases;
            this.observedStatus = observedStatus;
            this.enteredOnIoThread = enteredOnIoThread;
            this.resumedOnIoThread = resumedOnIoThread;
            this.next = next;
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            byte[] expected = exchange.getAttachment(BODY_FINGERPRINT);
            if (expected == null || !MessageDigest.isEqual(expected, fingerprint(exchange))) {
                exchange.setStatusCode(500);
                exchange.getResponseSender().send("buffered body changed");
                return;
            }

            enteredOnIoThread.set(exchange.isInIoThread());
            exchange.addExchangeCompleteListener(new FailureReleaseListener(
                    completionObserved, releases, observedStatus));
            reservationStarted.countDown();
            // Keep the exchange open after this I/O-thread call returns. The
            // completion callback redispatches through Undertow's root-handler
            // machinery, which owns final exchange completion.
            exchange.dispatch();
            reservation.whenComplete((ignored, failure) -> exchange.dispatch(ex -> {
                resumedOnIoThread.set(exchange.isInIoThread());
                if (failure != null) {
                    exchange.setStatusCode(503);
                    exchange.getResponseSender().send("reservation failed");
                    return;
                }
                try {
                    next.handleRequest(exchange);
                } catch (Exception e) {
                    exchange.setStatusCode(500);
                    exchange.getResponseSender().send("continuation failed");
                }
            }));
        }
    }

    private static class FailureReleaseListener implements ExchangeCompletionListener {
        private final CountDownLatch completionObserved;
        private final AtomicInteger releases;
        private final AtomicInteger observedStatus;

        private FailureReleaseListener(CountDownLatch completionObserved,
                                       AtomicInteger releases,
                                       AtomicInteger observedStatus) {
            this.completionObserved = completionObserved;
            this.releases = releases;
            this.observedStatus = observedStatus;
        }

        @Override
        public void exchangeEvent(HttpServerExchange exchange, NextListener nextListener) {
            try {
                int status = exchange.getStatusCode();
                observedStatus.set(status);
                if (status < 200 || status >= 300)
                    releases.incrementAndGet();
                completionObserved.countDown();
            } finally {
                nextListener.proceed();
            }
        }
    }

    private static class RunningServer implements AutoCloseable {
        private final Undertow server;
        private final URI endpoint;
        private final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        private RunningServer(HttpHandler handler) {
            server = Undertow.builder()
                    .addHttpListener(0, "127.0.0.1")
                    .setBufferSize(1024)
                    .setServerOption(UndertowOptions.MAX_ENTITY_SIZE, 32L * 1024 * 1024)
                    .setHandler(handler)
                    .build();
            server.start();
            InetSocketAddress address = (InetSocketAddress) server.getListenerInfo().get(0).getAddress();
            endpoint = URI.create("http://127.0.0.1:" + address.getPort() + "/phase0");
        }

        private HttpResponse<byte[]> postChunked(byte[] body) throws Exception {
            return postChunkedAsync(body).get(5, TimeUnit.SECONDS);
        }

        private CompletableFuture<HttpResponse<byte[]>> postChunkedAsync(byte[] body) {
            HttpRequest.BodyPublisher knownLength = HttpRequest.BodyPublishers.ofByteArray(body);
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/octet-stream")
                    .POST(HttpRequest.BodyPublishers.fromPublisher(knownLength))
                    .build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
        }

        @Override
        public void close() {
            server.stop();
        }
    }
}
