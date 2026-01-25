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

package com.networknt.info;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.*;
import com.networknt.server.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ConfigSchema(
        configName = "info",
        configKey = "info",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD},
        configDescription = "Server info endpoint that can output environment and component along with configuration."
)
public class ServerInfoConfig {
    private static final Logger logger = LoggerFactory.getLogger(ServerInfoConfig.class);

    public static final String CONFIG_NAME = "info";
    public static final String ENABLE_SERVER_INFO = "enableServerInfo";
    public static final String KEYS_TO_NOT_SORT = "keysToNotSort";
    private static final String DOWNSTREAM_ENABLED = "downstreamEnabled";
    private static final String DOWNSTREAM_HOST = "downstreamHost";
    private static final String DOWNSTREAM_PATH = "downstreamPath";
    private Map<String, Object> mappedConfig;
    private final Config config;

    @BooleanField(
            configFieldName = ENABLE_SERVER_INFO,
            externalizedKeyName = ENABLE_SERVER_INFO,
            defaultValue = "true",
            description = "Indicate if the server info is enabled or not."
    )
    boolean enableServerInfo;

    @ArrayField(
            configFieldName = KEYS_TO_NOT_SORT,
            externalizedKeyName = KEYS_TO_NOT_SORT,
            defaultValue = "[\"admin\", \"default\", \"defaultHandlers\", \"request\", \"response\"]",
            description = "String list keys that should not be sorted in the normalized info output. If you have a list of string values\n" +
            "define in one of your config files and the sequence of the values is important, you can add the key to this list.\n" +
            "If you want to add your own keys, please make sure that you include the following default keys in your values.yml",
            items = String.class
    )
    List<String> keysToNotSort;

    @BooleanField(
            configFieldName = DOWNSTREAM_ENABLED,
            externalizedKeyName = DOWNSTREAM_ENABLED,
            description = "For some of the services like light-gateway, http-sidecar and kafka-sidecar, we might need to check the down\n" +
            "stream API before return the server info to the invoker. By default, it is not enabled.\n" +
            "if the server info needs to invoke down streams API. It is false by default.",
            defaultValue = "false"
    )
    boolean downstreamEnabled;

    @StringField(
            configFieldName = DOWNSTREAM_HOST,
            externalizedKeyName = DOWNSTREAM_HOST,
            defaultValue = "http://localhost:8081",
            description = "down stream API host. http://localhost is the default when used with http-sidecar and kafka-sidecar."
    )
    String downstreamHost;

    @StringField(
            configFieldName = DOWNSTREAM_PATH,
            externalizedKeyName = DOWNSTREAM_PATH,
            defaultValue = "/adm/server/info",
            description = "down stream API server info path. This allows the down stream API to have customized path implemented."
    )
    String downstreamPath;

    private static volatile ServerInfoConfig instance;

    private ServerInfoConfig() {
        this(CONFIG_NAME);
    }

    private ServerInfoConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        setData();
        setList();
    }

    public static ServerInfoConfig load() {
        return load(CONFIG_NAME);
    }

    public static ServerInfoConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (ServerInfoConfig.class) {
                mappedConfig = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new ServerInfoConfig(configName);
                // Register the module with the configuration.
                ModuleRegistry.registerModule(configName, ServerInfoConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), null);
                return instance;
            }
        }
        return new ServerInfoConfig(configName);
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public Config getConfig() {
        return config;
    }

    public List<String> getKeysToNotSort() {
        return keysToNotSort;
    }

    public boolean isEnableServerInfo() {
        return enableServerInfo;
    }

    public void setEnableServerInfo(boolean enableServerInfo) {
        this.enableServerInfo = enableServerInfo;
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

    private void setData() {
        Object object = mappedConfig.get(ENABLE_SERVER_INFO);
        if(object != null) enableServerInfo = Config.loadBooleanValue(ENABLE_SERVER_INFO, object);
        object = getMappedConfig().get(DOWNSTREAM_ENABLED);
        if(object != null) downstreamEnabled = Config.loadBooleanValue(DOWNSTREAM_ENABLED, object);
        object = getMappedConfig().get(DOWNSTREAM_HOST);
        if(object != null) downstreamHost = (String)object;
        object = getMappedConfig().get(DOWNSTREAM_PATH);
        if(object != null) downstreamPath = (String)object;
    }

    private void setList() {
        if(mappedConfig.get(KEYS_TO_NOT_SORT) instanceof String) {
            String s = (String)mappedConfig.get(KEYS_TO_NOT_SORT);
            s = s.trim();
            if(logger.isTraceEnabled()) logger.trace("s = " + s);
            if(s.startsWith("[")) {
                // this is a JSON string, and we need to parse it.
                try {
                    keysToNotSort = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                } catch (Exception e) {
                    throw new ConfigException("could not parse the keysToNotSort json with a list of strings.");
                }
            } else {
                // this is a comma separated string.
                keysToNotSort = Arrays.asList(s.split("\\s*,\\s*"));
            }
        } else if (getMappedConfig().get(KEYS_TO_NOT_SORT) instanceof List) {
            keysToNotSort = (List<String>) mappedConfig.get(KEYS_TO_NOT_SORT);
        } else {
            keysToNotSort = Arrays.asList("admin","default","defaultHandlers");
        }
    }
}
