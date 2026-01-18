package com.networknt.rule;

import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.config.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@ConfigSchema(
        configName = "rule-loader",
        configKey = "rule-loader",
        configDescription = "Rule loader configuration",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
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
    private final Config config;

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            defaultValue = "true",
            description = "A flag to enable the rule loader to get rules for the service from portal"
    )
    boolean enabled;


    @StringField(
            configFieldName = PORTAL_HOST,
            externalizedKeyName = PORTAL_HOST,
            defaultValue = "https://localhost",
            description = "The portal host with port number if it is not default TLS port 443. Used when ruleSource is light-portal"
    )
    String portalHost;

    @StringField(
            configFieldName = PORTAL_TOKEN,
            externalizedKeyName = PORTAL_TOKEN,
            description = "An authorization token that allows the rule loader to connect to the light-portal. Only used if ruleSource\n" +
                    "is light-portal."
    )
    String portalToken;

    @StringField(
            configFieldName = RULE_SOURCE,
            externalizedKeyName = RULE_SOURCE,
            defaultValue = "light-portal",
            description = "Source of the rule. light-portal or config-folder and default to light-portal. If config folder is set,\n" +
                    "a rules.yml must be in the externalized folder to load rules from it. The config-folder option should\n" +
                    "only be used for local testing or the light-portal is not implemented in the organization and cloud\n" +
                    "light-portal is not allowed due to security policy or blocked."
    )
    String ruleSource;

    @MapField(
            configFieldName = ENDPOINT_RULES,
            externalizedKeyName = ENDPOINT_RULES,
            description = "When ruleSource is config-folder, then we can load the endpoint to rules mapping here instead of portal\n" +
                    "service details. Each endpoint will have a list of rules and the type of the rules.",
            valueType = List.class
    )
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
        if(object != null) {
            if(object instanceof String) {
                enabled = Boolean.parseBoolean((String)object);
            } else if (object instanceof Boolean) {
                enabled = (Boolean) object;
            } else {
                throw new RuntimeException("enabled must be a boolean value.");
            }
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
