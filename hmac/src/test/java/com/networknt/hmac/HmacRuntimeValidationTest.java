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
import com.networknt.hmac.config.HmacConfig;
import com.networknt.handler.RequestInjectionConfig;
import com.networknt.handler.RequestInterceptor;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.reqtrans.RequestTransformerConfig;
import com.networknt.security.UnifiedPathPrefixAuth;
import com.networknt.security.UnifiedSecurityConfig;
import com.networknt.server.ServerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

class HmacRuntimeValidationTest {
    private final List<String> configNames = new ArrayList<>();

    @AfterEach
    void clearConfigs() {
        configNames.forEach(Config.getInstance()::clearConfigCache);
    }

    @Test
    void acceptsHmacOnlyHmacJwtAndHmacApiKeyRules() {
        Assertions.assertNotNull(compile("only", rule(false, false, false, false, false)));
        Assertions.assertNotNull(compile("jwt", rule(true, false, false, false, false)));
        Assertions.assertNotNull(compile("apikey", rule(false, true, false, false, false)));
    }

    @Test
    void rejectsUnsupportedFactorCombinationsAndUnknownProfiles() {
        Assertions.assertThrows(ConfigException.class,
                () -> compile("basic", rule(false, false, true, false, false)));
        Assertions.assertThrows(ConfigException.class,
                () -> compile("sjwt", rule(false, false, false, true, false)));
        Assertions.assertThrows(ConfigException.class,
                () -> compile("swt", rule(false, false, false, false, true)));
        Assertions.assertThrows(ConfigException.class,
                () -> compile("jwt-apikey", rule(true, true, false, false, false)));

        Map<String, Object> unknown = rule(false, false, false, false, false);
        unknown.put("hmacProfile", "missing");
        Assertions.assertThrows(ConfigException.class, () -> compile("unknown", unknown));
    }

    @Test
    void rejectsShadowedAndAnonymousOverlappingHmacRules() {
        Map<String, Object> hmac = rule(false, false, false, false, false);
        Assertions.assertThrows(ConfigException.class,
                () -> unified("unified-shadowed",
                        List.of(Map.of("prefix", "/github"), hmac), List.of()));

        Assertions.assertThrows(ConfigException.class,
                () -> unified("unified-anonymous",
                        List.of(hmac), List.of("/github")));

        UnifiedSecurityConfig mutated = unified("unified-mutated-shadow",
                List.of(hmac), List.of());
        UnifiedPathPrefixAuth earlierSpecific = new UnifiedPathPrefixAuth();
        earlierSpecific.setPrefix("/github-webhook/v2");
        earlierSpecific.setApikey(true);
        mutated.getPathPrefixAuths().add(0, earlierSpecific);
        Assertions.assertThrows(ConfigException.class,
                () -> runtime("hmac-mutated-shadow", mutated));
    }

    @Test
    void validatesExactRequestInjectionCoverageLimitAndCapacity() {
        Map<String, Object> rule = rule(false, false, false, false, false);
        UnifiedSecurityConfig unified = unified("unified-injection", List.of(rule), List.of());
        HmacRuntime runtime = runtime("hmac-injection", unified);
        ServerConfig server = server("server-injection", 1024);

        Assertions.assertDoesNotThrow(() -> HmacIntegrationValidator.validate(runtime, unified,
                injection("request-injection-valid", true, List.of("/github"), 16384, 16777216), server));
        Assertions.assertThrows(ConfigException.class, () -> HmacIntegrationValidator.validate(runtime, unified,
                injection("request-injection-disabled", false, List.of("/github"), 16384, 16777216), server));
        Assertions.assertThrows(ConfigException.class, () -> HmacIntegrationValidator.validate(runtime, unified,
                injection("request-injection-limit", true, List.of("/github"), 16384, 1024), server));
        Assertions.assertThrows(ConfigException.class, () -> HmacIntegrationValidator.validate(runtime, unified,
                injection("request-injection-capacity", true, List.of("/github"), 1, 16777216), server));
        Assertions.assertThrows(ConfigException.class, () -> HmacIntegrationValidator.validate(runtime, unified,
                injection("request-injection-coverage", true, List.of("/other"), 16384, 16777216), server));

        String transformerName = "request-transformer-overlap";
        cache(transformerName, Map.of("enabled", true,
                "appliedPathPrefixes", List.of("/github")));
        RequestTransformerConfig transformer = RequestTransformerConfig.load(transformerName);
        Assertions.assertThrows(ConfigException.class, () -> HmacIntegrationValidator.validate(runtime, unified,
                injection("request-injection-transformer", true, List.of("/github"), 16384, 16777216),
                server, transformer));
        Assertions.assertThrows(ConfigException.class,
                () -> HmacIntegrationValidator.validateInterceptorOrder(
                        new RequestInterceptor[]{new NoopInterceptor()}));
    }

    @Test
    void requiresExactlyOneValidReplayStoreAndPinsItAcrossConfigReload() {
        UnifiedSecurityConfig unified = unified("unified-replay-binding",
                List.of(rule(false, false, false, false, false)), List.of());
        HmacConfig first = replayConfig("hmac-replay-binding-first");

        Assertions.assertThrows(ConfigException.class, () -> new HmacRuntimeManager(name -> "secret-value",
                () -> null).get(first, unified));
        Assertions.assertThrows(ConfigException.class, () -> new HmacRuntimeManager(name -> "secret-value",
                () -> new WebhookReplayStore[]{new LocalWebhookReplayStore(), new LocalWebhookReplayStore()})
                .get(first, unified));
        Assertions.assertThrows(ConfigException.class, () -> new HmacRuntimeManager(name -> "secret-value",
                () -> new WebhookReplayStore[]{new InvalidReplayStore()}).get(first, unified));

        LocalWebhookReplayStore pinned = new LocalWebhookReplayStore();
        LocalWebhookReplayStore replacement = new LocalWebhookReplayStore();
        AtomicInteger resolutions = new AtomicInteger();
        HmacRuntimeManager manager = new HmacRuntimeManager(name -> "secret-value", () ->
                resolutions.getAndIncrement() == 0
                        ? new WebhookReplayStore[]{pinned}
                        : new WebhookReplayStore[]{replacement});
        Assertions.assertSame(pinned, manager.get(first, unified).getReplayStore());
        Assertions.assertSame(pinned, manager.get(replayConfig("hmac-replay-binding-second"), unified).getReplayStore());
        Assertions.assertEquals(1, resolutions.get());
    }

    private HmacRuntime compile(String suffix, Map<String, Object> rule) {
        UnifiedSecurityConfig unified = unified("unified-" + suffix, List.of(rule), List.of());
        return runtime("hmac-" + suffix, unified);
    }

    private HmacRuntime runtime(String hmacName, UnifiedSecurityConfig unified) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("signatureHeader", "X-Hub-Signature-256");
        profile.put("secrets", Map.of(
                "selectorHeader", "",
                "bySelector", Map.of(),
                "defaultEnvNames", List.of("SECRET_ENV")));
        profile.put("replay", Map.of("enabled", false));
        cache(hmacName, Map.of("enabled", true, "profiles", Map.of("github", profile)));
        HmacConfig hmac = HmacConfig.load(hmacName);
        LocalWebhookReplayStore store = new LocalWebhookReplayStore();
        return new HmacRuntimeManager(name -> "secret-value",
                () -> new WebhookReplayStore[]{store}).get(hmac, unified);
    }

    private HmacConfig replayConfig(String name) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("signatureHeader", "X-Hub-Signature-256");
        profile.put("secrets", Map.of(
                "selectorHeader", "",
                "bySelector", Map.of(),
                "defaultEnvNames", List.of("SECRET_ENV")));
        profile.put("replay", Map.of("enabled", true, "idHeader", "X-GitHub-Delivery"));
        cache(name, Map.of("enabled", true, "profiles", Map.of("github", profile)));
        return HmacConfig.load(name);
    }

    private UnifiedSecurityConfig unified(String name,
                                          List<Map<String, Object>> rules,
                                          List<String> anonymousPrefixes) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("enabled", true);
        root.put("anonymousPrefixes", anonymousPrefixes);
        root.put("pathPrefixAuths", rules);
        cache(name, root);
        return UnifiedSecurityConfig.load(name);
    }

    private Map<String, Object> rule(boolean jwt,
                                     boolean apikey,
                                     boolean basic,
                                     boolean sjwt,
                                     boolean swt) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("prefix", "/github-webhook");
        rule.put("hmacProfile", "github");
        rule.put("jwt", jwt);
        rule.put("apikey", apikey);
        rule.put("basic", basic);
        rule.put("sjwt", sjwt);
        rule.put("swt", swt);
        return rule;
    }

    private void cache(String name, Map<String, Object> value) {
        configNames.add(name);
        Config.getInstance().putInConfigCache(name, value);
    }

    private RequestInjectionConfig injection(String name,
                                             boolean enabled,
                                             List<String> prefixes,
                                             int maxBuffers,
                                             int maxBodyBytes) {
        cache(name, Map.of(
                "enabled", enabled,
                "appliedBodyInjectionPathPrefixes", prefixes,
                "maxBuffers", maxBuffers,
                "maxBodyBytes", maxBodyBytes));
        return RequestInjectionConfig.load(name);
    }

    private ServerConfig server(String name, int bufferSize) {
        cache(name, Map.of("bufferSize", bufferSize));
        return ServerConfig.load(name);
    }

    private static final class InvalidReplayStore implements WebhookReplayStore {
        @Override public CompletionStage<ReserveOutcome> reserve(WebhookReplayKey key, Duration retention) {
            return CompletableFuture.completedFuture(ReserveOutcome.Duplicate.INSTANCE);
        }
        @Override public CompletionStage<Void> release(ReplayReservation reservation) {
            return CompletableFuture.completedFuture(null);
        }
        @Override public CompletionStage<Boolean> forceRemove(WebhookReplayKey key) {
            return CompletableFuture.completedFuture(false);
        }
        @Override public ReplayStoreScope getScope() { return ReplayStoreScope.LOCAL; }
        @Override public ReplayStoreSummary getSummary() {
            return new ReplayStoreSummary(getClass().getName(), getScope(), 0, 1);
        }
        @Override public void validate() { throw new IllegalStateException("invalid provider"); }
    }

    private static final class NoopInterceptor implements RequestInterceptor {
        @Override public void handleRequest(HttpServerExchange exchange) { }
        @Override public boolean isRequiredContent() { return false; }
        @Override public HttpHandler getNext() { return null; }
        @Override public MiddlewareHandler setNext(HttpHandler next) { return this; }
        @Override public boolean isEnabled() { return true; }
    }
}
