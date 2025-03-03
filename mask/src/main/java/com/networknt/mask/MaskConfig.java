package com.networknt.mask;

import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;

import java.util.Map;

@ConfigSchema(configName = "mask", configKey = "mask", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML})
public class MaskConfig {

    private Map<String, Map<String, String>> string;
    private MaskRegexConfig regex;


}
