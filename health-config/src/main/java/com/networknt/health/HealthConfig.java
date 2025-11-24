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

package com.networknt.health;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.*;

import java.util.Map;

/**
 * Config class for Health Handler
 *
 * @author Steve Hu
 */
@ConfigSchema(
        configKey = "health",
        configName = "health",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD},
        configDescription = "Server health endpoint that can output OK to indicate the server is alive."
)
public class HealthConfig {
    public static final String CONFIG_NAME = "health";
    private static final String ENABLED = "enabled";
    private static final String USE_JSON = "useJson";
    private static final String TIMEOUT = "timeout";
    private static final String DOWNSTREAM_ENABLED = "downstreamEnabled";
    private static final String DOWNSTREAM_HOST = "downstreamHost";
    private static final String DOWNSTREAM_PATH = "downstreamPath";

    private Map<String, Object> mappedConfig;
    private final Config config;

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            defaultValue = "true",
            externalized = true,
            description = "true to enable this middleware handler. By default, the health check is enabled."
    )
    boolean enabled;

    @BooleanField(
            configFieldName = USE_JSON,
            externalizedKeyName = USE_JSON,
            externalized = true,
            description = "true to return Json format message. By default, it is false. It will only be changed to true if the monitor\n" +
                          "tool only support JSON response body.",
            defaultValue = "false"
    )
    boolean useJson;

    @IntegerField(
            configFieldName = TIMEOUT,
            externalizedKeyName = TIMEOUT,
            defaultValue = "2000",
            externalized = true,
            description = "timeout in milliseconds for the health check. If the duration is passed, a failure will return.\n" +
                    "It is to prevent taking too much time to check subsystems that are not available or timeout.\n" +
                    "As the health check is used by the control plane for service discovery, by default, one request\n" +
                    "per ten seconds. The quick response time is very important to not block the control plane."
    )
    int timeout;

    @BooleanField(
            configFieldName = DOWNSTREAM_ENABLED,
            externalizedKeyName = DOWNSTREAM_ENABLED,
            externalized = true,
            description = "For some of the services like light-proxy, http-sidecar and kafka-sidecar, we might need to check the down\n" +
                    "stream API before return the health status to the invoker. By default, it is not enabled.\n" +
                    "if the health check needs to invoke down streams API. It is false by default.",
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
            configFieldName = DOWNSTREAM_PATH,
            externalizedKeyName = DOWNSTREAM_PATH,
            externalized = true,
            defaultValue = "/health",
            description = "down stream API health check path. This allows the down stream API to have customized path implemented."
    )
    String downstreamPath;

    private HealthConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }
    private HealthConfig() {
        this(CONFIG_NAME);
    }

    public static HealthConfig load(String configName) {
        return new HealthConfig(configName);
    }

    public static HealthConfig load() {
        return new HealthConfig();
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

    public boolean isUseJson() {
        return useJson;
    }

    public void setUseJson(boolean useJson) {
        this.useJson = useJson;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
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

    public String getDownstreamPath() {
        return downstreamPath;
    }

    public void setDownstreamPath(String downstreamPath) {
        this.downstreamPath = downstreamPath;
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
            object = getMappedConfig().get(USE_JSON);
            if(object != null) useJson = Config.loadBooleanValue(USE_JSON, object);
            object = getMappedConfig().get(TIMEOUT);
            if(object != null) timeout = Config.loadIntegerValue(TIMEOUT, object);
            object = getMappedConfig().get(DOWNSTREAM_ENABLED);
            if(object != null) downstreamEnabled = Config.loadBooleanValue(DOWNSTREAM_ENABLED, object);
            object = getMappedConfig().get(DOWNSTREAM_HOST);
            if(object != null) downstreamHost = (String)object;
            object = getMappedConfig().get(DOWNSTREAM_PATH);
            if(object != null) downstreamPath = (String)object;
        }
    }

}
