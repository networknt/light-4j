package com.networknt.expect100continue;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ConfigSchema(
        configKey = "expect-100-continue",
        configName = "expect-100-continue",
        configDescription = "Configuration for the 'Expect100Continue' handler.",
        outputFormats = {
                OutputFormat.JSON_SCHEMA,
                OutputFormat.YAML
        })
public class Expect100ContinueConfig {
    public static final String CONFIG_NAME = "expect-100-continue";
    private static final String ENABLED = "enabled";
    private static final String IGNORED_PATH_PREFIXES = "ignoredPathPrefixes";
    private static final String IN_PLACE_PATH_PREFIXES = "inPlacePathPrefixes";

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            externalized = true,
            description = "Indicate if the Expect100Continue middleware is enabled or not."
    )
    private boolean enabled;

    @ArrayField(
            configFieldName = IN_PLACE_PATH_PREFIXES,
            externalizedKeyName = IN_PLACE_PATH_PREFIXES,
            externalized = true,
            description = "List of paths that will not follow the expect-100-continue protocol. The Expect header will be removed altogether.\n" +
                    "format is in array format, or in string array format (i.e. '[path1, path2]')",
            items = String.class
    )
    private List<String> ignoredPathPrefixes;

    @ArrayField(
            configFieldName = IGNORED_PATH_PREFIXES,
            externalizedKeyName = IGNORED_PATH_PREFIXES,
            externalized = true,
            description = "List of paths that will respond 100-continue in place before continuing execution of the remaining handlers.\n" +
                    "The Expect header will be removed after the response is sent.\n" +
                    "format is in array format, or in string array format (i.e. '[path1, path2]')",
            items = String.class
    )
    private List<String> inPlacePathPrefixes;

    private final Config config;
    private Map<String, Object> mappedConfig;

    public static Expect100ContinueConfig load() {
        return new Expect100ContinueConfig();
    }

    private Expect100ContinueConfig() {
        this(CONFIG_NAME);
    }

    private Expect100ContinueConfig(String configName) {
        this.config = Config.getInstance();
        this.mappedConfig = config.getJsonMapConfigNoCache(configName);
        this.setConfigData();
    }

    void reload() {
        this.mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        this.setConfigData();
    }

    private void setConfigData() {

        if (this.mappedConfig.containsKey(ENABLED))
            this.enabled = (Boolean) this.mappedConfig.get(ENABLED);

        if (this.mappedConfig.containsKey(IN_PLACE_PATH_PREFIXES))
            this.inPlacePathPrefixes = handleMultiTypeListConfigProperty(this.mappedConfig, IN_PLACE_PATH_PREFIXES);

        if (this.mappedConfig.containsKey(IGNORED_PATH_PREFIXES))
            this.ignoredPathPrefixes = handleMultiTypeListConfigProperty(this.mappedConfig, IGNORED_PATH_PREFIXES);

    }

    private static List<String> handleMultiTypeListConfigProperty(final Map<String, Object> mappedConfig, final String propertyName) {
        if (mappedConfig.get(propertyName) == null)
            return new ArrayList<>();

        else if (mappedConfig.get(propertyName) instanceof List)
            return (List) mappedConfig.get(propertyName);

        else if (mappedConfig.get(propertyName) instanceof String) {

            final var ignoredPathsString = ((String) mappedConfig.get(propertyName)).trim();

            if (!ignoredPathsString.isEmpty()
                    && !ignoredPathsString.isBlank()
                    && ignoredPathsString.contains("["))
                return List.of(ignoredPathsString
                        .trim()
                        .replace("[", "")
                        .replace("]", "")
                        .replace(" ", "")
                        .split(",")
                );
        }

        throw new ConfigException("'" + propertyName + "' must be a List<String>, a String, or empty.");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getIgnoredPathPrefixes() {
        return ignoredPathPrefixes;
    }

    public List<String> getInPlacePathPrefixes() {
        return inPlacePathPrefixes;
    }
}
