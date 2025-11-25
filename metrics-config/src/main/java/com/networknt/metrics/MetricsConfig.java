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
import com.networknt.config.schema.*;

import java.util.Map;

/**
 * Metrics middleware handler configuration that is mapped to all properties in metrics.yml config file.
 * This config file is shared by all implementations of the push type metrics handlers, and it supports
 * config reload from the control pane.
 *
 * @author Steve Hu
 */
@ConfigSchema(
        configName = "metrics",
        configKey = "metrics",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD},
        configDescription = "Metrics handler configuration that is shared by all the push metrics handlers."
)
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

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            externalized = true,
            defaultValue = "true",
            description = "If metrics handler is enabled or not. Default is true as long as one of the handlers is in the\n" +
                    "request/response chain."
    )
    boolean enabled;

    @BooleanField(
            configFieldName = ENABLED_JVM_MONITOR,
            externalizedKeyName = ENABLED_JVM_MONITOR,
            externalized = true,
            defaultValue = "false",
            description = "If metrics handler is enabled for JVM MBean or not. If enabled, the CPU and Memory usage will be\n" +
                    "collected and send to the time series database."
    )
    boolean enableJVMMonitor;

    @StringField(
            configFieldName = SERVER_PROTOCOL,
            externalizedKeyName = SERVER_PROTOCOL,
            externalized = true,
            defaultValue = "http",
            description = "Time series database server protocol. It can be http or https. Others can be added upon request."
    )
    String serverProtocol;

    @StringField(
            configFieldName = SERVER_HOST,
            externalizedKeyName = SERVER_HOST,
            externalized = true,
            defaultValue = "localhost",
            description = "Time series database or metrics server hostname."
    )
    String serverHost;

    @StringField(
            configFieldName = SERVER_PATH,
            externalizedKeyName = SERVER_PATH,
            externalized = true,
            defaultValue = "/apm/metricFeed",
            description = "Time series database or metrics server request path. It is optional and only some metrics handlers\n" +
                    "will use it. For example, the Broadcom APM metrics server needs the path to access the agent."
    )
    String serverPath;

    @IntegerField(
            configFieldName = SERVER_PORT,
            externalizedKeyName = SERVER_PORT,
            externalized = true,
            defaultValue = "8086",
            description = "Time series database or metrics server port number."
    )
    int serverPort;

    @StringField(
            configFieldName = SERVER_NAME,
            externalizedKeyName = SERVER_NAME,
            externalized = true,
            defaultValue = "metrics",
            description = "Time series database name."
    )
    String serverName;

    @StringField(
            configFieldName = SERVER_USER,
            externalizedKeyName = SERVER_USER,
            externalized = true,
            defaultValue = "admin",
            description = "Time series database or metrics server user."
    )
    String serverUser;

    @StringField(
            configFieldName = SERVER_PASS,
            externalizedKeyName = SERVER_PASS,
            externalized = true,
            defaultValue = "admin",
            description = "Time series database or metrics server password."
    )
    String serverPass;

    @IntegerField(
            configFieldName = REPORT_IN_MINUTES,
            externalizedKeyName = REPORT_IN_MINUTES,
            externalized = true,
            defaultValue = "1",
            description = "report and reset metrics in minutes."
    )
    int reportInMinutes;

    @StringField(
            configFieldName = PRODUCT_NAME,
            externalizedKeyName = PRODUCT_NAME,
            externalized = true,
            defaultValue = "http-sidecar",
            description = "This is the metrics product name for the centralized time series database. The product name will be\n" +
                    "the top level category under a Kubernetes cluster or a virtual machine. The following is the light-4j\n" +
                    "product list. http-sidecar, kafka-sidecar, corp-gateway, aiz-gateway, proxy-server, proxy-client,\n" +
                    "proxy-lambda, light-balancer etc. By default, http-sidecar is used as a placeholder. Please change it\n" +
                    "based on your usage in the values.yml file."
    )
    String productName;

    @BooleanField(
            configFieldName = SEND_SCOPE_CLIENT_ID,
            externalizedKeyName = SEND_SCOPE_CLIENT_ID,
            externalized = true,
            defaultValue = "false",
            description = "A flag to indicate if the scope client id will be sent as a common tag. If it is true, try to retrieve\n" +
                    "it from the audit info and send it if it is not null. If it does not exist, \"unknown\" will be sent.\n" +
                    "By default, this tag is not sent regardless if it is in the audit info. You only enable this if your\n" +
                    "API will be accessed by a Mobile or SPA application with authorization code flow. In this case, the\n" +
                    "primary token is the authorization code token that contains user info and the secondary scope token\n" +
                    "is the client_credentials token from the immediate caller service in the invocation chain."
    )
    boolean sendScopeClientId;

    @BooleanField(
            configFieldName = SEND_CALLER_ID,
            externalizedKeyName = SEND_CALLER_ID,
            externalized = true,
            defaultValue = "false",
            description = "A flag to indicate if the caller id will be sent as a common tag. If it is true, try to retrieve it\n" +
                    "from the audit info and send it if it is not null. If it doesn't exist, \"unknown\" will be sent.\n" +
                    "By default, this tag is not sent regardless if it is in the audit info. The purpose of this tag is\n" +
                    "similar to the scopeClientId to identify the immediate caller service in a microservice application.\n" +
                    "As the scopeClientId is only available when the scope token is used, it cannot be used for all apps.\n" +
                    "light-4j client module has a config to enforce all services to send the callerId to the downstream\n" +
                    "API, and it can be enforced within an organization. In most cases, this callerId is more reliable."
    )
    boolean sendCallerId;

    @BooleanField(
            configFieldName = SEND_ISSUER,
            externalizedKeyName = SEND_ISSUER,
            externalized = true,
            defaultValue = "false",
            description = "A flag to indicate if the issuer will be sent as a common tag. If it is true, try to retrieve it\n" +
                    "from the audit info and send it if it is not null. If it doesn't exist, \"unknown\" will be sent.\n" +
                    "By default, this tag is not sent regardless if it is in the audit info. This tag should only be\n" +
                    "sent if the organization uses multiple OAuth 2.0 providers. For example, Okta will provide multiple\n" +
                    "virtual instances, so each service can have its private OAuth 2.0 provider. If all services are\n" +
                    "sharing the same OAuth 2.0 provide (same issuer in the token), this tag should not be used."
    )
    boolean sendIssuer;

    @StringField(
            configFieldName = ISSUER_REGEX,
            externalizedKeyName = ISSUER_REGEX,
            externalized = true,
            description = "If issuer is sent, it might be necessary to extract only partial of the string with a regex pattern.\n" +
                    "For example, Okta iss is something like: \"https://networknt.oktapreview.com/oauth2/aus9xt6dd1cSYyRPH1d6\"\n" +
                    "We only need to extract the last part after the last slash. The following default regex is just for it.\n" +
                    "The code in the light-4j is trying to extract the matcher.group(1) and there is a junit test to allow\n" +
                    "users to test their regex. If you are using Okat, you can set metrics.issuerRegex: /([^/]+)$\n" +
                    "By default, the regex is empty, and the original iss will be sent as a tag."
    )
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

    public static MetricsConfig load() {
        return new MetricsConfig();
    }

    public static MetricsConfig load(String configName) {
        return new MetricsConfig(configName);
    }

    public void reload() {
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
