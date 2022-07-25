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

package com.networknt.body;

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
 * Created by steve on 29/09/16.
 */
public class BodyConfig {
    private static final Logger logger = LoggerFactory.getLogger(BodyConfig.class);

    public static final String CONFIG_NAME = "body";
    private static final String ENABLED = "enabled";
    private static final String CACHE_REQUEST_BODY = "cacheRequestBody";
    private static final String APPLIED_PATH_PREFIXES = "appliedPathPrefixes";

    boolean enabled;
    boolean cacheRequestBody;
    List<String> appliedPathPrefixes;

    private Config config;
    private Map<String, Object> mappedConfig;

    public BodyConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private BodyConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
    }

    public static BodyConfig load() {
        return new BodyConfig();
    }

    public static BodyConfig load(String configName) {
        return new BodyConfig(configName);
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setConfigList();
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isCacheRequestBody() {
        return cacheRequestBody;
    }

    public List<String> getAppliedPathPrefixes() {
        return appliedPathPrefixes;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if (object != null && (Boolean) object) {
            enabled = (Boolean)object;
        }
        object = mappedConfig.get(CACHE_REQUEST_BODY);
        if (object != null && (Boolean) object) {
            cacheRequestBody = (Boolean)object;
        }
    }

    private void setConfigList() {
        if (mappedConfig.get(APPLIED_PATH_PREFIXES) != null) {
            Object object = mappedConfig.get(APPLIED_PATH_PREFIXES);
            appliedPathPrefixes = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        appliedPathPrefixes = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the appliedPathPrefixes json with a list of strings.");
                    }
                } else {
                    // comma separated
                    appliedPathPrefixes = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                List prefixes = (List)object;
                prefixes.forEach(item -> {
                    appliedPathPrefixes.add((String)item);
                });
            } else {
                throw new ConfigException("appliedPathPrefixes must be a string or a list of strings.");
            }
        }
    }

}
