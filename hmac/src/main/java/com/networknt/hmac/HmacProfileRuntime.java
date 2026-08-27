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

import com.networknt.config.ConfigException;
import com.networknt.hmac.config.HmacProfileConfig;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Immutable compiled profile containing non-serializable key material. */
public final class HmacProfileRuntime {
    private static final String JCA_ALGORITHM = "HmacSHA256";
    private final String name;
    private final Set<String> allowedMethods;
    private final String signatureHeader;
    private final String signaturePrefix;
    private final String signatureEncoding;
    private final int maxBodyBytes;
    private final String selectorHeader;
    private final Map<String, List<SecretKeySpec>> selectorKeys;
    private final List<SecretKeySpec> defaultKeys;
    private final boolean replayEnabled;
    private final String replayIdHeader;
    private final Duration replayRetention;

    HmacProfileRuntime(String name, HmacProfileConfig config, Function<String, String> environment) {
        this.name = name;
        this.allowedMethods = Set.copyOf(config.getAllowedMethods());
        this.signatureHeader = config.getSignatureHeader();
        this.signaturePrefix = config.getSignaturePrefix();
        this.signatureEncoding = config.getSignatureEncoding();
        this.maxBodyBytes = config.getMaxBodyBytes();
        this.selectorHeader = config.getSecrets().getSelectorHeader();

        Map<String, List<SecretKeySpec>> compiledSelectors = new LinkedHashMap<>();
        config.getSecrets().getBySelector().forEach((selector, names) ->
                compiledSelectors.put(selector, resolveKeys(name, names, environment)));
        this.selectorKeys = Collections.unmodifiableMap(compiledSelectors);
        this.defaultKeys = resolveKeys(name, config.getSecrets().getDefaultEnvNames(), environment);
        this.replayEnabled = config.getReplay().isEnabled();
        this.replayIdHeader = config.getReplay().getIdHeader();
        this.replayRetention = Duration.ofSeconds(config.getReplay().getRetentionSeconds());
    }

    private static List<SecretKeySpec> resolveKeys(String profileName,
                                                   List<String> environmentNames,
                                                   Function<String, String> environment) {
        List<SecretKeySpec> keys = new ArrayList<>(environmentNames.size());
        for (String environmentName : environmentNames) {
            String secret = environment.apply(environmentName);
            if (secret == null || secret.isEmpty())
                throw new ConfigException("Missing or empty HMAC secret environment variable "
                        + environmentName + " for profile " + profileName + '.');
            try {
                SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), JCA_ALGORITHM);
                Mac mac = Mac.getInstance(JCA_ALGORITHM);
                mac.init(key);
                keys.add(key);
            } catch (GeneralSecurityException e) {
                throw new ConfigException("Unable to initialize HMAC for profile " + profileName + '.');
            }
        }
        return List.copyOf(keys);
    }

    String verify(String selector, byte[] suppliedSignature, List<ByteBuffer> body) throws GeneralSecurityException {
        String namespace;
        List<SecretKeySpec> keys;
        if (selectorHeader.isBlank()) {
            namespace = "shared";
            keys = defaultKeys;
        } else if (selector != null && selectorKeys.containsKey(selector)) {
            namespace = selector;
            keys = selectorKeys.get(selector);
        } else if (!defaultKeys.isEmpty()) {
            namespace = "shared";
            keys = defaultKeys;
        } else {
            return null;
        }

        boolean matched = false;
        for (SecretKeySpec key : keys) {
            Mac mac = Mac.getInstance(JCA_ALGORITHM);
            mac.init(key);
            for (ByteBuffer bytes : body)
                mac.update(bytes.duplicate());
            matched |= MessageDigest.isEqual(mac.doFinal(), suppliedSignature);
        }
        return matched ? namespace : null;
    }

    public String getName() {
        return name;
    }

    public Set<String> getAllowedMethods() {
        return allowedMethods;
    }

    public String getSignatureHeader() {
        return signatureHeader;
    }

    public String getSignaturePrefix() {
        return signaturePrefix;
    }

    public String getSignatureEncoding() {
        return signatureEncoding;
    }

    public int getMaxBodyBytes() {
        return maxBodyBytes;
    }

    public String getSelectorHeader() {
        return selectorHeader;
    }

    public boolean isReplayEnabled() {
        return replayEnabled;
    }

    public String getReplayIdHeader() {
        return replayIdHeader;
    }

    public Duration getReplayRetention() {
        return replayRetention;
    }

    String normalizeReplaySelector(String selector) {
        if (selectorHeader.isBlank())
            return selector == null || selector.isBlank() || "shared".equals(selector) ? "shared" : null;
        if (selector != null && selectorKeys.containsKey(selector))
            return selector;
        if (!defaultKeys.isEmpty() && (selector == null || selector.isBlank() || "shared".equals(selector)))
            return "shared";
        return null;
    }

    @Override
    public String toString() {
        return "HmacProfileRuntime{name='" + name + "', allowedMethods=" + allowedMethods
                + ", signatureHeader='" + signatureHeader + "', maxBodyBytes=" + maxBodyBytes
                + ", replayEnabled=" + replayEnabled + ", keyMaterial=[REDACTED]}";
    }
}
