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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by Steve Hu on 2017-01-21.
 */
public class CorsConfig {
    private static final Logger logger = LoggerFactory.getLogger(CorsConfig.class);

    public static final String CONFIG_NAME = "cors";
    public static final String ENABLED = "enabled";
    public static final String ALLOWED_ORIGINS = "allowedOrigins";
    public static final String ALLOWED_METHODS = "allowedMethods";
    public static final String PATH_PREFIX_ALLOWED = "pathPrefixAllowed";

    private Map<String, Object> mappedConfig;
    private final Config config;

    boolean enabled;
    List<String> allowedOrigins;
    List<String> allowedMethods;
    Map<String, Object> pathPrefixAllowed;

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

    public Map<String, Object> getPathPrefixAllowed() {
        return pathPrefixAllowed;
    }

    public void setPathPrefixAllowed(Map<String, Object> pathPrefixAllowed) {
        this.pathPrefixAllowed = pathPrefixAllowed;
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
                    pathPrefixAllowed = (Map<String, Object>) object;
                } else if (object instanceof String) {
                    String s = (String) object;
                    s = s.trim();
                    if (s.startsWith("{")) {
                        // json format
                        try {
                            pathPrefixAllowed = Config.getInstance().getMapper().readValue(s, new TypeReference<Map<String, Object>>() {
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
