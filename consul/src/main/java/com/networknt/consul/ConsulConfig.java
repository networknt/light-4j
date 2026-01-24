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
import com.networknt.config.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ConfigSchema(configKey = "consul", configName = "consul", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD})
public class ConsulConfig {
    public static final String CONFIG_NAME = "consul";
    private static final Logger logger = LoggerFactory.getLogger(ConsulConfig.class);
    private static final String CONSUL_URL = "consulUrl";
    private static final String DEREGISTER_AFTER = "deregisterAfter";
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


    @StringField(
            configFieldName = CONSUL_URL,
            externalizedKeyName = CONSUL_URL,
            defaultValue = "http://localhost:8500",
            description = "Consul URL for accessing APIs"
    )
    String consulUrl;

    @StringField(
            configFieldName = CONSUL_TOKEN,
            externalizedKeyName = CONSUL_TOKEN,
            defaultValue = "the_one_ring",
            description = "access token to the consul server"
    )
    String consulToken;

    @IntegerField(
            configFieldName = MAX_REQ_PER_CONN,
            externalizedKeyName = MAX_REQ_PER_CONN,
            defaultValue = "1000000",
            description = "number of requests before reset the shared connection."
    )
    int maxReqPerConn;

    @StringField(
            configFieldName = DEREGISTER_AFTER,
            externalizedKeyName = DEREGISTER_AFTER,
            pattern = "^\\d+[smh]$",
            defaultValue = "2m",
            description = "deregister the service after the amount of time after health check failed."
    )
    String deregisterAfter;

    @StringField(
            configFieldName = CHECK_INTERVAL,
            externalizedKeyName = CHECK_INTERVAL,
            pattern = "^\\d+[smh]$",
            defaultValue = "10s",
            description = "health check interval for TCP or HTTP check. Or it will be the TTL for TTL check. Every 10 seconds,\n" +
                    "TCP or HTTP check request will be sent. Or if there is no heart beat request from service after 10 seconds,\n" +
                    "then mark the service is critical."
    )
    String checkInterval;

    @BooleanField(
            configFieldName = TCP_CHECK,
            externalizedKeyName = TCP_CHECK,
            defaultValue = "false",
            description = "One of the following health check approach will be selected. Two passive (TCP and HTTP) and one active (TTL)\n" +
                    "enable health check TCP. Ping the IP/port to ensure that the service is up. This should be used for most of\n" +
                    "the services with simple dependencies. If the port is open on the address, it indicates that the service is up."
    )
    boolean tcpCheck;

    @BooleanField(
            configFieldName = HTTP_CHECK,
            externalizedKeyName = HTTP_CHECK,
            defaultValue = "false",
            description = "enable health check HTTP. A http get request will be sent to the service to ensure that 200 response status is\n" +
                    "coming back. This is suitable for service that depending on database or other infrastructure services. You should\n" +
                    "implement a customized health check handler that checks dependencies. i.e. if db is down, return status 400."
    )
    boolean httpCheck;

    @BooleanField(
            configFieldName = TTL_CHECK,
            externalizedKeyName = TTL_CHECK,
            defaultValue = "true",
            description = "enable health check TTL. When this is enabled, Consul won't actively check your service to ensure it is healthy,\n" +
                    "but your service will call check endpoint with heart beat to indicate it is alive. This requires that the service\n" +
                    "is built on top of light-4j and the above options are not available. For example, your service is behind NAT."
    )
    boolean ttlCheck;

    @StringField(
            configFieldName = WAIT,
            externalizedKeyName = WAIT,
            defaultValue = "600s",
            pattern = "^\\d+[smh]$",
            description = "endpoints that support blocking will also honor a wait parameter specifying a maximum duration for the blocking request.\n" +
                    "This is limited to 10 minutes.This value can be specified in the form of \"10s\" or \"5m\" (i.e., 10 seconds or 5 minutes,\n" +
                    "respectively)."
    )
    String wait = "600s";

    @StringField(
            configFieldName = TIMEOUT_BUFFER,
            externalizedKeyName = TIMEOUT_BUFFER,
            defaultValue = "5s",
            pattern = "^\\d+[smh]$",
            description = "Additional buffer of time to allow Consul to terminate the blocking query connection."
    )
    String timeoutBuffer = "5s";

    @BooleanField(
            configFieldName = ENABLE_HTTP2,
            externalizedKeyName = ENABLE_HTTP2,
            defaultValue = "false",
            description = "enable HTTP/2\n" +
                    "must disable when using HTTP with Consul (mostly using local Consul agent), Consul only supports HTTP/1.1 when not using TLS\n" +
                    "optional to enable when using HTTPS with Consul, it will have better performance"
    )
    boolean enableHttp2;

    @IntegerField(
            configFieldName = CONNECTION_TIMEOUT,
            externalizedKeyName = CONNECTION_TIMEOUT,
            defaultValue = "5",
            description = "Consul connection establishment timeout in seconds"
    )
    int connectionTimeout = 5;

    @IntegerField(
            configFieldName = REQUEST_TIMEOUT,
            externalizedKeyName = REQUEST_TIMEOUT,
            defaultValue = "5",
            description = "Consul request completion timeout in seconds\n" +
                    "This does NOT apply to Consul service discovery lookups (see the 'wait' and 'timeoutBuffer' properties for that)"
    )
    int requestTimeout = 5;

    @IntegerField(
            configFieldName = RECONNECT_INTERVAL,
            externalizedKeyName = RECONNECT_INTERVAL,
            defaultValue = "2",
            description = "Time to wait in seconds between reconnect attempts when Consul connection fails"
    )
    int reconnectInterval = 2;

    @IntegerField(
            configFieldName = RECONNECT_JITTER,
            externalizedKeyName = RECONNECT_JITTER,
            defaultValue = "2",
            description = "A random number of seconds in between 0 and reconnectJitter added to reconnectInterval (to avoid too many reconnect\n" +
                    "requests at one time)"
    )
    int reconnectJitter = 2;

    @IntegerField(
            configFieldName = LOOKUP_INTERVAL,
            externalizedKeyName = LOOKUP_INTERVAL,
            defaultValue = "30",
            description = "Time in seconds between blocking queries with Consul. Consul blocking queries time should be set via the\n" +
                    "'lookupInterval' parameter in consul.yml, instead of 'registrySessionTimeout' in service.yml"
    )
    int lookupInterval = 30;

    @IntegerField(
            configFieldName = MAX_ATTEMPTS_BEFORE_SHUTDOWN,
            externalizedKeyName = MAX_ATTEMPTS_BEFORE_SHUTDOWN,
            defaultValue = "-1",
            description = "Max number of failed Consul connection or request attempts before self-termination\n" +
                    "-1 means an infinite # of attempts are allowed"
    )
    int maxAttemptsBeforeShutdown = -1;


    @BooleanField(
            configFieldName = SHUTDOWN_IF_THREAD_FROZEN,
            externalizedKeyName = SHUTDOWN_IF_THREAD_FROZEN,
            defaultValue = "false",
            description = "Shuts down host application if any Consul lookup thread stops reporting a heartbeat for\n" +
                    "2 * ( lookupInterval + wait (in seconds) + timeoutBuffer (in seconds) ) seconds"
    )
    boolean shutdownIfThreadFrozen = false;


    private ConsulConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
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
        if(mappedConfig != null) {
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
            object = mappedConfig.get(DEREGISTER_AFTER);
            if(object != null) {
                deregisterAfter = (String)object;
            }
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

}
