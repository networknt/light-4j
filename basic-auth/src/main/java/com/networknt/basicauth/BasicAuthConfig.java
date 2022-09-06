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

package com.networknt.basicauth;

import com.networknt.common.DecryptUtil;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicAuthConfig {
    public static final String CONFIG_NAME = "basic-auth";
    private static final String ENABLED = "enabled";
    private static final String ALLOW_ANONYMOUS = "allowAnonymous";
    private static final String ALLOW_BEARER_TOKEN = "allowBearerToken";
    private static final String USERS = "users";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String PATHS = "paths";
    public static final String ANONYMOUS = "anonymous";
    public static final String BEARER = "bearer";

    boolean enabled;
    boolean allowAnonymous;
    boolean allowBearerToken;
    Map<String, UserAuth> users;  // the key is the username to locate the object
    private Config config;
    private Map<String, Object> mappedConfig;

    public BasicAuthConfig() {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setConfigUser();
    }
    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    public BasicAuthConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigUser();
    }

    static BasicAuthConfig load() {
        return new BasicAuthConfig();
    }

    static BasicAuthConfig load(String configName) {
        return new BasicAuthConfig(configName);
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setConfigUser();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAllowAnonymous() {
        return allowAnonymous;
    }

    public void setAllowAnonymous(boolean allowAnonymous) {
        this.allowAnonymous = allowAnonymous;
    }

    public boolean isAllowBearerToken() {
        return allowBearerToken;
    }

    public void setAllowBearerToken(boolean allowBearerToken) {
        this.allowBearerToken = allowBearerToken;
    }

    public Map<String, UserAuth> getUsers() { return users; }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if(object != null && (Boolean) object) {
            setEnabled(true);
        }
        object = mappedConfig.get(ALLOW_ANONYMOUS);
        if(object != null && (Boolean) object) {
            setAllowAnonymous(true);
        }
        object = mappedConfig.get(ALLOW_BEARER_TOKEN);
        if(object != null && (Boolean) object) {
            setAllowBearerToken(true);
        }
    }

    private void setConfigUser() {
        if (mappedConfig.get(USERS) instanceof List) {
            List<Map<String, Object>> userList = (List) mappedConfig.get(USERS);
            populateUsers(userList);
        } else if (mappedConfig.get(USERS) instanceof String) {
            // The value can be a string from the config server or in values.yml
            // It must start with '[' in the beginning.
            String s = (String)mappedConfig.get(USERS);
            s = s.trim();
            if(!s.startsWith("[")) {
                throw new ConfigException("The string value must be start with [ as a JSON list");
            }
            List<Map<String, Object>> userList = JsonMapper.string2List(s);
            populateUsers(userList);
        } else {
            // if the basic auth is enabled and users is empty, we throw the ConfigException.
            if(enabled) {
                throw new ConfigException("Basic Auth is enabled but there is no users definition.");
            }
        }
    }

    private void populateUsers(List<Map<String, Object>> userList) {
        users = new HashMap<>();
        userList.forEach(user -> {
            if (user instanceof Map) {
                // the password might be encrypted.
                user = DecryptUtil.decryptMap(user);
                UserAuth userAuth = new UserAuth();
                user.forEach((k, v) -> {
                    if (USERNAME.equals(k)) {
                        userAuth.setUsername((String) v);
                    }
                    if (PASSWORD.equals(k)) {
                        userAuth.setPassword((String) v);
                    }
                    if (PATHS.equals(k)) {
                        if (v instanceof List) {
                            userAuth.setPaths((List) v);
                        } else {
                            throw new ConfigException("Paths must be an array of strings.");
                        }
                    }
                });
                users.put(userAuth.getUsername(), userAuth);
            }
        });
    }
}
