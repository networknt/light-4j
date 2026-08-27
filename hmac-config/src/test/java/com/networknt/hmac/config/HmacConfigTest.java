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

package com.networknt.hmac.config;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class HmacConfigTest {
    private final List<String> configNames = new ArrayList<>();

    @AfterEach
    void clearConfigs() {
        configNames.forEach(Config.getInstance()::clearConfigCache);
    }

    @Test
    void loadsAndNormalizesASecretFreeProfile() {
        HmacConfig config = load("hmac-valid", profile());
        HmacProfileConfig profile = config.getProfiles().get("github");

        Assertions.assertTrue(config.isEnabled());
        Assertions.assertEquals(List.of("POST", "PUT"), profile.getAllowedMethods());
        Assertions.assertEquals("hex", profile.getSignatureEncoding());
        Assertions.assertEquals(HmacProfileConfig.DEFAULT_MAX_BODY_BYTES, profile.getMaxBodyBytes());
        Assertions.assertEquals(HmacReplayConfig.DEFAULT_RETENTION_SECONDS,
                profile.getReplay().getRetentionSeconds());
        Assertions.assertFalse(config.toString().contains("GITHUB_CURRENT"));
    }

    @Test
    void rejectsUnsupportedPrimitiveMethodAndEncoding() {
        Map<String, Object> invalidAlgorithm = profile();
        invalidAlgorithm.put("algorithm", "hmacSha512");
        Assertions.assertThrows(ConfigException.class, () -> load("hmac-invalid-algorithm", invalidAlgorithm));

        Map<String, Object> invalidMethod = profile();
        invalidMethod.put("allowedMethods", List.of("DELETE"));
        Assertions.assertThrows(ConfigException.class, () -> load("hmac-invalid-method", invalidMethod));

        Map<String, Object> invalidEncoding = profile();
        invalidEncoding.put("signatureEncoding", "utf8");
        Assertions.assertThrows(ConfigException.class, () -> load("hmac-invalid-encoding", invalidEncoding));
    }

    @Test
    void rejectsUnsafeSecretAndReplayConfiguration() {
        Map<String, Object> tooManySecrets = profile();
        secrets(tooManySecrets).put("defaultEnvNames", List.of("ONE", "TWO", "THREE"));
        Assertions.assertThrows(ConfigException.class, () -> load("hmac-too-many-secrets", tooManySecrets));

        Map<String, Object> selectorWithoutHeader = profile();
        secrets(selectorWithoutHeader).put("selectorHeader", "");
        Assertions.assertThrows(ConfigException.class, () -> load("hmac-selector-header", selectorWithoutHeader));

        Map<String, Object> invalidReplay = profile();
        replay(invalidReplay).put("idHeader", "");
        Assertions.assertThrows(ConfigException.class, () -> load("hmac-replay-header", invalidReplay));
    }

    @Test
    void schemaDescribesSelectorValuesAsBoundedStringArrays() throws Exception {
        try (var input = getClass().getResourceAsStream("/config/hmac-schema.json")) {
            Assertions.assertNotNull(input);
            var selectorSchema = Config.getInstance().getMapper().readTree(input)
                    .path("properties").path("profiles").path("additionalProperties")
                    .path("properties").path("secrets").path("properties")
                    .path("bySelector").path("additionalProperties");
            Assertions.assertEquals("array", selectorSchema.path("type").asText());
            Assertions.assertEquals(1, selectorSchema.path("minItems").asInt());
            Assertions.assertEquals(2, selectorSchema.path("maxItems").asInt());
            Assertions.assertEquals("string", selectorSchema.path("items").path("type").asText());
        }
    }

    private HmacConfig load(String name, Map<String, Object> profile) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("enabled", true);
        root.put("profiles", Map.of("github", profile));
        configNames.add(name);
        Config.getInstance().putInConfigCache(name, root);
        return HmacConfig.load(name);
    }

    private Map<String, Object> profile() {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("signedInput", "rawBody");
        profile.put("algorithm", "hmacSha256");
        profile.put("allowedMethods", List.of("post", "PUT"));
        profile.put("signatureHeader", "X-Hub-Signature-256");
        profile.put("signaturePrefix", "sha256=");
        profile.put("signatureEncoding", "HEX");
        Map<String, Object> secrets = new LinkedHashMap<>();
        secrets.put("selectorHeader", "X-GitHub-Hook-ID");
        secrets.put("bySelector", Map.of("123", List.of("GITHUB_CURRENT", "GITHUB_PREVIOUS")));
        secrets.put("defaultEnvNames", List.of());
        profile.put("secrets", secrets);
        Map<String, Object> replay = new LinkedHashMap<>();
        replay.put("enabled", true);
        replay.put("idHeader", "X-GitHub-Delivery");
        profile.put("replay", replay);
        return profile;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> secrets(Map<String, Object> profile) {
        return (Map<String, Object>) profile.get("secrets");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> replay(Map<String, Object> profile) {
        return (Map<String, Object>) profile.get("replay");
    }
}
