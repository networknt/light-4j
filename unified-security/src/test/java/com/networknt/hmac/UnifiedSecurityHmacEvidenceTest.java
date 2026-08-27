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
import com.networknt.handler.Handler;
import com.networknt.hmac.config.HmacConfig;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.security.UnifiedSecurityConfig;
import com.networknt.security.UnifiedSecurityHandler;
import com.networknt.status.Status;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class UnifiedSecurityHmacEvidenceTest {
    private static final String SECRET = "unified-security-hmac-evidence-secret";
    private static final String HMAC_CONFIG = "unified-security-hmac-evidence";

    @AfterEach
    void clearConfig() {
        Config.getInstance().clearConfigCache(HMAC_CONFIG);
        Config.getInstance().clearConfigCache(UnifiedSecurityConfig.CONFIG_NAME);
    }

    @Test
    void hmacOnlyRequiresFinalEvidenceAndLegacyHeaderBranchesRemainOptional() throws Exception {
        Map<String, Object> rule = Map.of(
                "prefix", "/webhook",
                "hmacProfile", "github");
        Config.getInstance().putInConfigCache(UnifiedSecurityConfig.CONFIG_NAME, Map.of(
                "enabled", true,
                "anonymousPrefixes", List.of(),
                "pathPrefixAuths", List.of(rule)));
        Map<String, Object> profile = Map.of(
                "signatureHeader", "X-Hub-Signature-256",
                "signaturePrefix", "sha256=",
                "signatureEncoding", "hex",
                "maxBodyBytes", 1024,
                "secrets", Map.of(
                        "selectorHeader", "",
                        "bySelector", Map.of(),
                        "defaultEnvNames", List.of("HMAC_EVIDENCE_SECRET")),
                "replay", Map.of("enabled", false));
        Config.getInstance().putInConfigCache(HMAC_CONFIG, Map.of(
                "enabled", true,
                "profiles", Map.of("github", profile)));

        UnifiedSecurityConfig unifiedConfig = UnifiedSecurityConfig.load();
        HmacConfig hmacConfig = HmacConfig.load(HMAC_CONFIG);
        HmacRuntime runtime = HmacRuntime.compile(hmacConfig, unifiedConfig,
                name -> "HMAC_EVIDENCE_SECRET".equals(name) ? SECRET : null);
        byte[] body = "exact signed bytes".getBytes(StandardCharsets.UTF_8);
        HttpServerExchange admitted = exchange(body);
        admitted.getRequestHeaders().put(new HttpString("X-Hub-Signature-256"),
                "sha256=" + HexFormat.of().formatHex(sign(body)));
        Assertions.assertNull(new HmacVerifier().verify(admitted, runtime, unifiedConfig));

        AtomicInteger calls = new AtomicInteger();
        UnifiedSecurityHandler unified = new UnifiedSecurityHandler();
        unified.setNext(exchange -> calls.incrementAndGet());
        HmacHandler hmac = new HmacHandler(() -> hmacConfig, () -> unifiedConfig, false);
        hmac.setNext(unified);
        hmac.handleRequest(admitted);

        Assertions.assertEquals(1, calls.get());
        Assertions.assertNotNull(admitted.getAttachment(HmacAttachments.AUTHENTICATION_EVIDENCE));
        Assertions.assertNull(admitted.getRequestHeaders().getFirst("Authorization"));

        Status missing = unified.verifyUnifiedSecurity(exchange(body));
        Assertions.assertNotNull(missing);
        Assertions.assertEquals(503, missing.getStatusCode());
    }

    @Test
    void validHmacCannotReplaceRequiredJwtOrApiKeyAndMissingHmacWinsFirst() throws Exception {
        byte[] body = "composed authentication".getBytes(StandardCharsets.UTF_8);
        Status jwtMissing = admitThenVerify(body, Map.of(
                "prefix", "/webhook", "hmacProfile", "github", "jwt", true));
        Assertions.assertNotNull(jwtMissing);
        Assertions.assertEquals(401, jwtMissing.getStatusCode());

        var previousApiKey = Handler.getHandlers().remove("apikey");
        try {
            Status apiKeyMissing = admitThenVerify(body, Map.of(
                    "prefix", "/webhook", "hmacProfile", "github", "apikey", true));
            Assertions.assertNotNull(apiKeyMissing);
            Assertions.assertEquals(400, apiKeyMissing.getStatusCode());
        } finally {
            if (previousApiKey != null) Handler.getHandlers().put("apikey", previousApiKey);
        }

        configure(Map.of("prefix", "/webhook", "hmacProfile", "github", "jwt", true));
        HttpServerExchange withoutEvidence = exchange(body);
        withoutEvidence.getRequestHeaders().put(new HttpString("Authorization"), "Bearer syntactically.valid.token");
        Status hmacMissing = new UnifiedSecurityHandler().verifyUnifiedSecurity(withoutEvidence);
        Assertions.assertNotNull(hmacMissing);
        Assertions.assertEquals(503, hmacMissing.getStatusCode());
    }

    private Status admitThenVerify(byte[] body, Map<String, Object> rule) throws Exception {
        HmacRuntime runtime = configure(rule);
        HttpServerExchange exchange = exchange(body);
        exchange.getRequestHeaders().put(new HttpString("X-Hub-Signature-256"),
                "sha256=" + HexFormat.of().formatHex(sign(body)));
        UnifiedSecurityConfig unifiedConfig = UnifiedSecurityConfig.load();
        Assertions.assertNull(new HmacVerifier().verify(exchange, runtime, unifiedConfig));
        HmacHandler hmac = new HmacHandler(() -> HmacConfig.load(HMAC_CONFIG), () -> unifiedConfig, false);
        hmac.setNext(ignored -> { });
        hmac.handleRequest(exchange);
        Assertions.assertNotNull(exchange.getAttachment(HmacAttachments.AUTHENTICATION_EVIDENCE));
        return new UnifiedSecurityHandler().verifyUnifiedSecurity(exchange);
    }

    private HmacRuntime configure(Map<String, Object> rule) {
        Config.getInstance().clearConfigCache(HMAC_CONFIG);
        Config.getInstance().clearConfigCache(UnifiedSecurityConfig.CONFIG_NAME);
        Config.getInstance().putInConfigCache(UnifiedSecurityConfig.CONFIG_NAME, Map.of(
                "enabled", true, "anonymousPrefixes", List.of(), "pathPrefixAuths", List.of(rule)));
        Config.getInstance().putInConfigCache(HMAC_CONFIG, Map.of(
                "enabled", true, "profiles", Map.of("github", Map.of(
                        "signatureHeader", "X-Hub-Signature-256",
                        "signaturePrefix", "sha256=",
                        "signatureEncoding", "hex",
                        "maxBodyBytes", 1024,
                        "secrets", Map.of("selectorHeader", "", "bySelector", Map.of(),
                                "defaultEnvNames", List.of("HMAC_EVIDENCE_SECRET")),
                        "replay", Map.of("enabled", false)))));
        UnifiedSecurityConfig unifiedConfig = UnifiedSecurityConfig.load();
        return HmacRuntime.compile(HmacConfig.load(HMAC_CONFIG), unifiedConfig,
                name -> "HMAC_EVIDENCE_SECRET".equals(name) ? SECRET : null);
    }

    private HttpServerExchange exchange(byte[] body) {
        HttpServerExchange exchange = new HttpServerExchange(null);
        exchange.setRequestPath("/webhook");
        exchange.setRequestURI("/webhook");
        exchange.setRequestMethod(Methods.POST);
        exchange.putAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY,
                new PooledByteBuffer[]{new TestPooledBuffer(ByteBuffer.wrap(body))});
        return exchange;
    }

    private byte[] sign(byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(body);
    }

    private static final class TestPooledBuffer implements PooledByteBuffer {
        private final ByteBuffer buffer;

        private TestPooledBuffer(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public ByteBuffer getBuffer() {
            return buffer;
        }

        @Override
        public void close() {
        }

        @Override
        public boolean isOpen() {
            return true;
        }
    }
}
