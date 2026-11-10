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


    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            defaultValue = "true",
            description = "Indicate if the CORS middleware is enabled or not."
    )
    boolean enabled;

    @ArrayField(
            configFieldName = ALLOWED_ORIGINS,
            externalizedKeyName = ALLOWED_ORIGINS,
            description = "Allowed origins, you can have multiple and with port if port is not 80 or 443. This is the global\n" +
                    "configuration for all paths. If you want to have different configuration for different paths, you\n" +
                    "can use pathPrefixAllowed. The value is a list of strings.\n" +
                    "HTTP/HTTPS origins use ASCII/Punycode hosts, no trailing dot, and ports 0-65535.\n" +
                    "Scoped wildcards such as https://*.example.com trust every subdomain at every depth, excluding the apex.\n" +
                    "Use only organization-controlled trusted subdomains; public/private PSL suffixes and unknown suffixes are rejected.\n" +
                    "Credentials remain enabled. Explicit null is supported only by an explicit null entry.\n" +
                    "The legacy server-origin fallback remains after allowlist misses, including empty lists.\n" +
                    "Invalid startup configuration fails; invalid refresh retains the last valid policy.\n - http://localhost\n - https://*.example.com",
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
            description = "CORS configuration per path prefix on a shared gateway. The first raw string prefix match in map order\n" +
                    "replaces the global origins and methods; unmatched paths use global settings. Prefixes are not segment-aware:\n" +
                    " /api also matches /api-other. Put more specific prefixes first; map order must be preserved.\n" +
                    "Missing per-path origins or methods do not inherit the global values. The server-origin fallback still applies.\n" +
                    "The value is a map with the key as the path prefix and the value is another map with allowedOrigins and\n" +
                    "allowedMethods. The allowedOrigins is a list of strings and allowedMethods is a list of strings.\n" +
                    "\n" +
                    "Use the above global configuration if you are dealing with a single API in the case of http-sidecar,\n" +
                    "proxy server or build the API with light-4j frameworks. If you are using light-gateway with multiple\n" +
                    "downstream APIs, you can use the pathPrefixAllowed to set up different CORS configuration for different APIs.\n" +
                    "\n" +
                    "\n" +
                    "Here is an example in values.yml\n" +
                    "cors.pathPrefixAllowed:\n" +
                    "  /v1/pets:\n" +
                    "    allowedOrigins:\n" +
                    "      - https://abc.com\n" +
                    "      - https://www.xyz.com\n" +
                    "    allowedMethods:\n" +
                    "      - GET\n" +
                    "      - PUT\n" +
                    "      - POST\n" +
                    "      - DELETE\n" +
                    "  /v1/market:\n" +
                    "    allowedOrigins:\n" +
                    "      - https://def.com\n" +
                    "      - https://abc.com\n" +
                    "    allowedMethods:\n" +
                    "      - GET\n" +
                    "      - POST",
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

    private static Policy compilePolicy(List<String> origins, List<String> methods, String location) {
        return new Policy(CorsOriginPolicy.compile(origins, location),
                methods == null ? List.of() : List.copyOf(methods));
    }

    private static Map<String, Policy> compilePaths(Map<String, CorsPathPrefix> paths) {
        Map<String, Policy> compiled = new LinkedHashMap<>();
        if (paths != null) {
            paths.forEach((path, value) -> {
                if (path == null || value == null) throw new ConfigException("pathPrefixAllowed requires non-null paths and policies");
                compiled.put(path, compilePolicy(value.allowedOrigins, value.allowedMethods,
                        "pathPrefixAllowed[" + path + "].allowedOrigins"));
            });
        }
        return Collections.unmodifiableMap(compiled);
    }

    private CorsConfig(String configName) {
        mappedConfig = Config.getInstance().getJsonMapConfig(configName);
        setConfigData();
        setConfigList();
        setConfigMap();
        policies = new Policies(compilePolicy(allowedOrigins, allowedMethods, configName + ".allowedOrigins"),
                compilePaths(pathPrefixAllowed));
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List getAllowedOrigins() {
        return allowedOrigins;
    }

    public synchronized void setAllowedOrigins(List allowedOrigins) {
        Policy candidate = compilePolicy(allowedOrigins, allowedMethods, "allowedOrigins");
        this.allowedOrigins = allowedOrigins == null ? null : List.copyOf(allowedOrigins);
        policies = new Policies(candidate, policies.paths);
    }

    public List getAllowedMethods() {
        return allowedMethods;
    }

    public synchronized void setAllowedMethods(List allowedMethods) {
        Policy candidate = compilePolicy(allowedOrigins, allowedMethods, "allowedMethods");
        this.allowedMethods = allowedMethods == null ? null : List.copyOf(allowedMethods);
        policies = new Policies(candidate, policies.paths);
    }

    @Deprecated(since = "2.2.1")
    public Map<String, Object> getPathPrefixAllowed() {
        final var mapper = Config.getInstance().getMapper();
        return mapper.convertValue(pathPrefixAllowed, new TypeReference<>() {});
    }

    @Deprecated(since = "2.2.1")
    public synchronized void setPathPrefixAllowed(Map<String, Object> pathPrefixAllowed) {
        final var mapper = Config.getInstance().getMapper();
        Map<String, CorsPathPrefix> candidate = mapper.convertValue(pathPrefixAllowed, new TypeReference<>() {});
        Map<String, Policy> compiled = compilePaths(candidate);
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
        if (mappedConfig != null && mappedConfig.get(ALLOWED_ORIGINS) != null) {
            Object object = mappedConfig.get(ALLOWED_ORIGINS);
            allowedOrigins = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if (s.isEmpty()) {
                    // Preserve blank externalized values as an empty allowlist.
                } else if(s.startsWith("[")) {
                    // json format
                    try {
                        allowedOrigins = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse allowedOrigins JSON with a list of strings.");
                    }
                } else {
                    // comma separated
                    allowedOrigins = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                List prefixes = (List)object;
                prefixes.forEach(item -> {
                    allowedOrigins.add((String)item);
                });
            } else {
                throw new ConfigException("allowedOrigins must be a string or a list of strings.");
            }
        }
        if (mappedConfig != null && mappedConfig.get(ALLOWED_METHODS) != null) {
            Object object = mappedConfig.get(ALLOWED_METHODS);
            allowedMethods = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if (s.isEmpty()) {
                    // Preserve blank externalized values as an empty allowlist.
                } else if(s.startsWith("[")) {
                    // json format
                    try {
                        allowedMethods = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse allowedMethods JSON with a list of strings.");
                    }
                } else {
                    // comma separated
                    allowedMethods = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                List<String> prefixes = (List)object;
                allowedMethods.addAll(prefixes);
            } else {
                throw new ConfigException("allowedMethods must be a string or a list of strings.");
            }
        }
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
