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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by stevehu on 2017-01-21.
 */
public class CorsConfig {
    private static final Logger logger = LoggerFactory.getLogger(CorsConfig.class);

    public static final String CONFIG_NAME = "cors";
    private static final String ENABLED = "enabled";
    private static final String ALLOWED_ORIGINS = "allowedOrigins";
    private static final String ALLOWED_METHODS = "allowedMethods";

    private Map<String, Object> mappedConfig;
    private final Config config;

    boolean enabled;
    List<String> allowedOrigins;
    List<String> allowedMethods;

    private CorsConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
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
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setConfigList();
    }

    public void reload(String configName) {
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
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
}
