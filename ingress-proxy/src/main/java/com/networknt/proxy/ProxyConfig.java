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
    private static final String FORWARD_JWT_CLAIMS = "forwardJwtClaims";

    boolean enabled;
    boolean http2Enabled;
    String hosts;
    int connectionsPerThread;
    int maxRequestTime;
    boolean rewriteHostHeader;
    boolean reuseXForwarded;
    int maxConnectionRetries;
    private boolean forwardJwtClaims;

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

    public boolean isForwardJwtClaims() {
        return forwardJwtClaims;
    }

    private void setConfigData() {
        Object object = getMappedConfig().get(HTTP2_ENABLED);
        if(object != null && (Boolean) object) {
            http2Enabled = true;
        }
        object = getMappedConfig().get(REWRITE_HOST_HEADER);
        if(object != null && (Boolean) object) {
            rewriteHostHeader = true;
        }
        object = getMappedConfig().get(REUSE_X_FORWARDED);
        if(object != null && (Boolean) object) {
            reuseXForwarded = true;
        }
        object = getMappedConfig().get(FORWARD_JWT_CLAIMS);
        if(object != null && (Boolean) object) {
            forwardJwtClaims = true;
        }
        hosts = (String)getMappedConfig().get(HOSTS);
        connectionsPerThread = (Integer)getMappedConfig().get(CONNECTIONS_PER_THREAD);
        maxRequestTime = (Integer)getMappedConfig().get(MAX_REQUEST_TIME);
        maxConnectionRetries = (Integer)getMappedConfig().get(MAX_CONNECTION_RETRIES);
    }
}
