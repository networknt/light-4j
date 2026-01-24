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

package com.networknt.portal.registry;

import com.networknt.config.Config;
import com.networknt.config.schema.*;

@ConfigSchema(configKey = "portalRegistry", configName = "portal-registry", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD})
public class PortalRegistryConfig {
    public static final String CONFIG_NAME = "portal-registry";

    private static final String PORTAL_URL = "portalUrl";
    private static final String PORTAL_TOKEN = "portalToken";
    private static final String MAX_REQ_PER_CONN = "maxReqPerConn";
    private static final String DEREGISTER_AFTER = "deregisterAfter";
    private static final String CHECK_INTERVAL = "checkInterval";
    private static final String HTTP_CHECK = "httpCheck";
    private static final String TTL_CHECK = "ttlCheck";
    private static final String HEALTH_PATH = "healthPath";

    private final Config config;
    private java.util.Map<String, Object> mappedConfig;

    private PortalRegistryConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        if (mappedConfig != null) {
            setConfigData();
        }
    }

    private PortalRegistryConfig() {
        this(CONFIG_NAME);
    }

    public static PortalRegistryConfig load() {
        return new PortalRegistryConfig();
    }

    public static PortalRegistryConfig load(String configName) {
        return new PortalRegistryConfig(configName);
    }

    @StringField(
            configFieldName = "portalUrl",
            externalizedKeyName = "portalUrl",
            defaultValue = "https://lightapi.net",
            description = "Portal URL for accessing controller API. Default to lightapi.net public portal, and it can be pointed to a standalone\n" +
                    "light-controller instance for testing in the same Kubernetes cluster or docker-compose."
    )
    String portalUrl;

    @StringField(
            configFieldName = "portalToken",
            externalizedKeyName = "portalToken",
            description = "Bootstrap jwt token to access the light-controller. In most case, the pipeline will get the token from OAuth 2.0\n" +
                    "provider during the deployment. And then pass the token to the container with an environment variable. The other\n" +
                    "option is to use the light-4j encyptor to encrypt token and put it into the values.yml in the config server. In\n" +
                    "that case, you can use portalRegistry.portalToken as the key instead of the environment variable."
    )
    String portalToken;

    @IntegerField(
            configFieldName = "maxReqPerConn",
            externalizedKeyName = "maxReqPerConn",
            defaultValue = "1000000",
            description = "number of requests before resetting the shared connection to work around HTTP/2 limitation"
    )
    int maxReqPerConn;

    @IntegerField(
            configFieldName = "deregisterAfter",
            externalizedKeyName = "deregisterAfter",
            defaultValue = "120000",
            description = "De-register the service after the amount of time with health check failed. Once a health check is failed, the\n" +
                    "service will be put into a critical state. After the deregisterAfter, the service will be removed from discovery.\n" +
                    "the value is an integer in milliseconds. 1000 means 1 second and default to 2 minutes"
    )
    int deregisterAfter;

    @IntegerField(
            configFieldName = "checkInterval",
            externalizedKeyName = "checkInterval",
            defaultValue = "10000",
            description = "health check interval for HTTP check. Or it will be the TTL for TTL check. Every 10 seconds, an HTTP check\n" +
                    "request will be sent from the light-portal controller. Or if there is no heartbeat TTL request from service\n" +
                    "after 10 seconds, then mark the service is critical. The value is an integer in milliseconds"
    )
    int checkInterval;

    @BooleanField(
            configFieldName = "httpCheck",
            externalizedKeyName = "httpCheck",
            defaultValue = "false",
            description = "enable health check HTTP. An HTTP get request will be sent to the service to ensure that 200 response status is\n" +
                    "coming back. This is suitable for service that depending on the database or other infrastructure services. You should\n" +
                    "implement a customized health check handler that checks dependencies. i.e. if DB is down, return status 400. This\n" +
                    "is the recommended configuration that allows the light-portal controller to poll the health info from each service."
    )
    boolean httpCheck;

    @BooleanField(
            configFieldName = "ttlCheck",
            externalizedKeyName = "ttlCheck",
            defaultValue = "true",
            description = "enable health check TTL. When this is enabled, The light-portal controller won't actively check your service to\n" +
                    "ensure it is healthy, but your service will call check endpoint with a heartbeat to indicate it is alive. This\n" +
                    "requires that the service is built on top of light-4j, and the HTTP check is not available. For example, your service\n" +
                    "is behind NAT. If you are running the service within your internal network and using the SaaS lightapi.net portal,\n" +
                    "this is the only option as our portal controller cannot access your internal service to perform a health check.\n" +
                    "We recommend deploying light-portal internally if you are running services within an internal network for efficiency.\n"
    )
    boolean ttlCheck;

    @StringField(
            configFieldName = "healthPath",
            externalizedKeyName = "healthPath",
            defaultValue = "/health/",
            description = "The health check path implemented on the server. In most of the cases, it would be /health/ plus the serviceId;\n" +
                    "however, on a kubernetes cluster, it might be /health/liveness/ in order to differentiate from the /health/readiness/\n" +
                    "Note that we need to provide the leading and trailing slash in the path definition."
    )
    String healthPath;

    public String getPortalUrl() {
        return portalUrl;
    }

    public void setPortalUrl(String portalUrl) {
        this.portalUrl = portalUrl;
    }

    public String getPortalToken() {
        return portalToken;
    }

    public void setPortalToken(String portalToken) {
        this.portalToken = portalToken;
    }

    public int getMaxReqPerConn() {
        return maxReqPerConn;
    }

    public void setMaxReqPerConn(int maxReqPerConn) {
        this.maxReqPerConn = maxReqPerConn;
    }

    public int getDeregisterAfter() {
        return deregisterAfter;
    }

    public void setDeregisterAfter(int deregisterAfter) {
        this.deregisterAfter = deregisterAfter;
    }

    public int getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(int checkInterval) {
        this.checkInterval = checkInterval;
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

    public String getHealthPath() {
        return healthPath;
    }

    public void setHealthPath(String healthPath) {
        this.healthPath = healthPath;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(PORTAL_URL);
        if (object != null) portalUrl = (String) object;
        object = mappedConfig.get(PORTAL_TOKEN);
        if (object != null) portalToken = (String) object;
        object = mappedConfig.get(MAX_REQ_PER_CONN);
        if (object != null) maxReqPerConn = Config.loadIntegerValue(MAX_REQ_PER_CONN, object);
        object = mappedConfig.get(DEREGISTER_AFTER);
        if (object != null) deregisterAfter = Config.loadIntegerValue(DEREGISTER_AFTER, object);
        object = mappedConfig.get(CHECK_INTERVAL);
        if (object != null) checkInterval = Config.loadIntegerValue(CHECK_INTERVAL, object);
        object = mappedConfig.get(HTTP_CHECK);
        if (object != null) httpCheck = Config.loadBooleanValue(HTTP_CHECK, object);
        object = mappedConfig.get(TTL_CHECK);
        if (object != null) ttlCheck = Config.loadBooleanValue(TTL_CHECK, object);
        object = mappedConfig.get(HEALTH_PATH);
        if (object != null) healthPath = (String) object;
    }
}
