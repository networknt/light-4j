package com.networknt.rule;

import com.networknt.config.Config;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import com.networknt.config.schema.MapField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.server.ModuleRegistry;

import java.util.Map;

@ConfigSchema(
        configName = "rule",
        configKey = "rule",
        configDescription = "Rule configuration",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
public class RuleConfig {
    private static final Logger logger = LoggerFactory.getLogger(RuleConfig.class);

    public static final String CONFIG_NAME = "rule";
    public static final String RULE_BODIES = "ruleBodies";
    public static final String ENDPOINT_RULES = "endpointRules";

    private static volatile RuleConfig instance;
    private final Map<String, Object> mappedConfig;
    private final Config config;

    @MapField(
            configFieldName = RULE_BODIES,
            externalizedKeyName = RULE_BODIES,
            description = "Map of rule definitions, keyed by ruleId",
            valueType = Object.class
    )
    private Map<String, Object> ruleBodies;

    @MapField(
            configFieldName = ENDPOINT_RULES,
            externalizedKeyName = ENDPOINT_RULES,
            description = "Map of rule assignments mapping endpoints to rules",
            valueType = Object.class
    )
    private Map<String, Object> endpointRules;

    private RuleConfig() {
        this(CONFIG_NAME);
    }

    private RuleConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        if (mappedConfig != null) {
            setConfigData();
            setMapData();
        }
    }

    public static RuleConfig load() {
        return load(CONFIG_NAME);
    }

    public static RuleConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            if (instance != null && instance.getMappedConfig() == Config.getInstance().getJsonMapConfig(configName)) {
                return instance;
            }
            synchronized (RuleConfig.class) {
                if (instance != null && instance.getMappedConfig() == Config.getInstance().getJsonMapConfig(configName)) {
                    return instance;
                }
                instance = new RuleConfig(configName);
                ModuleRegistry.registerModule(CONFIG_NAME, RuleConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
                return instance;
            }
        }
        return new RuleConfig(configName);
    }

    public Map<String, Object> getRuleBodies() {
        return ruleBodies;
    }

    public Map<String, Object> getEndpointRules() {
        return endpointRules;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
    }

    private void setMapData() {
        Object object = getMappedConfig().get(RULE_BODIES);
        if(object != null) {
            ruleBodies = (Map<String, Object>)object;
        }
        object = getMappedConfig().get(ENDPOINT_RULES);
        if(object != null) {
            endpointRules = (Map<String, Object>)object;
        }
    }
}
