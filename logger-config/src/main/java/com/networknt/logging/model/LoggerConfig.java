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
import com.networknt.config.schema.*;

import java.util.Map;

/**
 * Config class for Logger handlers
 */
@ConfigSchema(
        configKey = "logging",
        configName = "logging",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD},
        configDescription = "Logging endpoint that can output the logger with logging levels."
)
public class LoggerConfig {
    public static final String CONFIG_NAME = "logging";
    private static final String ENABLED = "enabled";
    private static final String LOG_START = "logStart";
    private static final String DOWNSTREAM_ENABLED = "downstreamEnabled";
    private static final String DOWNSTREAM_HOST = "downstreamHost";
    private static final String DOWNSTREAM_FRAMEWORK = "downstreamFramework";

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            defaultValue = "true",
            externalized = true,
            description = "Indicate if the logging info is enabled or not."
    )
    boolean enabled;

    @IntegerField(
            configFieldName = LOG_START,
            externalizedKeyName = LOG_START,
            defaultValue = "600000",
            externalized = true,
            description = "Indicate the default time period backward in millisecond for log content retrieve.\n" +
                          "Default is an hour which indicate system will retrieve one hour log by default"
    )
    long logStart;

    @BooleanField(
            configFieldName = DOWNSTREAM_ENABLED,
            externalizedKeyName = DOWNSTREAM_ENABLED,
            externalized = true,
            description = "if the logger access needs to invoke down streams API. It is false by default.",
            defaultValue = "false"
    )
    boolean downstreamEnabled;

    @StringField(
            configFieldName = DOWNSTREAM_HOST,
            externalizedKeyName = DOWNSTREAM_HOST,
            externalized = true,
            defaultValue = "http://localhost:8081",
            description = "down stream API host. http://localhost is the default when used with http-sidecar and kafka-sidecar."
    )
    String downstreamHost;

    @StringField(
            configFieldName = DOWNSTREAM_FRAMEWORK,
            externalizedKeyName = DOWNSTREAM_FRAMEWORK,
            externalized = true,
            defaultValue = "Light4j",
            description = "down stream API framework that has the admin endpoints. Light4j, SpringBoot, Quarkus, Micronaut, Helidon, etc. If the adm endpoints are different between\n" +
                          "different versions, you can use the framework plus version as the identifier. For example, Light4j-1.6.0, SpringBoot-2.4.0, etc."
    )
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
        if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
        object = getMappedConfig().get(DOWNSTREAM_ENABLED);
        if(object != null) downstreamEnabled = Config.loadBooleanValue(DOWNSTREAM_ENABLED, object);
        object = getMappedConfig().get(LOG_START);
        if(object != null)  logStart = Config.loadLongValue(LOG_START, object);
        object = getMappedConfig().get(DOWNSTREAM_HOST);
        if(object != null ) downstreamHost = (String)object;
        object = getMappedConfig().get(DOWNSTREAM_FRAMEWORK);
        if(object != null ) downstreamFramework = (String)object;
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
