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

public class ConsulConfig {
    public static final String CONFIG_NAME = "consul";
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
    long connectionTimeout = 5;             // Consul connection timeout in seconds
    long requestTimeout = 5;                // Consul request timeout in seconds (excluding /v1/health/service)
    long reconnectInterval = 2;             // Time to wait in seconds between reconnect attempts when Consul connection fails
    long reconnectJitter = 2;               // Random seconds in [0..reconnectJitter) added to reconnectInterval
    long lookupInterval = 15;               // Time in seconds between blocking queries with Consul
    long maxAttemptsBeforeShutdown = -1;    // Max number of failed Consul reconnection attempts before self-termination
                                            // -1 means an infinite # of attempts
    boolean shutdownIfThreadFrozen = false; // Shuts down host application if any Consul lookup thread stops reporting a
                                            // heartbeat for 2 * ( lookupInterval + wait (s) + timeoutBuffer (s) ) seconds

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
}
