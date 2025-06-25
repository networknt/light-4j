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

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import com.networknt.config.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by Steve Hu on 2017-01-21.
 */
@ConfigSchema(configKey = "cors", configName = "cors", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML})
public class CorsConfig {
    private static final Logger logger = LoggerFactory.getLogger(CorsConfig.class);

    public static final String CONFIG_NAME = "cors";
    public static final String ENABLED = "enabled";
    public static final String ALLOWED_ORIGINS = "allowedOrigins";
    public static final String ALLOWED_METHODS = "allowedMethods";
    public static final String PATH_PREFIX_ALLOWED = "pathPrefixAllowed";

    private Map<String, Object> mappedConfig;
    private final Config config;

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            defaultValue = "true",
            externalized = true,
            description = "Indicate if the CORS middleware is enabled or not."
    )
    boolean enabled;

    @ArrayField(
            configFieldName = ALLOWED_ORIGINS,
            externalizedKeyName = ALLOWED_ORIGINS,
            externalized = true,
            description = "Allowed origins, you can have multiple and with port if port is not 80 or 443. This is the global\n" +
                    "configuration for all paths. If you want to have different configuration for different paths, you\n" +
                    "can use pathPrefixAllowed. The value is a list of strings.\n" +
                    "Wildcard is not supported for security reasons.",
            items = String.class
    )
    List<String> allowedOrigins;

    @ArrayField(
            configFieldName = ALLOWED_METHODS,
            externalizedKeyName = ALLOWED_METHODS,
            externalized = true,
            description = "Allowed methods list. The value is a list of strings. The possible value is GET, POST, PUT, DELETE, PATCH\n" +
                    "This is the global configuration for all paths. If you want to have different configuration for different\n" +
                    "paths, you can use pathPrefixAllowed.",
            items = String.class
    )
    List<String> allowedMethods;


    @MapField(
            configFieldName = PATH_PREFIX_ALLOWED,
            externalizedKeyName = PATH_PREFIX_ALLOWED,
            externalized = true,
            description = "cors configuration per path prefix on a shared gateway. You either have allowedOrigins and allowedMethods\n" +
                    "or you have pathPrefixAllowed. You can't have both. If you have both, pathPrefixAllowed will be used.\n" +
                    "The value is a map with the key as the path prefix and the value is another map with allowedOrigins and\n" +
                    "allowedMethods. The allowedOrigins is a list of strings and allowedMethods is a list of strings.\n" +
                    "\n" +
                    "Use the above global configuration if you are dealing with a single API in the case of http-sidecar,\n" +
                    "proxy server or build the API with light-4j frameworks. If you are using light-gateway with multiple\n" +
                    "downstream APIs, you can use the pathPrefixAllowed to set up different CORS configuration for different\n" +
                    "APIs.\n" +
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

    private CorsConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
        setConfigMap();
    }
    private CorsConfig() {
        this(CONFIG_NAME);
    }

    public static CorsConfig load(String configName) {
        return new CorsConfig(configName);
    }

    public static CorsConfig load() {
        return new CorsConfig();
    }

    public void reload() {
        this.reload(CONFIG_NAME);
    }

    public void reload(String configName) {
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
        setConfigMap();
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

    public void setAllowedOrigins(List allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    @Deprecated(since = "2.2.1")
    public Map<String, Object> getPathPrefixAllowed() {
        final var mapper = Config.getInstance().getMapper();
        return mapper.convertValue(pathPrefixAllowed, new TypeReference<>() {});
    }

    @Deprecated(since = "2.2.1")
    public void setPathPrefixAllowed(Map<String, Object> pathPrefixAllowed) {
        final var mapper = Config.getInstance().getMapper();
        this.pathPrefixAllowed = mapper.convertValue(pathPrefixAllowed, new TypeReference<>() {});
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    Config getConfig() {
        return config;
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
                if(s.startsWith("[")) {
                    // json format
                    try {
                        allowedOrigins = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the skipPathPrefixes json with a list of strings.");
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
                if(s.startsWith("[")) {
                    // json format
                    try {
                        allowedMethods = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the skipPathPrefixes json with a list of strings.");
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
