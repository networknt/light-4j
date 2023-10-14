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

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by steve on 29/09/16.
 */
public class BodyConfig {
    private static final Logger logger = LoggerFactory.getLogger(BodyConfig.class);

    public static final String CONFIG_NAME = "body";
    private static final String ENABLED = "enabled";
    private static final String CACHE_REQUEST_BODY = "cacheRequestBody";
    private static final String CACHE_RESPONSE_BODY = "cacheResponseBody";
    private static final String LOG_FULL_REQUEST_BODY = "logFullRequestBody";
    private static final String LOG_FULL_RESPONSE_BODY = "logFullResponseBody";

    boolean enabled;
    boolean cacheRequestBody;
    boolean cacheResponseBody;
    boolean logFullRequestBody;
    boolean logFullResponseBody;
    private final Config config;
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
    public boolean isCacheResponseBody() {
        return cacheResponseBody;
    }

    public boolean isLogFullRequestBody() { return logFullRequestBody; }

    public boolean isLogFullResponseBody() { return logFullResponseBody; }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
        object = mappedConfig.get(CACHE_REQUEST_BODY);
        if(object != null) cacheRequestBody = Config.loadBooleanValue(CACHE_REQUEST_BODY, object);
        object = mappedConfig.get(CACHE_RESPONSE_BODY);
        if(object != null) cacheResponseBody = Config.loadBooleanValue(CACHE_RESPONSE_BODY, object);
        object = mappedConfig.get(LOG_FULL_REQUEST_BODY);
        if(object != null) logFullRequestBody = Config.loadBooleanValue(LOG_FULL_REQUEST_BODY, object);
        object = mappedConfig.get(LOG_FULL_RESPONSE_BODY);
        if(object != null) logFullResponseBody = Config.loadBooleanValue(LOG_FULL_RESPONSE_BODY, object);
    }
}
