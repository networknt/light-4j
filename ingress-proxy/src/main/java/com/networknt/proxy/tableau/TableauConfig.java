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

package com.networknt.proxy.tableau;

import com.networknt.config.Config;

import java.util.Map;

/**
 * Config class for TableauCacheAuthHandler
 *
 * @author Steve Hu
 */
public class TableauConfig {
    public static final String CONFIG_NAME = "tableau";
    public static final String ENABLED = "enabled";
    public static final String SERVER_URL = "serverUrl";
    public static final String SERVER_PATH = "serverPath";
    public static final String TABLEAU_USERNAME = "tableauUsername";

    boolean enabled;
    String serverUrl;
    String serverPath;
    String tableauUsername;

    private final Config config;
    private Map<String, Object> mappedConfig;

    private TableauConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private TableauConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    public static TableauConfig load() {
        return new TableauConfig();
    }

    public static TableauConfig load(String configName) {
        return new TableauConfig(configName);
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getServerPath() {
        return serverPath;
    }

    public void setServerPath(String serverPath) {
        this.serverPath = serverPath;
    }

    public String getTableauUsername() {
        return tableauUsername;
    }

    public void setTableauUsername(String tableauUsername) {
        this.tableauUsername = tableauUsername;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if (object != null) enabled = Config.loadBooleanValue(ENABLED, object);
        object = mappedConfig.get(SERVER_URL);
        if (object != null) serverUrl = (String)object;
        object = mappedConfig.get(SERVER_PATH);
        if (object != null) serverPath = (String)object;
        object = mappedConfig.get(TABLEAU_USERNAME);
        if (object != null) tableauUsername = (String)object;
    }

}
