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

package com.networknt.proxy;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;

import java.util.Map;

/**
 * Config class for reverse proxy. This config class supports reload.
 *
 * @author Steve Hu
 */
public class ProxyConfig {
    public static final String CONFIG_NAME = "proxy";
    private static final String ENABLED = "enabled";
    private static final String HTTP2_ENABLED = "http2Enabled";
    private static final String HOSTS = "hosts";
    private static final String CONNECTIONS_PER_THREAD = "connectionsPerThread";
    private static final String MAX_REQUEST_TIME = "maxRequestTime";
    private static final String REWRITE_HOST_HEADER = "rewriteHostHeader";
    private static final String REUSE_X_FORWARDED = "reuseXForwarded";
    private static final String MAX_CONNECTION_RETRIES = "maxConnectionRetries";
    private static final String MAX_QUEUE_SIZE = "maxQueueSize";
    private static final String FORWARD_JWT_CLAIMS = "forwardJwtClaims";
    private static final String METRICS_INJECTION = "metricsInjection";
    private static final String METRICS_NAME = "metricsName";

    boolean enabled;
    boolean http2Enabled;
    String hosts;
    int connectionsPerThread;
    int maxRequestTime;
    boolean rewriteHostHeader;
    boolean reuseXForwarded;
    int maxConnectionRetries;
    int maxQueueSize;
    private boolean forwardJwtClaims;
    boolean metricsInjection;
    String metricsName;

    private Config config;
    private Map<String, Object> mappedConfig;

    private ProxyConfig() {
        this(CONFIG_NAME);
    }

    private ProxyConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    public static ProxyConfig load() {
        return new ProxyConfig();
    }

    public static ProxyConfig load(String configName) {
        return new ProxyConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isHttp2Enabled() {
        return http2Enabled;
    }

    public String getHosts() {
        return hosts;
    }

    public int getConnectionsPerThread() {
        return connectionsPerThread;
    }

    public int getMaxRequestTime() {
        return maxRequestTime;
    }

    public boolean isRewriteHostHeader() { return rewriteHostHeader; }

    public boolean isReuseXForwarded() { return reuseXForwarded; }

    public int getMaxConnectionRetries() { return maxConnectionRetries; }

    public int getMaxQueueSize() { return maxQueueSize; }

    public boolean isForwardJwtClaims() {
        return forwardJwtClaims;
    }
    public boolean isMetricsInjection() { return metricsInjection; }
    public String getMetricsName() { return metricsName; }

    private void setConfigData() {
        Object object = getMappedConfig().get(HTTP2_ENABLED);
        if(object != null) http2Enabled = Config.loadBooleanValue(HTTP2_ENABLED, object);
        object = getMappedConfig().get(REWRITE_HOST_HEADER);
        if(object != null) rewriteHostHeader = Config.loadBooleanValue(REWRITE_HOST_HEADER, object);
        object = getMappedConfig().get(REUSE_X_FORWARDED);
        if(object != null) reuseXForwarded = Config.loadBooleanValue(REUSE_X_FORWARDED, object);
        object = getMappedConfig().get(FORWARD_JWT_CLAIMS);
        if(object != null) forwardJwtClaims = Config.loadBooleanValue(FORWARD_JWT_CLAIMS, object);
        object = getMappedConfig().get(HOSTS);
        if(object != null) hosts = (String)object;
        object = getMappedConfig().get(CONNECTIONS_PER_THREAD);
        if(object != null) connectionsPerThread = Config.loadIntegerValue(CONNECTIONS_PER_THREAD, object);
        object = getMappedConfig().get(MAX_REQUEST_TIME);
        if(object != null) maxRequestTime = Config.loadIntegerValue(MAX_REQUEST_TIME, object);
        object = getMappedConfig().get(MAX_CONNECTION_RETRIES);
        if(object != null) maxConnectionRetries = Config.loadIntegerValue(MAX_CONNECTION_RETRIES, object);
        object = getMappedConfig().get(MAX_QUEUE_SIZE);
        if(object != null) maxQueueSize = Config.loadIntegerValue(MAX_QUEUE_SIZE, object);
        object = getMappedConfig().get(METRICS_INJECTION);
        if(object != null) metricsInjection = Config.loadBooleanValue(METRICS_INJECTION, object);
        object = getMappedConfig().get(METRICS_NAME);
        if(object != null ) metricsName = (String)object;
    }
}
