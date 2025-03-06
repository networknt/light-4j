package com.networknt.mask;

import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.MapField;
import com.networknt.config.schema.OutputFormat;

import java.util.Map;

@ConfigSchema(configName = "mask", configKey = "mask", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML})
public class MaskConfig {

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
            externalized = true,
            valueType = Map.class
    )
    private Map<String, Map<String, String>> regex;

    @MapField(
            configFieldName = JSON,
            externalizedKeyName = JSON,
            externalized = true,
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
}
