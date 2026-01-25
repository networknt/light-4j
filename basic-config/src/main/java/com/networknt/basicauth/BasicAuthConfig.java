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

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import com.networknt.config.schema.*;
import com.networknt.server.ModuleRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigSchema(
        configKey = "basic",
        configName = "basic-auth",
        configDescription = "Basic Authentication Security Configuration for light-4j",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
public class BasicAuthConfig {
    public static final String CONFIG_NAME = "basic-auth";
    private static final String ENABLED = "enabled";
    private static final String ENABLE_AD = "enableAD";
    private static final String ALLOW_ANONYMOUS = "allowAnonymous";
    private static final String ALLOW_BEARER_TOKEN = "allowBearerToken";
    private static final String USERS = "users";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String PATHS = "paths";
    public static final String ANONYMOUS = "anonymous";
    public static final String BEARER = "bearer";

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            defaultValue = "false",
            description = "Enable Basic Authentication Handler, default is true."
    )
    boolean enabled;

    @BooleanField(
            configFieldName = ENABLE_AD,
            externalizedKeyName = ENABLE_AD,
            defaultValue = "true",
            description = "Enable Ldap Authentication, default is true."
    )
    boolean enableAD;

    @BooleanField(
            configFieldName = ALLOW_ANONYMOUS,
            externalizedKeyName = ALLOW_ANONYMOUS,
            defaultValue = "false",
            description = "Do we allow the anonymous to pass the authentication and limit it with some paths\n" +
                    "to access? Default is false, and it should only be true in client-proxy."
    )
    boolean allowAnonymous;

    @BooleanField(
            configFieldName = ALLOW_BEARER_TOKEN,
            externalizedKeyName = ALLOW_BEARER_TOKEN,
            defaultValue = "false",
            description = "Allow the Bearer OAuth 2.0 token authorization to pass to the next handler with paths\n" +
                    "authorization defined under username bearer. This feature is used in proxy-client\n" +
                    "that support multiple clients with different authorizations.\n"
    )
    boolean allowBearerToken;

    @MapField(
            configFieldName = USERS,
            externalizedKeyName = USERS,
            description = "usernames and passwords in a list, the password can be encrypted like user2 in test.\n" +
                    "As we are supporting multiple users, so leave the passwords in this file with users.\n" +
                    "For each user, you can specify a list of optional paths that this user is allowed to\n" +
                    "access. A special user anonymous can be used to set the paths for client without an\n" +
                    "authorization header. The paths are optional and used for proxy only to authorize.\n",
            valueType = UserAuth.class
    )
    Map<String, UserAuth> users;  // the key is the username to locate the object

    private final Config config;
    private Map<String, Object> mappedConfig;
    private static BasicAuthConfig instance;

    private BasicAuthConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        setConfigData();
    }

    private BasicAuthConfig() {
        this(CONFIG_NAME);
    }

    public static BasicAuthConfig load() {
        return load(CONFIG_NAME);
    }

    public static BasicAuthConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (BasicAuthConfig.class) {
                mappedConfig = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new BasicAuthConfig(configName);
                // Register the module with the configuration. masking the password property.
                List<String> masks = new ArrayList<>();
                masks.add("password");
                ModuleRegistry.registerModule(configName, BasicAuthConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), masks);
                return instance;
            }
        }
        return new BasicAuthConfig(configName);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnableAD() {
        return enableAD;
    }

    public void setEnableAD(boolean enabled) {
        this.enableAD = enabled;
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

    public Map<String, UserAuth> getUsers() {
        return users;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    Config getConfig() {
        return config;
    }

    private void setConfigData() {
        if (mappedConfig != null) {
            Object object = mappedConfig.get(ENABLED);
            if (object != null) enabled = Config.loadBooleanValue(ENABLED, object);
            object = mappedConfig.get(ENABLE_AD);
            if (object != null) enableAD = Config.loadBooleanValue(ENABLE_AD, object);
            object = mappedConfig.get(ALLOW_ANONYMOUS);
            if (object != null) allowAnonymous = Config.loadBooleanValue(ALLOW_ANONYMOUS, object);
            object = mappedConfig.get(ALLOW_BEARER_TOKEN);
            if (object != null) allowBearerToken = Config.loadBooleanValue(ALLOW_BEARER_TOKEN, object);
            setConfigUser();
        }
    }

    private void setConfigUser() {
        if (mappedConfig.get(USERS) instanceof List) {
            List<Map<String, Object>> userList = (List) mappedConfig.get(USERS);
            populateUsers(userList);
        } else if (mappedConfig.get(USERS) instanceof String) {
            // The value can be a string from the config server or in values.yml
            // It must start with '[' in the beginning.
            String s = (String) mappedConfig.get(USERS);
            s = s.trim();
            if (!s.startsWith("[")) {
                throw new ConfigException("The string value must be start with [ as a JSON list");
            }
            List<Map<String, Object>> userList = JsonMapper.string2List(s);
            populateUsers(userList);
        } else {
            // if the basic auth is enabled and users is empty, we throw the ConfigException.
            if (enabled) {
                throw new ConfigException("Basic Auth is enabled but there is no users definition.");
            }
        }
    }

    private void populateUsers(List<Map<String, Object>> userList) {
        users = new HashMap<>();
        userList.forEach(user -> {
            if (user instanceof Map) {
                // the password might be encrypted.
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
