package com.networknt.mask;

import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.MapField;
import com.networknt.config.schema.OutputFormat;

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

    public static MaskConfig load() {
        return (MaskConfig) com.networknt.config.Config.getInstance().getJsonObjectConfig(CONFIG_NAME, MaskConfig.class);
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
