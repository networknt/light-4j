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
package com.networknt.correlation;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by steve on 29/09/16.
 */
public class CorrelationConfig {
    public static final String CONFIG_NAME = "correlation";
    private static final String ENABLED = "enabled";
    private static final String AUTOGEN_CORRELATION_ID = "autogenCorrelationID";
    private Map<String, Object> mappedConfig;
    private final Config config;
    boolean enabled;
    boolean autogenCorrelationID;

    private CorrelationConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }
    private CorrelationConfig() {
        this(CONFIG_NAME);
    }

    public static CorrelationConfig load(String configName) {
        return new CorrelationConfig(configName);
    }

    public static CorrelationConfig load() {
        return new CorrelationConfig();
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
    }

    public void reload(String configName) {
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAutogenCorrelationID() {
    	return autogenCorrelationID;
    }

    public void setAutogenCorrelationID(boolean autogenCorrelationID) {
    	this.autogenCorrelationID = autogenCorrelationID;
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
            object = getMappedConfig().get(AUTOGEN_CORRELATION_ID);
            if(object != null) autogenCorrelationID = Config.loadBooleanValue(AUTOGEN_CORRELATION_ID, object);
        }
    }
}
