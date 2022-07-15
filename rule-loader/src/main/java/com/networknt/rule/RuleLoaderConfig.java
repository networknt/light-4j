package com.networknt.rule;

import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class RuleLoaderConfig {
    private static final Logger logger = LoggerFactory.getLogger(RuleLoaderConfig.class);

    public static final String CONFIG_NAME = "rule-loader";
    public static final String RULE_SOURCE_LIGHT_PORTAL = "light-portal";
    public static final String RULE_SOURCE_CONFIG_FOLDER = "config-folder";
    private static final String ENABLED = "enabled";
    private static final String RULE_SOURCE = "ruleSource";
    private static final String PORTAL_HOST = "portalHost";
    private static final String PORTAL_TOKEN = "portalToken";
    private static final String ENDPOINT_RULES = "endpointRules";

    private Map<String, Object> mappedConfig;
    private Config config;

    boolean enabled;
    String ruleSource;
    String portalHost;
    String portalToken;
    Map<String, Object> endpointRules;

    private RuleLoaderConfig() {
        this(CONFIG_NAME);
    }

    private RuleLoaderConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setMapData();
    }

    public static RuleLoaderConfig load() {
        return new RuleLoaderConfig();
    }

    public static RuleLoaderConfig load(String configName) {
        return new RuleLoaderConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setMapData();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getRuleSource() {
        return ruleSource;
    }

    public String getPortalHost() {
        return portalHost;
    }

    public String getPortalToken() {
        return portalToken;
    }

    public Map<String, Object> getEndpointRules() { return endpointRules; }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if(object != null && (Boolean) object) {
            enabled = true;
        }
        ruleSource = (String)mappedConfig.get(RULE_SOURCE);
        portalHost = (String)mappedConfig.get(PORTAL_HOST);
        portalToken = (String)mappedConfig.get(PORTAL_TOKEN);
    }

    private void setMapData() {
        // set the endpointRules map. It can be a string or a map in the values.yml
        if(mappedConfig.get(ENDPOINT_RULES) instanceof String) {
            // the json string is supported here.
            String s = (String)mappedConfig.get(ENDPOINT_RULES);
            if(logger.isTraceEnabled()) logger.trace("endpointRules = " + s);
            endpointRules = JsonMapper.string2Map(s);
        } else if (mappedConfig.get(ENDPOINT_RULES) instanceof Map) {
            endpointRules = (Map<String, Object>)mappedConfig.get(ENDPOINT_RULES);
        } else {
            if(logger.isInfoEnabled()) logger.info("endpointRules missing or wrong type.");
            // ignore this situation as a particular application might not have any injections.
        }
    }
}
