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

package com.networknt.logging.model;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;

import java.util.Map;

/**
 * Config class for Logger handlers
 */
public class LoggerConfig {
    public static final String CONFIG_NAME = "logging";
    private static final String ENABLED = "enabled";
    private static final String LOG_START = "logStart";
    private static final String DOWNSTREAM_ENABLED = "downstreamEnabled";
    private static final String DOWNSTREAM_HOST = "downstreamHost";
    private static final String DOWNSTREAM_FRAMEWORK = "downstreamFramework";

    boolean enabled;
    long logStart;
    boolean downstreamEnabled;
    String downstreamHost;
    String downstreamFramework;
    private final Config config;
    private Map<String, Object> mappedConfig;

    private LoggerConfig() {
        this(CONFIG_NAME);
    }

    private LoggerConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    public static LoggerConfig load() {
        return new LoggerConfig();
    }

    public static LoggerConfig load(String configName) {
        return new LoggerConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
    }

    public void setConfigData() {

        Object object = getMappedConfig().get(ENABLED);
        if(object != null) {
            if(object instanceof String) {
                enabled = Boolean.parseBoolean((String)object);
            } else if (object instanceof Boolean) {
                enabled = (Boolean) object;
            } else {
                throw new ConfigException("enabled must be a boolean value.");
            }
        }
        object = getMappedConfig().get(DOWNSTREAM_ENABLED);
        if(object != null) {
            if(object instanceof String) {
                downstreamEnabled = Boolean.parseBoolean((String)object);
            } else if (object instanceof Boolean) {
                downstreamEnabled = (Boolean) object;
            } else {
                throw new ConfigException("downstreamEnabled must be a boolean value.");
            }
        }
        object = getMappedConfig().get(LOG_START);
        if(object != null) {
            if(object instanceof String) {
                logStart = Long.parseLong((String)object);
            } else if (object instanceof Number) {
                logStart = ((Number)object).longValue();
            } else {
                throw new ConfigException("logStart must be a long value.");
            }
        }
        object = getMappedConfig().get(DOWNSTREAM_HOST);
        if(object != null ) {
            downstreamHost = (String)object;
        }
        object = getMappedConfig().get(DOWNSTREAM_FRAMEWORK);
        if(object != null ) {
            downstreamFramework = (String)object;
        }
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getLogStart() {
        return logStart;
    }

    public void setLogStart(long logStart) {
        this.logStart = logStart;
    }

    public boolean isDownstreamEnabled() {
        return downstreamEnabled;
    }

    public void setDownstreamEnabled(boolean downstreamEnabled) {
        this.downstreamEnabled = downstreamEnabled;
    }

    public String getDownstreamHost() {
        return downstreamHost;
    }

    public void setDownstreamHost(String downstreamHost) {
        this.downstreamHost = downstreamHost;
    }

    public String getDownstreamFramework() {
        return downstreamFramework;
    }

    public void setDownstreamFramework(String downstreamFramework) {
        this.downstreamFramework = downstreamFramework;
    }
}
