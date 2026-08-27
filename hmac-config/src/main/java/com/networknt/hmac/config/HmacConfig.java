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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.MapField;
import com.networknt.config.schema.OutputFormat;
import com.networknt.server.ModuleRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Secret-free HMAC webhook profile configuration loaded from {@code hmac.yml}. */
@ConfigSchema(
        configKey = HmacConfig.CONFIG_NAME,
        configName = HmacConfig.CONFIG_NAME,
        configDescription = "Raw-body HMAC webhook authentication configuration.",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
public final class HmacConfig {
    public static final String CONFIG_NAME = "hmac";
    private static final Set<String> ALLOWED_METHODS = Set.of("POST", "PUT", "PATCH");

    @BooleanField(
            configFieldName = "enabled",
            externalizedKeyName = "enabled",
            defaultValue = "false",
            description = "Enable raw-body HMAC webhook authentication."
    )
    private boolean enabled;

    @MapField(
            configFieldName = "profiles",
            externalizedKeyName = "profiles",
            additionalProperties = true,
            valueType = HmacProfileConfig.class,
            description = "Named HMAC verification profiles. Secret values are referenced only by environment-variable name."
    )
    private Map<String, HmacProfileConfig> profiles = Collections.emptyMap();

    private static volatile HmacConfig instance;
    private final Map<String, Object> mappedConfig;

    private HmacConfig(String configName) {
        mappedConfig = Config.getInstance().getJsonMapConfig(configName);
        if (mappedConfig != null) {
            Object enabledValue = mappedConfig.get("enabled");
            if (enabledValue != null)
                enabled = Config.loadBooleanValue("enabled", enabledValue);
            profiles = parseProfiles(mappedConfig.get("profiles"));
        }
        validate();
    }

    public static HmacConfig load() {
        return load(CONFIG_NAME);
    }

    public static HmacConfig load(String configName) {
        if (!CONFIG_NAME.equals(configName))
            return new HmacConfig(configName);

        Map<String, Object> current = Config.getInstance().getJsonMapConfig(configName);
        if (instance != null && instance.mappedConfig == current)
            return instance;
        synchronized (HmacConfig.class) {
            if (instance != null && instance.mappedConfig == current)
                return instance;
            HmacConfig replacement = new HmacConfig(configName);
            instance = replacement;
            ModuleRegistry.registerModule(CONFIG_NAME, HmacConfig.class.getName(),
                    Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
            return replacement;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, HmacProfileConfig> getProfiles() {
        return profiles;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private Map<String, HmacProfileConfig> parseProfiles(Object value) {
        if (value == null)
            return Collections.emptyMap();
        ObjectMapper mapper = Config.getInstance().getMapper();
        Map<String, Object> values;
        try {
            if (value instanceof String text) {
                values = mapper.readValue(text, new TypeReference<>() { });
            } else if (value instanceof Map<?, ?> map) {
                values = new LinkedHashMap<>();
                map.forEach((key, profile) -> values.put(String.valueOf(key), profile));
            } else {
                throw new ConfigException("hmac.profiles must be a map of named profile objects.");
            }
            Map<String, HmacProfileConfig> parsed = new LinkedHashMap<>();
            values.forEach((name, profile) -> parsed.put(name, mapper.convertValue(profile, HmacProfileConfig.class)));
            return Collections.unmodifiableMap(parsed);
        } catch (ConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigException("Unable to parse hmac.profiles: " + e.getMessage());
        }
    }

    private void validate() {
        if (enabled && profiles.isEmpty())
            throw new ConfigException("hmac.profiles must not be empty when HMAC is enabled.");
        profiles.forEach(this::validateProfile);
    }

    private void validateProfile(String name, HmacProfileConfig profile) {
        String path = "hmac.profiles." + name;
        requireText(name, "hmac profile name");
        if (profile == null)
            throw new ConfigException(path + " must be an object.");
        if (!"rawBody".equals(profile.getSignedInput()))
            throw new ConfigException(path + ".signedInput must be rawBody.");
        if (!"hmacSha256".equals(profile.getAlgorithm()))
            throw new ConfigException(path + ".algorithm must be hmacSha256.");

        List<String> methods = profile.getAllowedMethods();
        if (methods == null || methods.isEmpty())
            methods = List.of("POST");
        LinkedHashSet<String> normalizedMethods = new LinkedHashSet<>();
        for (String method : methods) {
            String normalized = requireText(method, path + ".allowedMethods").toUpperCase(Locale.ROOT);
            if (!ALLOWED_METHODS.contains(normalized))
                throw new ConfigException(path + ".allowedMethods contains unsupported method " + normalized + '.');
            if (!normalizedMethods.add(normalized))
                throw new ConfigException(path + ".allowedMethods contains a duplicate method.");
        }
        profile.setAllowedMethods(List.copyOf(normalizedMethods));

        validateHeader(profile.getSignatureHeader(), path + ".signatureHeader", true);
        if (profile.getSignaturePrefix() == null)
            profile.setSignaturePrefix("");
        for (int i = 0; i < profile.getSignaturePrefix().length(); i++) {
            if (profile.getSignaturePrefix().charAt(i) < 32)
                throw new ConfigException(path + ".signaturePrefix must not contain control characters.");
        }
        String encoding = requireText(profile.getSignatureEncoding(), path + ".signatureEncoding").toLowerCase(Locale.ROOT);
        if (!"hex".equals(encoding) && !"base64".equals(encoding))
            throw new ConfigException(path + ".signatureEncoding must be hex or base64.");
        profile.setSignatureEncoding(encoding);
        if (profile.getMaxBodyBytes() <= 0)
            throw new ConfigException(path + ".maxBodyBytes must be positive.");

        validateSecrets(path, profile.getSecrets());
        validateReplay(path, profile.getReplay());
    }

    private void validateSecrets(String path, HmacSecretsConfig secrets) {
        if (secrets == null)
            throw new ConfigException(path + ".secrets must be configured.");
        String selectorHeader = secrets.getSelectorHeader();
        if (selectorHeader == null)
            selectorHeader = "";
        if (!selectorHeader.isBlank())
            validateHeader(selectorHeader, path + ".secrets.selectorHeader", true);
        secrets.setSelectorHeader(selectorHeader);

        Map<String, List<String>> bySelector = secrets.getBySelector();
        if (bySelector == null)
            bySelector = Collections.emptyMap();
        Map<String, List<String>> normalizedSelectors = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : bySelector.entrySet()) {
            String selector = requireText(entry.getKey(), path + ".secrets.bySelector key");
            if (!selector.equals(trimOws(selector)))
                throw new ConfigException(path + ".secrets.bySelector keys must not contain surrounding HTTP whitespace.");
            normalizedSelectors.put(selector, validateEnvironmentNames(
                    entry.getValue(), path + ".secrets.bySelector." + selector));
        }
        if (selectorHeader.isBlank() && !normalizedSelectors.isEmpty())
            throw new ConfigException(path + ".secrets.selectorHeader is required when bySelector is configured.");
        secrets.setBySelector(Collections.unmodifiableMap(normalizedSelectors));

        List<String> defaults = secrets.getDefaultEnvNames() == null || secrets.getDefaultEnvNames().isEmpty()
                ? Collections.emptyList()
                : validateEnvironmentNames(secrets.getDefaultEnvNames(), path + ".secrets.defaultEnvNames");
        secrets.setDefaultEnvNames(defaults);
        if (normalizedSelectors.isEmpty() && defaults.isEmpty())
            throw new ConfigException(path + ".secrets must configure bySelector or defaultEnvNames.");
    }

    private List<String> validateEnvironmentNames(List<String> names, String path) {
        if (names == null || names.isEmpty() || names.size() > 2)
            throw new ConfigException(path + " must contain one or two environment-variable names.");
        List<String> copy = new ArrayList<>(names.size());
        Set<String> unique = new LinkedHashSet<>();
        for (String name : names) {
            String normalized = requireText(name, path);
            if (!unique.add(normalized))
                throw new ConfigException(path + " contains a duplicate environment-variable name.");
            copy.add(normalized);
        }
        return List.copyOf(copy);
    }

    private void validateReplay(String path, HmacReplayConfig replay) {
        if (replay == null)
            throw new ConfigException(path + ".replay must be configured.");
        if (replay.getRetentionSeconds() <= 0)
            throw new ConfigException(path + ".replay.retentionSeconds must be positive.");
        if (replay.isEnabled()) {
            validateHeader(replay.getIdHeader(), path + ".replay.idHeader", true);
        }
    }

    private static void validateHeader(String value, String path, boolean required) {
        if (value == null || value.isBlank()) {
            if (required)
                throw new ConfigException(path + " must not be blank.");
            return;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c <= 32 || c >= 127 || "()<>@,;:\\\"/[]?={}".indexOf(c) >= 0)
                throw new ConfigException(path + " must be a valid HTTP header name.");
        }
    }

    private static String requireText(String value, String path) {
        if (value == null || value.isBlank())
            throw new ConfigException(path + " must not be blank.");
        return value;
    }

    private static String trimOws(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && (value.charAt(start) == ' ' || value.charAt(start) == '\t'))
            start++;
        while (end > start && (value.charAt(end - 1) == ' ' || value.charAt(end - 1) == '\t'))
            end--;
        return value.substring(start, end);
    }

    @Override
    public String toString() {
        return "HmacConfig{enabled=" + enabled + ", profiles=" + profiles.keySet() + '}';
    }
}
