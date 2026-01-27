package com.networknt.restrans;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.ConfigSchema; // REQUIRED IMPORT
import com.networknt.config.schema.OutputFormat; // REQUIRED IMPORT
import com.networknt.config.schema.BooleanField; // REQUIRED IMPORT
import com.networknt.config.schema.ArrayField; // REQUIRED IMPORT
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Configuration for the Response Filter Handler.
 */
@ConfigSchema(
        configKey = "response-filter",
        configName = "response-filter",
        configDescription = "Configuration for the Response Filter Handler.",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
public class ResponseFilterConfig {
    private static final Logger logger = LoggerFactory.getLogger(ResponseFilterConfig.class);

    public static final String CONFIG_NAME = "response-filter";
    private static final String ENABLED = "enabled";
    private static final String APPLIED_PATH_PREFIXES = "appliedPathPrefixes";

    private final Config config;
    private Map<String, Object> mappedConfig;

    // --- Annotated Fields ---
    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            description = "Indicate if the interceptor is enabled or not.",
            defaultValue = "true"
    )
    private boolean enabled;

    @ArrayField(
            configFieldName = APPLIED_PATH_PREFIXES,
            externalizedKeyName = APPLIED_PATH_PREFIXES,
            description = "A list of applied request path prefixes, other requests will skip this handler. The value can be a string\n" +
                    "if there is only one request path prefix needs this handler. or a list of strings if there are multiple.\n",
            items = String.class
    )
    List<String> appliedPathPrefixes;


    // --- Constructor and Loading Logic ---

    private ResponseFilterConfig() {
        this(CONFIG_NAME);
    }

    private ResponseFilterConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList(); // This method contains the custom logic
    }

    public static ResponseFilterConfig load() {
        return new ResponseFilterConfig();
    }

    public static ResponseFilterConfig load(String configName) {
        return new ResponseFilterConfig(configName);
    }

    // --- Getters and Setters (Original Methods) ---

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getAppliedPathPrefixes() {
        return appliedPathPrefixes;
    }

    public void setAppliedPathPrefixes(List<String> appliedPathPrefixes) {
        this.appliedPathPrefixes = appliedPathPrefixes;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
        // Load ENABLED field (handled by annotation processing using Config.loadBooleanValue)
        Object object = mappedConfig.get(ENABLED);
        enabled = Config.loadBooleanValue(ENABLED, object);
    }

    private void setConfigList() {
        if (mappedConfig.get(APPLIED_PATH_PREFIXES) != null) {
            Object objectList = mappedConfig.get(APPLIED_PATH_PREFIXES);
            appliedPathPrefixes = new ArrayList<>();

            // Retain the complex manual loading logic for handling various String formats
            if(objectList instanceof String) {
                String s = (String)objectList;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = {}", s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        appliedPathPrefixes = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the appliedPathPrefixes json with a list of strings.");
                    }
                } else {
                    // comma separated
                    appliedPathPrefixes = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (objectList instanceof List) {
                // If loaded as a List (standard YAML or JSON format), assign/copy elements
                List prefixes = (List)objectList;
                prefixes.forEach(item -> {
                    appliedPathPrefixes.add((String)item);
                });
            } else {
                throw new ConfigException("appliedPathPrefixes must be a string or a list of strings.");
            }
        } else {
            // Initialize to an empty list if not configured
            if (appliedPathPrefixes == null) {
                appliedPathPrefixes = new ArrayList<>();
            }
        }
    }
}
