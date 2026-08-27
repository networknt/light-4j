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
import com.networknt.status.Status;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class HmacVerifierTest {
    private static final byte[] GITHUB_BODY = "Hello, World!".getBytes(StandardCharsets.UTF_8);
    private static final String GITHUB_SECRET = "It's a Secret to Everybody";
    private static final String GITHUB_SIGNATURE =
            "757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17";
    private final List<String> configNames = new ArrayList<>();
    private final HmacVerifier verifier = new HmacVerifier();

    @AfterEach
    void clearConfigs() {
        configNames.forEach(Config.getInstance()::clearConfigCache);
    }

    @Test
    void verifiesPublishedGithubVectorAcrossBuffersWithoutAdvancingPositions() {
        Context context = context("github-vector", githubProfile(13),
                Map.of("GITHUB_CURRENT", GITHUB_SECRET), hmacRule(false, false));
        HttpServerExchange exchange = exchange("/github-webhook", "Hello, ", "World!");
        exchange.getRequestHeaders().put(new HttpString("X-GitHub-Hook-ID"), "12345678");
        exchange.getRequestHeaders().put(new HttpString("X-GitHub-Delivery"), "delivery-1");
        exchange.getRequestHeaders().put(new HttpString("X-Hub-Signature-256"),
                "sha256=" + GITHUB_SIGNATURE);
        int[] positions = positions(exchange);

        Status status = verifier.verify(exchange, context.runtime, context.unifiedConfig);

        Assertions.assertNull(status);
        Assertions.assertArrayEquals(positions, positions(exchange));
        HmacPendingAuthentication pending = exchange.getAttachment(HmacAttachments.PENDING_AUTHENTICATION);
        Assertions.assertNotNull(pending);
        Assertions.assertEquals("github", pending.getProfileName());
        Assertions.assertEquals(13, pending.getBodyLength());
        Assertions.assertSame(context.runtime, pending.getRuntime());
        Assertions.assertFalse(pending.toString().contains("12345678"));
        Assertions.assertFalse(pending.toString().contains("delivery-1"));
    }

    @Test
    void rejectsAlteredBytesMalformedHeadersWrongMethodEncodingAndExcessByte() {
        Context context = context("github-rejections", githubProfile(13),
                Map.of("GITHUB_CURRENT", GITHUB_SECRET), hmacRule(false, false));

        HttpServerExchange altered = signedExchange("Hello, world!", GITHUB_SIGNATURE);
        Assertions.assertEquals(401, verify(altered, context).getStatusCode());

        HttpServerExchange duplicateSignature = signedExchange("Hello, World!", GITHUB_SIGNATURE);
        duplicateSignature.getRequestHeaders().add(new HttpString("X-Hub-Signature-256"),
                "sha256=" + GITHUB_SIGNATURE);
        Assertions.assertEquals(401, verify(duplicateSignature, context).getStatusCode());

        HttpServerExchange malformed = signedExchange("Hello, World!", "not-hex");
        Assertions.assertEquals(401, verify(malformed, context).getStatusCode());

        HttpServerExchange compressed = signedExchange("Hello, World!", GITHUB_SIGNATURE);
        compressed.getRequestHeaders().put(Headers.CONTENT_ENCODING, "gzip");
        Assertions.assertEquals(415, verify(compressed, context).getStatusCode());

        HttpServerExchange wrongMethod = signedExchange("Hello, World!", GITHUB_SIGNATURE);
        wrongMethod.setRequestMethod(Methods.GET);
        Assertions.assertEquals(405, verify(wrongMethod, context).getStatusCode());

        HttpServerExchange tooLarge = signedExchange("Hello, World!!", GITHUB_SIGNATURE);
        Assertions.assertEquals(413, verify(tooLarge, context).getStatusCode());
    }

    @Test
    void verifiesEveryRotationCandidateAndSupportsBase64() throws Exception {
        Map<String, Object> profile = githubProfile(1024);
        secrets(profile).put("bySelector", Map.of("12345678", List.of("CURRENT", "PREVIOUS")));
        profile.put("signatureEncoding", "base64");
        profile.put("signaturePrefix", "");
        String previous = "previous-secret-value-for-rotation";
        Context context = context("github-rotation", profile,
                Map.of("CURRENT", "current-secret-value-for-rotation", "PREVIOUS", previous),
                hmacRule(false, false));
        String signature = Base64.getEncoder().encodeToString(sign(previous, GITHUB_BODY));
        HttpServerExchange exchange = signedExchange("Hello, World!", signature);
        exchange.getRequestHeaders().put(new HttpString("X-Hub-Signature-256"), signature);

        Assertions.assertNull(verifier.verify(exchange, context.runtime, context.unifiedConfig));
    }

    @Test
    void reloadCreatesANewRuntimeWhileInflightRuntimeRemainsUsable() throws Exception {
        Map<String, Object> oldProfile = sharedProfile("OLD_SECRET");
        Map<String, Object> newProfile = sharedProfile("NEW_SECRET");
        HmacConfig oldConfig = hmacConfig("hmac-reload-old", oldProfile);
        HmacConfig newConfig = hmacConfig("hmac-reload-new", newProfile);
        UnifiedSecurityConfig unified = unifiedConfig("unified-reload", hmacRule(false, false));
        HmacRuntimeManager manager = manager(name -> Map.of(
                "OLD_SECRET", "old-secret-value-that-remains-pinned",
                "NEW_SECRET", "new-secret-value-used-after-reload").get(name));

        HmacRuntime oldRuntime = manager.get(oldConfig, unified);
        HmacRuntime newRuntime = manager.get(newConfig, unified);
        HmacConfig invalidConfig = hmacConfig("hmac-reload-invalid", sharedProfile("MISSING_SECRET"));

        Assertions.assertNotSame(oldRuntime, newRuntime);
        Assertions.assertThrows(Exception.class, () -> manager.get(invalidConfig, unified));
        Assertions.assertSame(newRuntime, manager.get(newConfig, unified));
        Assertions.assertNull(verifier.verify(sharedSignedExchange("old-secret-value-that-remains-pinned"),
                oldRuntime, unified));
        Assertions.assertNull(verifier.verify(sharedSignedExchange("new-secret-value-used-after-reload"),
                newRuntime, unified));
        Assertions.assertEquals(401, verifier.verify(sharedSignedExchange("old-secret-value-that-remains-pinned"),
                newRuntime, unified).getStatusCode());
    }

    @Test
    void runtimeRepresentationsAndFailuresNeverExposeSecretValues() {
        Context context = context("hmac-redaction", sharedProfile("SECRET_ENV"),
                Map.of("SECRET_ENV", "super-secret-value"), hmacRule(false, false));
        Assertions.assertFalse(context.runtime.toString().contains("super-secret-value"));
        Assertions.assertFalse(context.runtime.getProfile("github").toString().contains("super-secret-value"));

        HmacConfig missingConfig = hmacConfig("hmac-missing-env", sharedProfile("MISSING_ENV"));
        UnifiedSecurityConfig unified = unifiedConfig("unified-missing-env", hmacRule(false, false));
        Exception exception = Assertions.assertThrows(Exception.class,
                () -> manager(name -> null).get(missingConfig, unified));
        Assertions.assertFalse(exception.getMessage().contains("super-secret-value"));
        Assertions.assertThrows(Exception.class,
                () -> manager(name -> "").get(missingConfig, unified));
    }

    @Test
    void authenticatesExactUtf8AndEmptyBytesAndRejectsEquivalentButDifferentJson() throws Exception {
        String secret = "shared-secret-for-byte-exact-tests";
        Context context = context("exact-bytes", sharedProfile("SECRET_ENV"),
                Map.of("SECRET_ENV", secret), hmacRule(false, false));

        String utf8 = "Héllo, 世界";
        HttpServerExchange utf8Exchange = exchange("/github-webhook", utf8);
        utf8Exchange.getRequestHeaders().put(new HttpString("X-Hub-Signature-256"),
                "sha256=" + HexFormat.of().formatHex(sign(secret, utf8.getBytes(StandardCharsets.UTF_8))));
        Assertions.assertNull(verifier.verify(utf8Exchange, context.runtime, context.unifiedConfig));

        HttpServerExchange empty = exchange("/github-webhook", "");
        empty.getRequestHeaders().put(new HttpString("X-Hub-Signature-256"),
                "sha256=" + HexFormat.of().formatHex(sign(secret, new byte[0])));
        Assertions.assertNull(verifier.verify(empty, context.runtime, context.unifiedConfig));

        byte[] compactJson = "{\"a\":1}".getBytes(StandardCharsets.UTF_8);
        HttpServerExchange spacedJson = exchange("/github-webhook", "{ \"a\": 1 }");
        spacedJson.getRequestHeaders().put(new HttpString("X-Hub-Signature-256"),
                "sha256=" + HexFormat.of().formatHex(sign(secret, compactJson)));
        Assertions.assertEquals(401,
                verifier.verify(spacedJson, context.runtime, context.unifiedConfig).getStatusCode());
    }

    @Test
    void rejectsMissingUnknownOrDuplicateSelectorAndReplayHeaders() {
        Context context = context("header-cardinality", githubProfile(1024),
                Map.of("GITHUB_CURRENT", GITHUB_SECRET), hmacRule(false, false));

        HttpServerExchange missingSelector = signedExchange("Hello, World!", GITHUB_SIGNATURE);
        missingSelector.getRequestHeaders().remove(new HttpString("X-GitHub-Hook-ID"));
        Assertions.assertEquals(401, verify(missingSelector, context).getStatusCode());

        HttpServerExchange unknownSelector = signedExchange("Hello, World!", GITHUB_SIGNATURE);
        unknownSelector.getRequestHeaders().put(new HttpString("X-GitHub-Hook-ID"), "unknown");
        Assertions.assertEquals(401, verify(unknownSelector, context).getStatusCode());

        HttpServerExchange duplicateSelector = signedExchange("Hello, World!", GITHUB_SIGNATURE);
        duplicateSelector.getRequestHeaders().add(new HttpString("X-GitHub-Hook-ID"), "12345678");
        Assertions.assertEquals(401, verify(duplicateSelector, context).getStatusCode());

        HttpServerExchange duplicateReplay = signedExchange("Hello, World!", GITHUB_SIGNATURE);
        duplicateReplay.getRequestHeaders().add(new HttpString("X-GitHub-Delivery"), "delivery-2");
        Assertions.assertEquals(401, verify(duplicateReplay, context).getStatusCode());
    }

    private Status verify(HttpServerExchange exchange, Context context) {
        return verifier.verify(exchange, context.runtime, context.unifiedConfig);
    }

    private Context context(String suffix,
                            Map<String, Object> profile,
                            Map<String, String> environment,
                            Map<String, Object> rule) {
        HmacConfig hmac = hmacConfig("hmac-" + suffix, profile);
        UnifiedSecurityConfig unified = unifiedConfig("unified-" + suffix, rule);
        HmacRuntime runtime = manager(environment::get).get(hmac, unified);
        return new Context(runtime, unified);
    }

    private HmacRuntimeManager manager(java.util.function.Function<String, String> environment) {
        LocalWebhookReplayStore store = new LocalWebhookReplayStore();
        return new HmacRuntimeManager(environment, () -> new WebhookReplayStore[]{store});
    }

    private HmacConfig hmacConfig(String name, Map<String, Object> profile) {
        cache(name, Map.of("enabled", true, "profiles", Map.of("github", profile)));
        return HmacConfig.load(name);
    }

    private UnifiedSecurityConfig unifiedConfig(String name, Map<String, Object> rule) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("enabled", true);
        root.put("anonymousPrefixes", List.of());
        root.put("pathPrefixAuths", List.of(rule));
        cache(name, root);
        return UnifiedSecurityConfig.load(name);
    }

    private void cache(String name, Map<String, Object> value) {
        configNames.add(name);
        Config.getInstance().putInConfigCache(name, value);
    }

    private Map<String, Object> githubProfile(int maxBodyBytes) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("signatureHeader", "X-Hub-Signature-256");
        profile.put("signaturePrefix", "sha256=");
        profile.put("signatureEncoding", "hex");
        profile.put("maxBodyBytes", maxBodyBytes);
        Map<String, Object> secrets = new LinkedHashMap<>();
        secrets.put("selectorHeader", "X-GitHub-Hook-ID");
        secrets.put("bySelector", Map.of("12345678", List.of("GITHUB_CURRENT")));
        secrets.put("defaultEnvNames", List.of());
        profile.put("secrets", secrets);
        profile.put("replay", Map.of("enabled", true, "idHeader", "X-GitHub-Delivery"));
        return profile;
    }

    private Map<String, Object> sharedProfile(String environmentName) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("signatureHeader", "X-Hub-Signature-256");
        profile.put("signaturePrefix", "sha256=");
        profile.put("secrets", Map.of(
                "selectorHeader", "",
                "bySelector", Map.of(),
                "defaultEnvNames", List.of(environmentName)));
        profile.put("replay", Map.of("enabled", false));
        return profile;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> secrets(Map<String, Object> profile) {
        return (Map<String, Object>) profile.get("secrets");
    }

    private Map<String, Object> hmacRule(boolean jwt, boolean apikey) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("prefix", "/github-webhook");
        rule.put("hmacProfile", "github");
        rule.put("jwt", jwt);
        rule.put("apikey", apikey);
        return rule;
    }

    private HttpServerExchange signedExchange(String body, String signature) {
        HttpServerExchange exchange = exchange("/github-webhook", body);
        exchange.getRequestHeaders().put(new HttpString("X-GitHub-Hook-ID"), "12345678");
        exchange.getRequestHeaders().put(new HttpString("X-GitHub-Delivery"), "delivery-1");
        exchange.getRequestHeaders().put(new HttpString("X-Hub-Signature-256"), "sha256=" + signature);
        return exchange;
    }

    private HttpServerExchange sharedSignedExchange(String secret) throws Exception {
        HttpServerExchange exchange = exchange("/github-webhook", "Hello, World!");
        exchange.getRequestHeaders().put(new HttpString("X-Hub-Signature-256"),
                "sha256=" + HexFormat.of().formatHex(sign(secret, GITHUB_BODY)));
        return exchange;
    }

    private HttpServerExchange exchange(String path, String... chunks) {
        HttpServerExchange exchange = new HttpServerExchange(null);
        exchange.setRequestPath(path);
        exchange.setRequestURI(path);
        exchange.setRequestMethod(Methods.POST);
        PooledByteBuffer[] buffers = new PooledByteBuffer[chunks.length];
        for (int i = 0; i < chunks.length; i++)
            buffers[i] = new TestPooledBuffer(ByteBuffer.wrap(chunks[i].getBytes(StandardCharsets.UTF_8)));
        exchange.putAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY, buffers);
        return exchange;
    }

    private int[] positions(HttpServerExchange exchange) {
        PooledByteBuffer[] buffers = exchange.getAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY);
        int[] positions = new int[buffers.length];
        for (int i = 0; i < buffers.length; i++)
            positions[i] = buffers[i].getBuffer().position();
        return positions;
    }

    private static byte[] sign(String secret, byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(body);
    }

    private record Context(HmacRuntime runtime, UnifiedSecurityConfig unifiedConfig) {
    }

    private static final class TestPooledBuffer implements PooledByteBuffer {
        private final ByteBuffer buffer;
        private boolean open = true;

        private TestPooledBuffer(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public ByteBuffer getBuffer() {
            return buffer;
        }

        @Override
        public void close() {
            open = false;
        }

        @Override
        public boolean isOpen() {
            return open;
        }
    }
}
