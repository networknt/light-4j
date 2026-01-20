/*
 * Copyright (c) 2019 Network New Technologies Inc.
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
package com.networknt.access;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A config class for fine-grained access control for rest and hybrid frameworks.
 */
@ConfigSchema(
        configName = "access-control",
        configKey = "access-control",
        configDescription = """
                AccessControlHandler will be the last middleware handler before the proxy on the sidecar or the last
                one before the business handler to handle the fine-grained authorization in the business domain.""",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
public class AccessControlConfig {
    private static final Logger logger = LoggerFactory.getLogger(AccessControlConfig.class);
    /**
     * The name of the configuration file.
     */
    public static final String CONFIG_NAME = "access-control";
    private static final String ENABLED = "enabled";
    private static final String ACCESS_RULE_LOGIC = "accessRuleLogic";
    private static final String DEFAULT_DENY = "defaultDeny";
    private static final String SKIP_PATH_PREFIXES = "skipPathPrefixes";

    private Map<String, Object> mappedConfig;
    private final Config config;

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName =  ENABLED,
            defaultValue = "true",
            description = "Enable Access Control Handler"
    )
    boolean enabled;

    @StringField(
            configFieldName = ACCESS_RULE_LOGIC,
            externalizedKeyName = ACCESS_RULE_LOGIC,
            defaultValue = "any",
            description = """
                    If there are multiple rules, the logic to combine them can be any or all. The default is any, and it
                    means that any rule is satisfied, the access is granted. If all is set, all rules must be satisfied."""
    )
    String accessRuleLogic;

    @BooleanField(
            configFieldName = DEFAULT_DENY,
            externalizedKeyName = DEFAULT_DENY,
            defaultValue = "true",
            description = """
                    If there is no access rule defined for the endpoint, default access is denied. Users can overwrite
                    this default action by setting this config value to false. If true, the handle will force users to
                    define the rules for each endpoint when the access control handler is enabled."""
    )
    boolean defaultDeny;

    @ArrayField(
            configFieldName = SKIP_PATH_PREFIXES,
            externalizedKeyName = SKIP_PATH_PREFIXES,
            description = """
                    Define a list of path prefixes to skip the access-control to ease the configuration for the handler.yml
                    so that users can define some endpoint without fine-grained access-control security even through it uses
                    the default chain. This is useful if some endpoints want to skip the fine-grained access control in the
                    application. The format is a list of strings separated with commas or a JSON list in values.yml definition
                    from config server, or you can use yaml format in externalized access-control.yml file.""",
            items = String.class
    )
    private List<String> skipPathPrefixes;

    private AccessControlConfig() {
        this(CONFIG_NAME);
    }
    private AccessControlConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
    }

    /**
     * Loads the configuration with the default config name.
     * @return AccessControlConfig object
     */
    public static AccessControlConfig load() {
        return new AccessControlConfig(CONFIG_NAME);
    }

    /**
     * Loads the configuration with a specific config name.
     * @param configName name of the configuration file
     * @return AccessControlConfig object
     */
    public static AccessControlConfig load(String configName) {
        return new AccessControlConfig(configName);
    }

    /**
     * Reloads the configuration from the config file.
     */
    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setConfigList();
    }

    /**
     * Checks if the access control is enabled.
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets if the access control is enabled.
     * @param enabled boolean indicator
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the access rule logic (any or all).
     * @return String representation of logic
     */
    public String getAccessRuleLogic() {
        return accessRuleLogic;
    }

    /**
     * Sets the access rule logic.
     * @param accessRuleLogic String representation of logic
     */
    public void setAccessRuleLogic(String accessRuleLogic) {
        this.accessRuleLogic = accessRuleLogic;
    }

    /**
     * Checks if default access is denied when no rule matches.
     * @return true if default deny is enabled
     */
    public boolean isDefaultDeny() {
        return defaultDeny;
    }

    /**
     * Sets the default deny behavior.
     * @param defaultDeny boolean indicator
     */
    public void setDefaultDeny(boolean defaultDeny) {
        this.defaultDeny = defaultDeny;
    }

    /**
     * Gets the list of path prefixes to skip access control.
     * @return list of strings
     */
    public List<String> getSkipPathPrefixes() {
        return skipPathPrefixes;
    }

    /**
     * Sets the list of path prefixes to skip access control.
     * @param skipPathPrefixes list of strings
     */
    public void setSkipPathPrefixes(List<String> skipPathPrefixes) {
        this.skipPathPrefixes = skipPathPrefixes;
    }

    /**
     * Gets the mapped configuration.
     * @return map of config items
     */
    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }
    Config getConfig() {
        return config;
    }

    private void setConfigData() {
        if(getMappedConfig() != null) {
            Object object = getMappedConfig().get(ENABLED);
            if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
            object = getMappedConfig().get(DEFAULT_DENY);
            if(object != null) defaultDeny = Config.loadBooleanValue(DEFAULT_DENY, object);
            object = getMappedConfig().get(ACCESS_RULE_LOGIC);
            if(object != null) accessRuleLogic = (String)object;
        }
    }

    private void setConfigList() {
        if (mappedConfig != null && mappedConfig.get(SKIP_PATH_PREFIXES) != null) {
            Object object = mappedConfig.get(SKIP_PATH_PREFIXES);
            skipPathPrefixes = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = {}", s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        skipPathPrefixes = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the skipPathPrefixes json with a list of strings.");
                    }
                } else {
                    // comma separated
                    skipPathPrefixes = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                List prefixes = (List)object;
                prefixes.forEach(item -> {
                    skipPathPrefixes.add((String)item);
                });
            } else {
                throw new ConfigException("skipPathPrefixes must be a string or a list of strings.");
            }
        }
    }

}
