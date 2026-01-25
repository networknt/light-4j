package com.networknt.mask;

import com.networknt.config.Config;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.MapField;
import com.networknt.config.schema.OutputFormat;
import com.networknt.server.ModuleRegistry;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@ConfigSchema(
        configName = "mask",
        configKey = "mask",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD},
        configDescription = "This is the default mask config file that is used to mask password in\n" +
                "uri before logging it. If you want to overwrite this config file, please\n" +
                "make sure that this entry is not removed. The metrics module is using it."

)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MaskConfig {
    private static volatile MaskConfig instance;
    private final Config config;
    private Map<String, Object> mappedConfig;

    private MaskConfig() {
        this(CONFIG_NAME);
    }

    private MaskConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        if(mappedConfig != null && mappedConfig.get(STRING) != null) string = (Map<String, Map<String, String>>)mappedConfig.get(STRING);
        if(mappedConfig != null && mappedConfig.get(REGEX) != null) regex = (Map<String, Map<String, String>>)mappedConfig.get(REGEX);
        if(mappedConfig != null && mappedConfig.get(JSON) != null) json = (Map<String, Map<String, String>>)mappedConfig.get(JSON);
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public static MaskConfig load() {
        return load(CONFIG_NAME);
    }

    public static MaskConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (MaskConfig.class) {
                mappedConfig = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new MaskConfig(configName);
                ModuleRegistry.registerModule(configName, MaskConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), null);
                return instance;
            }
        }
        return new MaskConfig(configName);
    }

    public static final String CONFIG_NAME = "mask";

    public static final String STRING = "string";
    public static final String REGEX = "regex";
    public static final String JSON = "json";

    @MapField(
            configFieldName = STRING,
            externalizedKeyName = STRING,
            defaultValue = "{\"uri\":{\"password=[^&]*\": \"password=******\"}}",
            valueType = Map.class
    )
    private Map<String, Map<String, String>> string;

    @MapField(
            configFieldName = REGEX,
            externalizedKeyName = REGEX,
            valueType = Map.class
    )
    private Map<String, Map<String, String>> regex;

    @MapField(
            configFieldName = JSON,
            externalizedKeyName = JSON,
            valueType = Map.class
    )
    private Map<String, Map<String, String>> json;

    public Map<String, Map<String, String>> getString() {
        return string;
    }

    public Map<String, Map<String, String>> getRegex() {
        return regex;
    }

    public Map<String, Map<String, String>> getJson() {
        return json;
    }

    public void setString(Map<String, Map<String, String>> string) {
        this.string = string;
    }

    public void setRegex(Map<String, Map<String, String>> regex) {
        this.regex = regex;
    }

    public void setJson(Map<String, Map<String, String>> json) {
        this.json = json;
    }
}
