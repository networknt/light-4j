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
import com.networknt.server.ModuleRegistry;

import java.util.List;
import java.util.Map;

@ConfigSchema(configKey = "portalRegistry", configName = "portal-registry", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD})
public class PortalRegistryConfig {
    public static final String CONFIG_NAME = "portal-registry";

    private static final String PORTAL_URL = "portalUrl";
    private static final String PORTAL_TOKEN = "portalToken";

    private static volatile PortalRegistryConfig instance;
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
        return load(CONFIG_NAME);
    }

    public static PortalRegistryConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (PortalRegistryConfig.class) {
                mappedConfig = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new PortalRegistryConfig(configName);
                ModuleRegistry.registerModule(CONFIG_NAME, PortalRegistryConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(CONFIG_NAME), List.of(PORTAL_TOKEN));
                return instance;
            }
        }
        return new PortalRegistryConfig(configName);
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    @StringField(
            configFieldName = "portalUrl",
            externalizedKeyName = "portalUrl",
            defaultValue = "https://lightapi.net",
            description = "Controller base URL. For local controller-rs or light-controller development, use something like\n" +
                    "https://localhost:8443. The unified registration and discovery channel is derived as /ws/microservice."
    )
    String portalUrl;

    @StringField(
            configFieldName = "portalToken",
            externalizedKeyName = "portalToken",
            description = "RS256 service registration token sent in service/register params.jwt. The controller verifies it against its configured\n" +
                    "JWKS endpoint, for example http://localhost:6881/oauth2/AZZRJE52eXu3t1hseacnGQ/keys for the light-portal dev security\n" +
                    "provider. Today the token should identify the service with cid matching the requested serviceId. Future tokens may use sid.\n" +
                    "This value is typically provided with the LIGHT_PORTAL_AUTHORIZATION environment variable."
    )
    String portalToken;

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

    private void setConfigData() {
        Object object = mappedConfig.get(PORTAL_URL);
        if (object != null) portalUrl = (String) object;
        object = mappedConfig.get(PORTAL_TOKEN);
        if (object != null) portalToken = (String) object;
    }
}
