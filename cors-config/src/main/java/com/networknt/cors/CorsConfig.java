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

package com.networknt.cors;

import tools.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.*;
import com.networknt.server.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Steve Hu on 2017-01-21.
 */
@ConfigSchema(configKey = "cors", configName = "cors", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD})
public class CorsConfig {
    private static final Logger logger = LoggerFactory.getLogger(CorsConfig.class);

    public static final String CONFIG_NAME = "cors";
    public static final String ENABLED = "enabled";
    public static final String ALLOWED_ORIGINS = "allowedOrigins";
    public static final String ALLOWED_METHODS = "allowedMethods";
    public static final String PATH_PREFIX_ALLOWED = "pathPrefixAllowed";

    private Map<String, Object> mappedConfig;
    private final String configName;


    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            defaultValue = "true",
            description = "Indicate if the CORS middleware is enabled or not. Missing configuration or an absent enabled key stays disabled; the packaged YAML explicitly enables CORS."
    )
    // Keep the established backing-field name and external ENABLED key distinct by convention.
    @SuppressWarnings("java:S1845")
    volatile boolean enabled;

    @ArrayField(
            configFieldName = ALLOWED_ORIGINS,
            externalizedKeyName = ALLOWED_ORIGINS,
            description = "Allowed origins, you can have multiple and with port if port is not 80 or 443. This is the global\n" +
                    "configuration for all paths. If you want to have different configuration for different paths, you\n" +
                    "can use pathPrefixAllowed. The value is a list of strings.\n" +
                    "Origins use ASCII/Punycode hosts and ports 0-65535; exact entries retain custom schemes, underscores, and trailing dots.\n" +
                    "HTTP/HTTPS DNS wildcards such as https://*.example.com trust every subdomain at every depth, excluding the apex.\n" +
                    "Use only organization-controlled trusted subdomains; public/private PSL suffixes and unknown suffixes are rejected.\n" +
                    "Credentials remain enabled. Explicit null is supported only by an explicit null entry.\n" +
                    "The legacy server-origin fallback remains after allowlist misses, including empty lists.\n" +
                    "Enabled CORS validates at startup and before enabling; invalid refresh retains the last valid policy.\n - http://localhost\n - https://*.example.com",
            items = String.class
    )
    List<String> allowedOrigins;

    @ArrayField(
            configFieldName = ALLOWED_METHODS,
            externalizedKeyName = ALLOWED_METHODS,
            description = "Allowed methods list. The value is a list of strings. The possible value is GET, POST, PUT, DELETE, PATCH\n" +
                    "This is the global configuration for all paths. If you want to have different configuration for different\n" +
                    "paths, you can use pathPrefixAllowed.\n - GET\n - POST",
            items = String.class
    )
    List<String> allowedMethods;


    @MapField(
            configFieldName = PATH_PREFIX_ALLOWED,
            externalizedKeyName = PATH_PREFIX_ALLOWED,
            description = """
                    CORS configuration per path prefix on a shared gateway. The first raw string prefix match in map order
                    replaces the global origins and methods; unmatched paths use global settings. Prefixes are not segment-aware:
                     /api also matches /api-other. Put more specific prefixes first; map order must be preserved.
                    Missing per-path origins or methods do not inherit the global values. The server-origin fallback still applies.
                    The value is a map with the key as the path prefix and the value is another map with allowedOrigins and
                    allowedMethods. The allowedOrigins is a list of strings and allowedMethods is a list of strings.

                    Use the above global configuration if you are dealing with a single API in the case of http-sidecar,
                    proxy server or build the API with light-4j frameworks. If you are using light-gateway with multiple
                    downstream APIs, you can use the pathPrefixAllowed to set up different CORS configuration for different APIs.


                    Here is an example in values.yml
                    cors.pathPrefixAllowed:
                      /v1/pets:
                        allowedOrigins:
                          - https://abc.com
                          - https://www.xyz.com
                        allowedMethods:
                          - GET
                          - PUT
                          - POST
                          - DELETE
                      /v1/market:
                        allowedOrigins:
                          - https://def.com
                          - https://abc.com
                        allowedMethods:
                          - GET
                          - POST""",
            valueType = CorsPathPrefix.class
    )
    Map<String, CorsPathPrefix> pathPrefixAllowed;

    private static final Map<String, CachedConfig> instances = new ConcurrentHashMap<>();
    private volatile Policies policies;

    private record CachedConfig(CorsConfig config, Map<String, Object> seen) { }
    private record Policies(Policy global, Map<String, Policy> paths) { }

    /** One immutable policy selected using the existing first raw-prefix match contract. */
    public record Policy(CorsOriginPolicy origins, List<String> methods) { }

    public Policy policyForPath(String requestPath) {
        Policies current = policies;
        for (Map.Entry<String, Policy> entry : current.paths.entrySet()) {
            if (requestPath.startsWith(entry.getKey())) return entry.getValue();
        }
        return current.global;
    }

    private static Policy compilePolicy(List<?> origins, List<?> methods, String location) {
        List<String> validatedMethods = new ArrayList<>();
        if (methods != null) {
            String methodLocation = location + "." + ALLOWED_METHODS;
            for (int i = 0; i < methods.size(); i++) {
                if (!(methods.get(i) instanceof String method)) {
                    throw new ConfigException(methodLocation + "[" + i + "]: expected a non-null string");
                }
                validatedMethods.add(method);
            }
        }
        return new Policy(CorsOriginPolicy.compile(origins, location + "." + ALLOWED_ORIGINS), List.copyOf(validatedMethods));
    }

    private Policies compilePolicies() {
        return new Policies(compilePolicy(allowedOrigins, allowedMethods, configName), compilePaths(pathPrefixAllowed, configName));
    }

    private static Map<String, Policy> compilePaths(Map<String, CorsPathPrefix> paths, String configName) {
        Map<String, Policy> compiled = new LinkedHashMap<>();
        if (paths != null) {
            paths.forEach((path, value) -> {
                if (path == null || value == null) throw new ConfigException("pathPrefixAllowed requires non-null paths and policies");
                compiled.put(path, compilePolicy(value.allowedOrigins, value.allowedMethods,
                        configName + "." + PATH_PREFIX_ALLOWED + "[" + path + "]"));
            });
        }
        return Collections.unmodifiableMap(compiled);
    }

    private CorsConfig(String configName) {
        this.configName = configName;
        mappedConfig = Config.getInstance().getJsonMapConfig(configName);
        setConfigData();
        setConfigList();
        setConfigMap();
        policies = enabled ? compilePolicies()
                : new Policies(compilePolicy(null, null, configName), Map.of());
    }
    private CorsConfig() {
        this(CONFIG_NAME);
    }

    public static CorsConfig load() {
        return load(CONFIG_NAME);
    }

    public static CorsConfig load(String configName) {
        Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
        CachedConfig cached = instances.get(configName);
        if (cached != null && cached.seen == mappedConfig) return cached.config;
        synchronized (CorsConfig.class) {
            mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            cached = instances.get(configName);
            if (cached != null && cached.seen == mappedConfig) return cached.config;
            final CorsConfig candidate;
            try {
                candidate = new CorsConfig(configName);
            } catch (RuntimeException e) {
                if (cached == null) throw e;
                logger.error("Rejected CORS configuration refresh for {}; retaining last valid policy: {}",
                        configName, e.getMessage());
                instances.put(configName, new CachedConfig(cached.config, mappedConfig));
                return cached.config;
            }
            if (CONFIG_NAME.equals(configName)) {
                ModuleRegistry.registerModule(configName, CorsConfig.class.getName(),
                        Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), null);
            }
            instances.put(configName, new CachedConfig(candidate, candidate.mappedConfig));
            return candidate;
        }
    }

    public synchronized boolean isEnabled() {
        return enabled;
    }

    public synchronized void setEnabled(boolean enabled) {
        // A disabled middleware may retain obsolete values; validate before it can become active.
        if (enabled && !this.enabled) policies = compilePolicies();
        this.enabled = enabled;
    }

    public synchronized List getAllowedOrigins() {
        return allowedOrigins;
    }

    public synchronized void setAllowedOrigins(List allowedOrigins) {
        Policy candidate = enabled ? compilePolicy(allowedOrigins, allowedMethods, configName) : policies.global;
        this.allowedOrigins = allowedOrigins == null ? null : Collections.unmodifiableList(new ArrayList<>(allowedOrigins));
        policies = new Policies(candidate, policies.paths);
    }

    public synchronized List getAllowedMethods() {
        return allowedMethods;
    }

    public synchronized void setAllowedMethods(List allowedMethods) {
        Policy candidate = enabled ? compilePolicy(allowedOrigins, allowedMethods, configName) : policies.global;
        this.allowedMethods = allowedMethods == null ? null : Collections.unmodifiableList(new ArrayList<>(allowedMethods));
        policies = new Policies(candidate, policies.paths);
    }

    /**
     * @return the configured per-path data in its legacy map representation.
     * @deprecated use {@link #policyForPath(String)} for request matching; retained for binary compatibility.
     */
    @SuppressWarnings("java:S1133") // Public compatibility API; removal requires a separate breaking release.
    @Deprecated(since = "2.2.1", forRemoval = false)
    public synchronized Map<String, Object> getPathPrefixAllowed() {
        final var mapper = Config.getInstance().getMapper();
        return mapper.convertValue(pathPrefixAllowed, new TypeReference<>() {});
    }

    /**
     * @param pathPrefixAllowed the legacy per-path configuration map.
     * @deprecated configure named CORS data and reload with {@link #load(String)}; retained for binary compatibility.
     */
    @SuppressWarnings("java:S1133") // Public compatibility API; removal requires a separate breaking release.
    @Deprecated(since = "2.2.1", forRemoval = false)
    public synchronized void setPathPrefixAllowed(Map<String, Object> pathPrefixAllowed) {
        final var mapper = Config.getInstance().getMapper();
        Map<String, CorsPathPrefix> candidate = mapper.convertValue(pathPrefixAllowed, new TypeReference<>() {});
        Map<String, Policy> compiled = enabled ? compilePaths(candidate, configName) : Map.of();
        this.pathPrefixAllowed = candidate;
        policies = new Policies(policies.global, compiled);
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }



    private void setConfigData() {
        if(getMappedConfig() != null) {
            Object object = getMappedConfig().get(ENABLED);
            if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
        }
    }

    private void setConfigList() {
        if (mappedConfig != null) {
            // Preserve legacy null getters for absent fields while readList itself always returns a collection.
            if (mappedConfig.get(ALLOWED_ORIGINS) != null) {
                allowedOrigins = readList(mappedConfig.get(ALLOWED_ORIGINS), configName + "." + ALLOWED_ORIGINS);
            }
            if (mappedConfig.get(ALLOWED_METHODS) != null) {
                allowedMethods = readList(mappedConfig.get(ALLOWED_METHODS), configName + "." + ALLOWED_METHODS);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> readList(Object value, String property) {
        List<?> values = switch (value) {
            case null -> List.of();
            case String text -> readTextList(text.trim(), property);
            case List<?> list -> list;
            default -> throw new ConfigException(property + ": expected a string or list of strings");
        };
        // Preserve runtime types until compilePolicy can report an actionable property/index error.
        return (List<String>) Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static List<?> readTextList(String text, String property) {
        if (text.startsWith("[")) {
            try {
                return Config.getInstance().getMapper().readValue(text, new TypeReference<List<Object>>() {});
            } catch (Exception _) {
                throw new ConfigException(property + ": expected a JSON list of strings");
            }
        }
        // Empty comma-separated items were harmless non-matches before pattern validation.
        return Arrays.stream(text.split(",")).map(String::trim).filter(item -> !item.isEmpty()).toList();
    }

    private void setConfigMap() {
        if (mappedConfig != null && mappedConfig.get(PATH_PREFIX_ALLOWED) != null) {
            Object object = mappedConfig.get(PATH_PREFIX_ALLOWED);
            if(object != null) {
                if (object instanceof Map) {
                    pathPrefixAllowed = Config.getInstance().getMapper().convertValue(object, new TypeReference<>() {});
                } else if (object instanceof String) {
                    String s = (String) object;
                    s = s.trim();
                    if (s.startsWith("{")) {
                        // json format
                        try {
                            pathPrefixAllowed = Config.getInstance().getMapper().readValue(s, new TypeReference<>() {
                            });
                        } catch (Exception e) {
                            throw new ConfigException("could not parse the pathPrefixAllowed json with a map of string and object.");
                        }
                    } else {
                        throw new ConfigException("pathPrefixAllowed must be a map.");
                    }
                } else {
                    throw new ConfigException("pathPrefixAllowed must be a map.");
                }

            }
        }
    }
}
