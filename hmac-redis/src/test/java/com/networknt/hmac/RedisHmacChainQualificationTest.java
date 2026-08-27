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
import com.networknt.handler.RequestInjectionConfig;
import com.networknt.handler.RequestInterceptor;
import com.networknt.handler.RequestInterceptorInjectionHandler;
import com.networknt.hmac.config.HmacConfig;
import com.networknt.hmac.redis.RedisWebhookReplayStore;
import com.networknt.security.UnifiedSecurityConfig;
import com.networknt.server.ServerConfig;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Opt-in qualification of the complete HTTP/HMAC chain against a real Redis provider. */
class RedisHmacChainQualificationTest {
    private static final String ENABLE_PROPERTY = "hmac.phase4.redisChain";
    private static final String SECRET = "phase-4-real-redis-secret";
    private static final String HMAC_CONFIG = "phase4-redis-chain-hmac";
    private static final String UNIFIED_CONFIG = "phase4-redis-chain-unified";
    private static final String INJECTION_CONFIG = "hmac-redis-chain-request-injection";
    private RequestInterceptor[] previousInterceptors;

    @AfterEach
    void cleanup() {
        if (previousInterceptors != null)
            SingletonServiceFactory.setBean(RequestInterceptor.class.getName(), previousInterceptors);
        Config.getInstance().clearConfigCache(HMAC_CONFIG);
        Config.getInstance().clearConfigCache(UNIFIED_CONFIG);
    }

    @Test
    void realRedisReservationContinuesOffIoThreadAndSuppressesDuplicate() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean(ENABLE_PROPERTY));
        cacheConfiguration();
        HmacConfig hmac = HmacConfig.load(HMAC_CONFIG);
        UnifiedSecurityConfig unified = UnifiedSecurityConfig.load(UNIFIED_CONFIG);
        RedisWebhookReplayStore store = new RedisWebhookReplayStore();
        store.setKeyPrefix("light:hmac-phase4-chain:" + UUID.randomUUID() + ":");
        store.validate();

        HmacRuntimeManager manager = new HmacRuntimeManager(
                name -> "PHASE4_SECRET".equals(name) ? SECRET : null,
                () -> new WebhookReplayStore[]{store});
        HmacRequestInterceptor interceptor = new HmacRequestInterceptor(manager, () -> hmac, () -> unified,
                () -> RequestInjectionConfig.load(INJECTION_CONFIG), ServerConfig::load);
        previousInterceptors = SingletonServiceFactory.getBeans(RequestInterceptor.class);
        SingletonServiceFactory.setBean(RequestInterceptor.class.getName(), new RequestInterceptor[]{interceptor});

        AtomicBoolean calleeOnIoThread = new AtomicBoolean(true);
        AtomicBoolean called = new AtomicBoolean();
        HttpHandler callee = exchange -> {
            called.set(true);
            calleeOnIoThread.set(exchange.isInIoThread());
            Assertions.assertEquals("push", exchange.getRequestHeaders().getFirst("X-GitHub-Event"));
            exchange.getResponseSender().send("accepted");
        };
        HmacHandler hmacHandler = new HmacHandler(() -> hmac, () -> unified, false);
        hmacHandler.setNext(callee);
        RequestInterceptorInjectionHandler injection = new RequestInterceptorInjectionHandler(INJECTION_CONFIG);
        injection.setNext(hmacHandler);

        Undertow server = Undertow.builder().addHttpListener(0, "127.0.0.1").setHandler(injection).build();
        server.start();
        try {
            InetSocketAddress address = (InetSocketAddress) server.getListenerInfo().get(0).getAddress();
            URI endpoint = URI.create("http://127.0.0.1:" + address.getPort() + "/github-webhook");
            byte[] body = "real-redis-chain".getBytes(StandardCharsets.UTF_8);
            String delivery = "delivery-" + UUID.randomUUID();

            HttpResponse<byte[]> first = post(endpoint, body, delivery);
            Assertions.assertEquals(200, first.statusCode());
            Assertions.assertTrue(called.get());
            Assertions.assertFalse(calleeOnIoThread.get());

            called.set(false);
            HttpResponse<byte[]> duplicate = post(endpoint, body, delivery);
            Assertions.assertEquals(200, duplicate.statusCode());
            Assertions.assertEquals(0, duplicate.body().length);
            Assertions.assertFalse(called.get());
        } finally {
            server.stop();
            store.onShutdown();
        }
    }

    private HttpResponse<byte[]> post(URI endpoint, byte[] body, String delivery) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(5))
                .header("X-GitHub-Delivery", delivery)
                .header("X-Hub-Signature-256", "sha256=" + HexFormat.of().formatHex(sign(body)))
                .header("X-GitHub-Event", "push")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private byte[] sign(byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(body);
    }

    private void cacheConfiguration() {
        Map<String, Object> profile = Map.of(
                "signatureHeader", "X-Hub-Signature-256",
                "signaturePrefix", "sha256=",
                "signatureEncoding", "hex",
                "maxBodyBytes", 4096,
                "secrets", Map.of("selectorHeader", "", "bySelector", Map.of(),
                        "defaultEnvNames", List.of("PHASE4_SECRET")),
                "replay", Map.of("enabled", true, "idHeader", "X-GitHub-Delivery",
                        "retentionSeconds", 300));
        Config.getInstance().putInConfigCache(HMAC_CONFIG,
                Map.of("enabled", true, "profiles", Map.of("github", profile)));
        Config.getInstance().putInConfigCache(UNIFIED_CONFIG,
                Map.of("enabled", true, "anonymousPrefixes", List.of(), "pathPrefixAuths",
                        List.of(Map.of("prefix", "/github-webhook", "hmacProfile", "github"))));
    }
}
