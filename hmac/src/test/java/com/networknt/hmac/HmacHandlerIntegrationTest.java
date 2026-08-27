/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.hmac;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.RequestInjectionConfig;
import com.networknt.handler.RequestInterceptor;
import com.networknt.handler.RequestInterceptorInjectionHandler;
import com.networknt.hmac.config.HmacConfig;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.security.UnifiedSecurityConfig;
import com.networknt.server.ServerConfig;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class HmacHandlerIntegrationTest {
    private static final String SECRET = "phase-3-exact-forwarding-secret";
    private static final String INJECTION_CONFIG = "hmac-phase3-request-injection";
    private final List<String> configNames = new java.util.ArrayList<>();
    private RequestInterceptor[] previousInterceptors;
    private boolean interceptorsCaptured;

    @AfterEach
    void cleanup() {
        if (interceptorsCaptured)
            SingletonServiceFactory.setBean(RequestInterceptor.class.getName(), previousInterceptors);
        configNames.forEach(Config.getInstance()::clearConfigCache);
    }

    @Test
    void reservationIsAsynchronousAndDuplicateKeepsExactSuccessfulDelivery() throws Exception {
        Context context = context("async-retain");
        DelayedReplayStore store = context.store;
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<byte[]> forwarded = new AtomicReference<>();
        byte[] body = "{ \"event\" : \"héllo 世界\" }\n".getBytes(StandardCharsets.UTF_8);

        HttpHandler callee = exchange -> {
            Assertions.assertTrue(exchange.getAttachment(HmacAttachments.AUTHENTICATION_EVIDENCE)
                    .matches("/github-webhook", "github", exchange.getRequestPath()));
            calls.incrementAndGet();
            exchange.startBlocking();
            forwarded.set(exchange.getInputStream().readAllBytes());
            exchange.getResponseSender().send("accepted");
        };

        try (RunningServer server = start(context, new RequestInterceptor[]{context.interceptor}, callee)) {
            CompletableFuture<HttpResponse<byte[]>> response = server.post(body, "delivery-retained");
            Assertions.assertTrue(store.reserveStarted.await(5, TimeUnit.SECONDS));
            Assertions.assertFalse(response.isDone());
            store.allowReservation.complete(null);

            Assertions.assertEquals(200, response.get(5, TimeUnit.SECONDS).statusCode());
            Assertions.assertArrayEquals(body, forwarded.get());
            Assertions.assertEquals(1, calls.get());

            HttpResponse<byte[]> duplicate = server.post(body, "delivery-retained").get(5, TimeUnit.SECONDS);
            Assertions.assertEquals(200, duplicate.statusCode());
            Assertions.assertEquals(0, duplicate.body().length);
            Assertions.assertEquals(1, calls.get());
        }
    }

    @Test
    void exactCountingCalleeMatrixCoversHttp11AndHttp2WithKnownAndUnknownLengths() throws Exception {
        for (HttpClient.Version version : List.of(HttpClient.Version.HTTP_1_1, HttpClient.Version.HTTP_2)) {
            Context context = context("protocol-" + version.name().toLowerCase());
            context.store.allowReservation.complete(null);
            AtomicInteger calls = new AtomicInteger();
            List<byte[]> forwarded = new java.util.concurrent.CopyOnWriteArrayList<>();
            HttpHandler callee = exchange -> {
                calls.incrementAndGet();
                Assertions.assertEquals("push", exchange.getRequestHeaders().getFirst("X-GitHub-Event"));
                exchange.startBlocking();
                forwarded.add(exchange.getInputStream().readAllBytes());
                exchange.getResponseSender().send("accepted");
            };
            byte[] knownLength = ("known-length-" + version).getBytes(StandardCharsets.UTF_8);
            byte[] streamed = ("streamed-世界-" + version).getBytes(StandardCharsets.UTF_8);

            try (RunningServer server = start(context,
                    new RequestInterceptor[]{context.interceptor}, callee, version)) {
                HttpResponse<byte[]> first = server.post(knownLength,
                        "delivery-known-" + version, false).get(5, TimeUnit.SECONDS);
                HttpResponse<byte[]> second = server.post(streamed,
                        "delivery-streamed-" + version, true).get(5, TimeUnit.SECONDS);
                HttpResponse<byte[]> duplicate = server.post(streamed,
                        "delivery-streamed-" + version, true).get(5, TimeUnit.SECONDS);

                Assertions.assertEquals(version, first.version());
                Assertions.assertEquals(version, second.version());
                Assertions.assertEquals(200, duplicate.statusCode());
                Assertions.assertEquals(0, duplicate.body().length);
                Assertions.assertEquals(2, calls.get());
                Assertions.assertArrayEquals(knownLength, forwarded.get(0));
                Assertions.assertArrayEquals(streamed, forwarded.get(1));
            }
        }
    }

    @Test
    void non2xxReleasesReservationForRedelivery() throws Exception {
        Context context = context("release");
        context.store.allowReservation.complete(null);
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger status = new AtomicInteger(502);
        byte[] body = "release-me".getBytes(StandardCharsets.UTF_8);
        HttpHandler callee = exchange -> {
            calls.incrementAndGet();
            exchange.setStatusCode(status.get());
            exchange.getResponseSender().send("downstream");
        };

        try (RunningServer server = start(context, new RequestInterceptor[]{context.interceptor}, callee)) {
            Assertions.assertEquals(502,
                    server.post(body, "delivery-release").get(5, TimeUnit.SECONDS).statusCode());
            Assertions.assertTrue(context.store.released.await(5, TimeUnit.SECONDS));
            status.set(200);
            Assertions.assertEquals(200,
                    server.post(body, "delivery-release").get(5, TimeUnit.SECONDS).statusCode());
            Assertions.assertEquals(2, calls.get());
        }
    }

    @Test
    void authenticationFailureAndThrownCalleeBothReleaseReservations() throws Exception {
        Context unifiedContext = context("authentication-release");
        unifiedContext.store.allowReservation.complete(null);
        AtomicInteger protectedCalls = new AtomicInteger();
        HttpHandler authenticationFailure = exchange -> {
            exchange.setStatusCode(401);
            exchange.endExchange();
        };
        try (RunningServer server = start(unifiedContext,
                new RequestInterceptor[]{unifiedContext.interceptor}, authenticationFailure)) {
            Assertions.assertEquals(401,
                    server.post("missing-jwt".getBytes(StandardCharsets.UTF_8), "delivery-unified-failure")
                            .get(5, TimeUnit.SECONDS).statusCode());
            Assertions.assertTrue(unifiedContext.store.released.await(5, TimeUnit.SECONDS));
            Assertions.assertEquals(0, protectedCalls.get());
            Assertions.assertInstanceOf(ReserveOutcome.Reserved.class,
                    unifiedContext.store.delegate.reserve(new WebhookReplayKey(
                                    "github", "shared", "delivery-unified-failure"), Duration.ofMinutes(1))
                            .toCompletableFuture().join());
        }

        Context thrownContext = context("thrown-release");
        thrownContext.store.allowReservation.complete(null);
        AtomicBoolean fail = new AtomicBoolean(true);
        HttpHandler throwingCallee = exchange -> {
            if (fail.getAndSet(false)) throw new IllegalStateException("simulated proxy failure");
            exchange.getResponseSender().send("recovered");
        };
        try (RunningServer server = start(thrownContext,
                new RequestInterceptor[]{thrownContext.interceptor}, throwingCallee)) {
            Assertions.assertEquals(500,
                    server.post("proxy-failure".getBytes(StandardCharsets.UTF_8), "delivery-thrown")
                            .get(5, TimeUnit.SECONDS).statusCode());
            Assertions.assertTrue(thrownContext.store.released.await(5, TimeUnit.SECONDS));
            Assertions.assertEquals(200,
                    server.post("proxy-failure".getBytes(StandardCharsets.UTF_8), "delivery-thrown")
                            .get(5, TimeUnit.SECONDS).statusCode());
        }
    }

    @Test
    void mutationAndMissingInterceptorWiringFailClosedBeforeReservationOrCallee() throws Exception {
        Context context = context("chain-errors");
        context.store.allowReservation.complete(null);
        AtomicInteger calls = new AtomicInteger();
        HttpHandler callee = exchange -> {
            calls.incrementAndGet();
            exchange.getResponseSender().send("should-not-run");
        };

        try (RunningServer server = start(context,
                new RequestInterceptor[]{context.interceptor, new MutatingInterceptor()}, callee)) {
            Assertions.assertEquals(500,
                    server.post("original".getBytes(StandardCharsets.UTF_8), "delivery-mutated")
                            .get(5, TimeUnit.SECONDS).statusCode());
            Assertions.assertEquals(0, context.store.reserveCalls.get());
            Assertions.assertEquals(0, calls.get());
        }

        Context pathContext = context("path-change");
        pathContext.store.allowReservation.complete(null);
        try (RunningServer server = start(pathContext,
                new RequestInterceptor[]{pathContext.interceptor, new PathMutatingInterceptor()}, callee)) {
            Assertions.assertEquals(503,
                    server.post("original".getBytes(StandardCharsets.UTF_8), "delivery-path-change")
                            .get(5, TimeUnit.SECONDS).statusCode());
            Assertions.assertEquals(0, pathContext.store.reserveCalls.get());
            Assertions.assertEquals(0, calls.get());
        }

        HmacHandler handler = new HmacHandler(() -> context.hmac, () -> context.unified, false);
        handler.setNext(callee);
        try (RunningServer server = new RunningServer(handler, context)) {
            Assertions.assertEquals(503,
                    server.post("unbuffered".getBytes(StandardCharsets.UTF_8), "delivery-missing")
                            .get(5, TimeUnit.SECONDS).statusCode());
            Assertions.assertEquals(0, calls.get());
        }
    }

    @Test
    void handlerRejectsWrongAdjacentHandlerWhenHmacIsEnabled() {
        Context context = context("wrong-next");
        HmacHandler handler = new HmacHandler(() -> context.hmac, () -> context.unified, true);
        Assertions.assertThrows(Exception.class,
                () -> handler.setNext(exchange -> exchange.endExchange()));
    }

    @Test
    void handlerRejectsWrongAdjacentHandlerBeforeHmacIsEnabledForSafeReload() {
        String hmacName = "phase3-hmac-disabled-wrong-next";
        String unifiedName = "phase3-unified-disabled-wrong-next";
        cache(hmacName, Map.of("enabled", false, "profiles", Map.of()));
        cache(unifiedName, Map.of("enabled", true, "anonymousPrefixes", List.of(),
                "pathPrefixAuths", List.of(Map.of("prefix", "/legacy", "jwt", true))));
        HmacHandler handler = new HmacHandler(() -> HmacConfig.load(hmacName),
                () -> UnifiedSecurityConfig.load(unifiedName), true);
        Assertions.assertThrows(Exception.class,
                () -> handler.setNext(exchange -> exchange.endExchange()));
    }

    @Test
    void handlerValidatesItsPositionInTheMaterializedChain() {
        Context context = context("materialized-next");
        HmacHandler handler = new HmacHandler(() -> context.hmac, () -> context.unified, false);
        HttpHandler unified = new com.networknt.security.UnifiedSecurityHandler();
        Assertions.assertDoesNotThrow(() -> handler.validateChain(List.of(handler, unified), 0,
                "path POST /github-webhook"));
        Assertions.assertThrows(ConfigException.class, () -> handler.validateChain(
                List.of(handler, exchange -> exchange.endExchange(), unified), 0,
                "path POST /github-webhook"));
    }

    private RunningServer start(Context context, RequestInterceptor[] interceptors, HttpHandler callee) {
        return start(context, interceptors, callee, HttpClient.Version.HTTP_1_1);
    }

    private RunningServer start(Context context,
                                RequestInterceptor[] interceptors,
                                HttpHandler callee,
                                HttpClient.Version version) {
        if (!interceptorsCaptured) {
            previousInterceptors = SingletonServiceFactory.getBeans(RequestInterceptor.class);
            interceptorsCaptured = true;
        }
        SingletonServiceFactory.setBean(RequestInterceptor.class.getName(), interceptors);
        HmacHandler hmacHandler = new HmacHandler(() -> context.hmac, () -> context.unified, false);
        hmacHandler.setNext(callee);
        RequestInterceptorInjectionHandler injection = new RequestInterceptorInjectionHandler(INJECTION_CONFIG);
        injection.setNext(hmacHandler);
        return new RunningServer(injection, context, version);
    }

    private Context context(String suffix) {
        String hmacName = "phase3-hmac-" + suffix;
        String unifiedName = "phase3-unified-" + suffix;
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("signatureHeader", "X-Hub-Signature-256");
        profile.put("signaturePrefix", "sha256=");
        profile.put("signatureEncoding", "hex");
        profile.put("maxBodyBytes", 4096);
        profile.put("secrets", Map.of(
                "selectorHeader", "",
                "bySelector", Map.of(),
                "defaultEnvNames", List.of("PHASE3_SECRET")));
        profile.put("replay", Map.of(
                "enabled", true,
                "idHeader", "X-GitHub-Delivery",
                "retentionSeconds", 300));
        cache(hmacName, Map.of("enabled", true, "profiles", Map.of("github", profile)));

        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("prefix", "/github-webhook");
        rule.put("hmacProfile", "github");
        Map<String, Object> unifiedRoot = Map.of(
                "enabled", true,
                "anonymousPrefixes", List.of(),
                "pathPrefixAuths", List.of(rule));
        cache(unifiedName, unifiedRoot);

        HmacConfig hmac = HmacConfig.load(hmacName);
        UnifiedSecurityConfig unified = UnifiedSecurityConfig.load(unifiedName);
        DelayedReplayStore store = new DelayedReplayStore();
        HmacRuntimeManager manager = new HmacRuntimeManager(
                name -> "PHASE3_SECRET".equals(name) ? SECRET : null,
                () -> new WebhookReplayStore[]{store});
        HmacRequestInterceptor interceptor = new HmacRequestInterceptor(manager, () -> hmac, () -> unified,
                () -> RequestInjectionConfig.load(INJECTION_CONFIG), ServerConfig::load);
        return new Context(hmac, unified, store, interceptor);
    }

    private void cache(String name, Map<String, Object> value) {
        configNames.add(name);
        Config.getInstance().putInConfigCache(name, value);
    }

    private static byte[] sign(byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(body);
    }

    private record Context(HmacConfig hmac,
                           UnifiedSecurityConfig unified,
                           DelayedReplayStore store,
                           HmacRequestInterceptor interceptor) {
    }

    private static final class DelayedReplayStore implements WebhookReplayStore {
        private final LocalWebhookReplayStore delegate = new LocalWebhookReplayStore();
        private final CompletableFuture<Void> allowReservation = new CompletableFuture<>();
        private final CountDownLatch reserveStarted = new CountDownLatch(1);
        private final CountDownLatch released = new CountDownLatch(1);
        private final AtomicInteger reserveCalls = new AtomicInteger();

        @Override
        public CompletionStage<ReserveOutcome> reserve(WebhookReplayKey key, Duration retention) {
            reserveCalls.incrementAndGet();
            reserveStarted.countDown();
            return allowReservation.thenCompose(ignored -> delegate.reserve(key, retention));
        }

        @Override
        public CompletionStage<Void> release(ReplayReservation reservation) {
            return delegate.release(reservation).whenComplete((ignored, failure) -> released.countDown());
        }

        @Override
        public CompletionStage<Boolean> forceRemove(WebhookReplayKey key) {
            return delegate.forceRemove(key);
        }

        @Override
        public ReplayStoreScope getScope() {
            return ReplayStoreScope.LOCAL;
        }

        @Override
        public ReplayStoreSummary getSummary() {
            return delegate.getSummary();
        }
    }

    private static final class MutatingInterceptor implements RequestInterceptor {
        @Override
        public void handleRequest(HttpServerExchange exchange) {
            PooledByteBuffer[] buffers = exchange.getAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY);
            for (PooledByteBuffer pooled : buffers) {
                if (pooled != null && pooled.getBuffer().hasRemaining()) {
                    ByteBuffer bytes = pooled.getBuffer();
                    bytes.put(bytes.position(), (byte) (bytes.get(bytes.position()) ^ 1));
                    return;
                }
            }
        }

        @Override
        public boolean isRequiredContent() {
            return true;
        }

        @Override
        public HttpHandler getNext() {
            return null;
        }

        @Override
        public MiddlewareHandler setNext(HttpHandler next) {
            return this;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }

    private static final class PathMutatingInterceptor implements RequestInterceptor {
        @Override public void handleRequest(HttpServerExchange exchange) {
            exchange.setRequestPath("/changed-after-hmac");
        }
        @Override public boolean isRequiredContent() { return false; }
        @Override public HttpHandler getNext() { return null; }
        @Override public MiddlewareHandler setNext(HttpHandler next) { return this; }
        @Override public boolean isEnabled() { return true; }
    }

    private static final class RunningServer implements AutoCloseable {
        private final Undertow server;
        private final URI endpoint;
        private final Context context;
        private final HttpClient client;

        private RunningServer(HttpHandler handler, Context context) {
            this(handler, context, HttpClient.Version.HTTP_1_1);
        }

        private RunningServer(HttpHandler handler, Context context, HttpClient.Version version) {
            this.context = context;
            server = Undertow.builder()
                    .addHttpListener(0, "127.0.0.1")
                    .setBufferSize(1024)
                    .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                    .setHandler(handler)
                    .build();
            server.start();
            InetSocketAddress address = (InetSocketAddress) server.getListenerInfo().get(0).getAddress();
            endpoint = URI.create("http://127.0.0.1:" + address.getPort() + "/github-webhook");
            client = HttpClient.newBuilder().version(version).build();
        }

        private CompletableFuture<HttpResponse<byte[]>> post(byte[] body, String delivery) throws Exception {
            return post(body, delivery, false);
        }

        private CompletableFuture<HttpResponse<byte[]>> post(byte[] body,
                                                              String delivery,
                                                              boolean unknownLength) throws Exception {
            HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.ofByteArray(body);
            if (unknownLength)
                publisher = HttpRequest.BodyPublishers.fromPublisher(publisher);
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(5))
                    .header("X-GitHub-Delivery", delivery)
                    .header("X-Hub-Signature-256", "sha256=" + HexFormat.of().formatHex(sign(body)))
                    .header("X-GitHub-Event", "push")
                    .POST(publisher)
                    .build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
        }

        @Override
        public void close() {
            server.stop();
        }
    }
}
