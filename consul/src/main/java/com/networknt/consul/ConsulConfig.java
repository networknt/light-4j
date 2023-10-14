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

package com.networknt.consul;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ConsulConfig {
    public static final String CONFIG_NAME = "consul";
    private static final Logger logger = LoggerFactory.getLogger(ConsulConfig.class);
    private static final String CONSUL_URL = "consulUrl";
    private static final String CONSUL_TOKEN = "consulToken";
    private static final String MAX_REQ_PER_CONN = "maxReqPerConn";
    private static final String CHECK_INTERVAL = "checkInterval";
    private static final String TCP_CHECK = "tcpCheck";
    private static final String HTTP_CHECK = "httpCheck";
    private static final String TTL_CHECK = "ttlCheck";
    private static final String WAIT = "wait";
    private static final String TIMEOUT_BUFFER = "timeoutBuffer";
    private static final String ENABLE_HTTP2 = "enableHttp2";
    private static final String CONNECTION_TIMEOUT = "connectionTimeout";
    private static final String REQUEST_TIMEOUT = "requestTimeout";
    private static final String RECONNECT_INTERVAL = "reconnectInterval";
    private static final String RECONNECT_JITTER = "reconnectJitter";
    private static final String LOOKUP_INTERVAL = "lookupInterval";
    private static final String MAX_ATTEMPTS_BEFORE_SHUTDOWN = "maxAttemptsBeforeShutdown";
    private static final String SHUTDOWN_IF_THREAD_FROZEN = "shutdownIfThreadFrozen";

    private Map<String, Object> mappedConfig;
    private final Config config;

    String consulUrl;
    String consulToken;
    int maxReqPerConn;
    String deregisterAfter;
    //the time period that consul determines health status of the server.
    String checkInterval;
    boolean tcpCheck;
    boolean httpCheck;
    boolean ttlCheck;
    boolean enableHttp2;
    String wait = "600s";                   // length of blocking query with Consul
    String timeoutBuffer = "5s";            // An additional amount of time to wait for Consul to respond (to account for network latency)
    int connectionTimeout = 5;             // Consul connection timeout in seconds
    int requestTimeout = 5;                // Consul request timeout in seconds (excluding /v1/health/service)
    int reconnectInterval = 2;             // Time to wait in seconds between reconnect attempts when Consul connection fails
    int reconnectJitter = 2;               // Random seconds in [0..reconnectJitter) added to reconnectInterval
    int lookupInterval = 15;               // Time in seconds between blocking queries with Consul
    int maxAttemptsBeforeShutdown = -1;    // Max number of failed Consul reconnection attempts before self-termination
                                            // -1 means an infinite # of attempts
    boolean shutdownIfThreadFrozen = false; // Shuts down host application if any Consul lookup thread stops reporting a
                                            // heartbeat for 2 * ( lookupInterval + wait (s) + timeoutBuffer (s) ) seconds

    private ConsulConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }
    private ConsulConfig() {
        this(CONFIG_NAME);
    }

    public static ConsulConfig load(String configName) {
        return new ConsulConfig(configName);
    }

    public static ConsulConfig load() {
        return new ConsulConfig();
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
    }

    public void reload(String configName) {
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    public String getConsulUrl() {
        return consulUrl;
    }

    public void setConsulUrl(String consulUrl) {
        this.consulUrl = consulUrl;
    }

    public String getConsulToken() {
        return consulToken;
    }

    public void setConsulToken(String consulToken) {
        this.consulToken = consulToken;
    }

    public int getMaxReqPerConn() { return maxReqPerConn; }

    public void setMaxReqPerConn(int maxReqPerConn) { this.maxReqPerConn = maxReqPerConn; }

    public String getDeregisterAfter() {
        return deregisterAfter;
    }

    public void setDeregisterAfter(String deregisterAfter) {
        this.deregisterAfter = deregisterAfter;
    }

    public String getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(String checkInterval) {
        this.checkInterval = checkInterval;
    }

    public boolean isTcpCheck() {
        return tcpCheck;
    }

    public void setTcpCheck(boolean tcpCheck) {
        this.tcpCheck = tcpCheck;
    }

    public boolean isHttpCheck() {
        return httpCheck;
    }

    public void setHttpCheck(boolean httpCheck) {
        this.httpCheck = httpCheck;
    }

    public boolean isTtlCheck() {
        return ttlCheck;
    }

    public void setTtlCheck(boolean ttlCheck) {
        this.ttlCheck = ttlCheck;
    }
    public String getWait() {
        return wait;
    }

    public void setWait(String wait) {
        this.wait = wait;
    }

    public String getTimeoutBuffer() { return timeoutBuffer; }

    public void setTimeoutBuffer(String timeoutBuffer) { this.timeoutBuffer = timeoutBuffer; }

    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public void setEnableHttp2(boolean enableHttp2) {
        this.enableHttp2 = enableHttp2;
    }

    public long getConnectionTimeout() { return connectionTimeout; }

    public long getRequestTimeout() { return requestTimeout; }

    public long getReconnectInterval() { return reconnectInterval; }

    public long getReconnectJitter() { return reconnectJitter; }

    public long getLookupInterval() { return lookupInterval; }

    public long getMaxAttemptsBeforeShutdown() { return maxAttemptsBeforeShutdown; }

    public boolean isShutdownIfThreadFrozen() { return shutdownIfThreadFrozen; }

    private void setConfigData() {
        Object object = mappedConfig.get(CONSUL_URL);
        if(object != null) {
            consulUrl = (String)object;
        }
        object = mappedConfig.get(CONSUL_TOKEN);
        if(object != null) {
            consulToken = (String)object;
        }
        object = mappedConfig.get(MAX_REQ_PER_CONN);
        if(object != null) maxReqPerConn = Config.loadIntegerValue(MAX_REQ_PER_CONN, object);
        object = mappedConfig.get(CHECK_INTERVAL);
        if(object != null) {
            checkInterval = (String)object;
        }
        object = mappedConfig.get(TCP_CHECK);
        if(object != null) tcpCheck = Config.loadBooleanValue(TCP_CHECK, object);
        object = mappedConfig.get(HTTP_CHECK);
        if(object != null) httpCheck = Config.loadBooleanValue(HTTP_CHECK, object);
        object = mappedConfig.get(TTL_CHECK);
        if(object != null) ttlCheck = Config.loadBooleanValue(TTL_CHECK, object);
        object = mappedConfig.get(WAIT);
        if(object != null) {
            wait = (String)object;
        }
        object = mappedConfig.get(TIMEOUT_BUFFER);
        if(object != null) {
            timeoutBuffer = (String)object;
        }
        object = mappedConfig.get(ENABLE_HTTP2);
        if(object != null) enableHttp2 = Config.loadBooleanValue(ENABLE_HTTP2, object);
        object = mappedConfig.get(CONNECTION_TIMEOUT);
        if(object != null) connectionTimeout = Config.loadIntegerValue(CONNECTION_TIMEOUT, object);
        object = mappedConfig.get(REQUEST_TIMEOUT);
        if(object != null) requestTimeout = Config.loadIntegerValue(REQUEST_TIMEOUT, object);
        object = mappedConfig.get(RECONNECT_INTERVAL);
        if(object != null) reconnectInterval = Config.loadIntegerValue(RECONNECT_INTERVAL, object);
        object = mappedConfig.get(RECONNECT_JITTER);
        if(object != null) reconnectJitter = Config.loadIntegerValue(RECONNECT_JITTER, object);
        object = mappedConfig.get(LOOKUP_INTERVAL);
        if(object != null) lookupInterval = Config.loadIntegerValue(LOOKUP_INTERVAL, object);
        object = mappedConfig.get(MAX_ATTEMPTS_BEFORE_SHUTDOWN);
        if(object != null) maxAttemptsBeforeShutdown = Config.loadIntegerValue(MAX_ATTEMPTS_BEFORE_SHUTDOWN, object);
        object = mappedConfig.get(SHUTDOWN_IF_THREAD_FROZEN);
        if(object != null) shutdownIfThreadFrozen = Config.loadBooleanValue(SHUTDOWN_IF_THREAD_FROZEN, object);
    }

}
