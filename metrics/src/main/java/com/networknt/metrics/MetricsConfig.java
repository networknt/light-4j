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

package com.networknt.metrics;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;

import java.util.Map;

/**
 * Metrics middleware handler configuration that is mapped to all properties in metrics.yml config file.
 * This config file is shared by all implementations of the push type metrics handlers, and it supports
 * config reload from the control pane.
 *
 * @author Steve Hu
 */
public class MetricsConfig {
    public static final String CONFIG_NAME = "metrics";
    private static final String ENABLED = "enabled";
    private static final String ENABLED_JVM_MONITOR = "enableJVMMonitor";
    private static final String SERVER_PROTOCOL = "serverProtocol";
    private static final String SERVER_HOST = "serverHost";
    private static final String SERVER_PORT = "serverPort";
    private static final String SERVER_PATH = "serverPath";
    private static final String SERVER_NAME = "serverName";
    private static final String SERVER_USER = "serverUser";
    private static final String SERVER_PASS = "serverPass";
    private static final String REPORT_IN_MINUTES = "reportInMinutes";
    private static final String PRODUCT_NAME = "productName";
    private static final String SEND_SCOPE_CLIENT_ID = "sendScopeClientId";
    private static final String SEND_CALLER_ID = "sendCallerId";
    private static final String SEND_ISSUER = "sendIssuer";
    private static final String ISSUER_REGEX = "issuerRegex";
    boolean enabled;
    boolean enableJVMMonitor;
    String serverProtocol;
    String serverHost;
    int serverPort;
    String serverPath;
    String serverName;
    String serverUser;
    String serverPass;
    int reportInMinutes;
    String productName;
    boolean sendScopeClientId;
    boolean sendCallerId;
    boolean sendIssuer;
    String issuerRegex;

    private Map<String, Object> mappedConfig;
    private final Config config;


    private MetricsConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private MetricsConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    static MetricsConfig load() {
        return new MetricsConfig();
    }

    static MetricsConfig load(String configName) {
        return new MetricsConfig(configName);
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnableJVMMonitor() {
        return enableJVMMonitor;
    }

    public void setEnableJVMMonitor(boolean enableJVMMonitor) {
        this.enableJVMMonitor = enableJVMMonitor;
    }

    public String getServerHost() {
        return serverHost;
    }

    public String getServerProtocol() {
        return serverProtocol;
    }

    public void setServerProtocol(String serverProtocol) {
        this.serverProtocol = serverProtocol;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public int getReportInMinutes() {
        return reportInMinutes;
    }

    public void setReportInMinutes(int reportInMinutes) {
        this.reportInMinutes = reportInMinutes;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getServerName() { return serverName; }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerUser() {
        return serverUser;
    }

    public void setServerUser(String serverUser) {
        this.serverUser = serverUser;
    }

    public String getServerPass() {
        return serverPass;
    }

    public void setServerPass(String serverPass) {
        this.serverPass = serverPass;
    }

    public String getServerPath() {
        return serverPath;
    }

    public void setServerPath(String serverPath) {
        this.serverPath = serverPath;
    }

    public boolean isSendScopeClientId() {
        return sendScopeClientId;
    }

    public void setSendScopeClientId(boolean sendScopeClientId) {
        this.sendScopeClientId = sendScopeClientId;
    }

    public boolean isSendCallerId() {
        return sendCallerId;
    }

    public void setSendCallerId(boolean sendCallerId) {
        this.sendCallerId = sendCallerId;
    }

    public boolean isSendIssuer() {
        return sendIssuer;
    }

    public void setSendIssuer(boolean sendIssuer) {
        this.sendIssuer = sendIssuer;
    }

    public String getIssuerRegex() {
        return issuerRegex;
    }

    public void setIssuerRegex(String issuerRegex) {
        this.issuerRegex = issuerRegex;
    }

    Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
        Object object = getMappedConfig().get(ENABLED);
        if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
        object = getMappedConfig().get(ENABLED_JVM_MONITOR);
        if(object != null) enableJVMMonitor = Config.loadBooleanValue(ENABLED_JVM_MONITOR, object);
        object = mappedConfig.get(SERVER_PROTOCOL);
        if (object != null) setServerProtocol((String) object);;
        object = mappedConfig.get(SERVER_HOST);
        if (object != null) serverHost = (String) object;
        object = mappedConfig.get(SERVER_PORT);
        if (object != null) serverPort = Config.loadIntegerValue(SERVER_PORT, object);
        object = getMappedConfig().get(SERVER_PATH);
        if(object != null) serverPath = (String) object;
        object = getMappedConfig().get(SERVER_NAME);
        if(object != null) serverName = (String) object;
        object = getMappedConfig().get(SERVER_USER);
        if(object != null) serverUser = (String) object;
        object = getMappedConfig().get(SERVER_PASS);
        if(object != null) serverPass = (String) object;
        object = getMappedConfig().get(REPORT_IN_MINUTES);
        if(object != null) reportInMinutes = Config.loadIntegerValue(REPORT_IN_MINUTES, object);
        object = getMappedConfig().get(PRODUCT_NAME);
        if(object != null) productName = (String) object;
        object = getMappedConfig().get(SEND_SCOPE_CLIENT_ID);
        if(object != null) sendScopeClientId = Config.loadBooleanValue(SEND_SCOPE_CLIENT_ID, object);
        object = getMappedConfig().get(SEND_CALLER_ID);
        if(object != null) sendCallerId = Config.loadBooleanValue(SEND_CALLER_ID, object);
        object = getMappedConfig().get(SEND_ISSUER);
        if(object != null) sendIssuer = Config.loadBooleanValue(SEND_ISSUER, object);
        object = getMappedConfig().get(ISSUER_REGEX);
        if(object != null) issuerRegex = (String) object;
    }
}
