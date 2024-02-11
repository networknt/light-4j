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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ServerInfoConfig {
    private static final Logger logger = LoggerFactory.getLogger(ServerInfoConfig.class);

    public static final String CONFIG_NAME = "info";
    public static final String ENABLE_SERVER_INFO = "enableServerInfo";
    public static final String KEYS_TO_NOT_SORT = "keysToNotSort";
    private Map<String, Object> mappedConfig;
    private final Config config;
    boolean enableServerInfo;
    List<String> keysToNotSort;

    private ServerInfoConfig() {
        this(CONFIG_NAME);
    }

    private ServerInfoConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setData();
        setList();
    }

    public static ServerInfoConfig load() {
        return new ServerInfoConfig();
    }

    public static ServerInfoConfig load(String configName) {
        return new ServerInfoConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setData();
        setList();
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

    private void setData() {
        Object object = mappedConfig.get(ENABLE_SERVER_INFO);
        if(object != null) enableServerInfo = Config.loadBooleanValue(ENABLE_SERVER_INFO, object);
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
