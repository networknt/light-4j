package com.networknt.rule;

import com.networknt.config.Config;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import com.networknt.config.schema.MapField;
import com.networknt.config.schema.ArrayField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.server.ModuleRegistry;

import java.util.List;
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

    private static volatile RuleConfig instance;
    private final Map<String, Object> mappedConfig;
    private final Config config;
    @MapField(
            configFieldName = "ruleBodies",
            externalizedKeyName = "ruleBodies",
            description = "Map of rule definitions, keyed by ruleId",
            valueType = Object.class
    )
    private Map<String, Object> ruleBodies;
    
    @ArrayField(
            configFieldName = "rules",
            externalizedKeyName = "rules",
            description = "List of rule assignments mapping endpoints to rules",
            items = Map.class
    )
    private List<Map<String, Object>> rules;

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

    public List<Map<String, Object>> getRules() {
        return rules;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
    }

    private void setMapData() {
        Object object = getMappedConfig().get("ruleBodies");
        if(object != null) {
            ruleBodies = (Map<String, Object>)object;
        }
        object = getMappedConfig().get("rules");
        if(object != null) {
            rules = (List<Map<String, Object>>)object;
        }
    }
}
