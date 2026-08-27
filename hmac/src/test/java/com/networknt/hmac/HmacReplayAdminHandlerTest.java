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
import com.networknt.hmac.config.HmacConfig;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.security.UnifiedSecurityConfig;
import io.undertow.Undertow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

class HmacReplayAdminHandlerTest {
    private final List<String> configNames = new ArrayList<>();

    @AfterEach
    void clearConfigs() {
        configNames.forEach(Config.getInstance()::clearConfigCache);
    }

    @Test
    void operatorRemovalIsAsynchronousAuditedAndDoesNotExposeReplayMaterial() throws Exception {
        LocalWebhookReplayStore store = new LocalWebhookReplayStore();
        HmacConfig hmac = hmac("hmac-admin");
        UnifiedSecurityConfig unified = unified("unified-admin");
        HmacRuntimeManager manager = new HmacRuntimeManager(name -> "secret-value",
                () -> new WebhookReplayStore[]{store});
        HmacReplayAdminHandler handler = new HmacReplayAdminHandler(manager, () -> hmac, () -> unified);
        String delivery = "sensitive-delivery-id";
        WebhookReplayKey key = new WebhookReplayKey("github", "shared", delivery);
        store.reserve(key, Duration.ofMinutes(1)).toCompletableFuture().join();
        AtomicReference<Object> requestBody = new AtomicReference<>(Map.of(
                "profile", "github", "selector", "shared", "deliveryId", delivery));
        AtomicReference<Map<String, Object>> audit = new AtomicReference<>();

        try (RunningServer server = new RunningServer(handler, requestBody, audit)) {
            HttpResponse<String> response = server.post();
            Assertions.assertEquals(200, response.statusCode());
            Assertions.assertEquals(Map.of("removed", true, "scope", "local"),
                    Config.getInstance().getMapper().readValue(response.body(), Map.class));
            Assertions.assertFalse(response.body().contains(delivery));
            Assertions.assertEquals("forceRemove", audit.get().get("hmacReplayAdminAction"));
            Assertions.assertEquals("removed", audit.get().get("hmacReplayAdminOutcome"));
            Assertions.assertEquals("local", audit.get().get("hmacReplayStoreScope"));
            Assertions.assertInstanceOf(ReserveOutcome.Reserved.class,
                    store.reserve(key, Duration.ofMinutes(1)).toCompletableFuture().join());

            requestBody.set(Map.of("profile", "github"));
            Assertions.assertEquals(400, server.post().statusCode());
        }

        requestBody.set(Map.of("profile", "github", "selector", "shared", "deliveryId", delivery));
        HmacReplayAdminHandler unavailable = new HmacReplayAdminHandler(
                new HmacRuntimeManager(name -> null, () -> new WebhookReplayStore[]{store}),
                () -> hmac, () -> unified);
        try (RunningServer server = new RunningServer(unavailable, requestBody, audit)) {
            Assertions.assertEquals(503, server.post().statusCode());
            Assertions.assertEquals("unavailable", audit.get().get("hmacReplayAdminOutcome"));
        }
    }

    private HmacConfig hmac(String name) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("signatureHeader", "X-Hub-Signature-256");
        profile.put("secrets", Map.of(
                "selectorHeader", "",
                "bySelector", Map.of(),
                "defaultEnvNames", List.of("SECRET_ENV")));
        profile.put("replay", Map.of(
                "enabled", true,
                "idHeader", "X-GitHub-Delivery",
                "retentionSeconds", 60));
        cache(name, Map.of("enabled", true, "profiles", Map.of("github", profile)));
        return HmacConfig.load(name);
    }

    private UnifiedSecurityConfig unified(String name) {
        cache(name, Map.of(
                "enabled", true,
                "anonymousPrefixes", List.of(),
                "pathPrefixAuths", List.of(Map.of(
                        "prefix", "/github-webhook",
                        "hmacProfile", "github"))));
        return UnifiedSecurityConfig.load(name);
    }

    private void cache(String name, Map<String, Object> value) {
        configNames.add(name);
        Config.getInstance().putInConfigCache(name, value);
    }

    private static final class RunningServer implements AutoCloseable {
        private final Undertow server;
        private final URI endpoint;
        private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

        private RunningServer(HmacReplayAdminHandler handler,
                              AtomicReference<Object> requestBody,
                              AtomicReference<Map<String, Object>> observedAudit) {
            server = Undertow.builder()
                    .addHttpListener(0, "127.0.0.1")
                    .setHandler(exchange -> {
                        Map<String, Object> audit = new HashMap<>();
                        observedAudit.set(audit);
                        exchange.putAttachment(AttachmentConstants.AUDIT_INFO, audit);
                        exchange.putAttachment(AttachmentConstants.REQUEST_BODY, requestBody.get());
                        handler.handleRequest(exchange);
                    })
                    .build();
            server.start();
            InetSocketAddress address = (InetSocketAddress) server.getListenerInfo().get(0).getAddress();
            endpoint = URI.create("http://127.0.0.1:" + address.getPort() + "/adm/hmac-replay/remove");
        }

        private HttpResponse<String> post() throws Exception {
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}", StandardCharsets.UTF_8))
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }

        @Override
        public void close() {
            server.stop();
        }
    }
}
