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
 * Configuration for the Response Cache Handler.
 */
@ConfigSchema(
        configKey = "response-cache",
        configName = "response-cache",
        configDescription = "Configuration for the Response Cache Handler.",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
public class ResponseCacheConfig {
    private static final Logger logger = LoggerFactory.getLogger(ResponseCacheConfig.class);

    public static final String CONFIG_NAME = "response-cache";
    private static final String ENABLED = "enabled";
    private static final String APPLIED_PATH_PREFIXES = "appliedPathPrefixes";

    private final Config config;
    private Map<String, Object> mappedConfig;

    // --- Annotated Fields ---
    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            description = "Indicate if the interceptor is enabled or not.",
            externalized = true,
            defaultValue = "true"
    )
    private boolean enabled;

    @ArrayField(
            configFieldName = APPLIED_PATH_PREFIXES,
            externalizedKeyName = APPLIED_PATH_PREFIXES,
            description = "A list of applied request path prefixes, other requests will skip this handler. The value can be a string\n" +
                    "if there is only one request path prefix needs this handler. or a list of strings if there are multiple.\n",
            externalized = true,
            items = String.class // The items in the list are strings
    )
    List<String> appliedPathPrefixes; // Keep as List<String>


    // --- Constructor and Loading Logic ---

    private ResponseCacheConfig() {
        this(CONFIG_NAME);
    }

    private ResponseCacheConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    public static ResponseCacheConfig load() {
        return new ResponseCacheConfig();
    }

    public static ResponseCacheConfig load(String configName) {
        return new ResponseCacheConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
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

        // Load appliedPathPrefixes List
        if (mappedConfig.get(APPLIED_PATH_PREFIXES) != null) {
            Object objectList = mappedConfig.get(APPLIED_PATH_PREFIXES);

            // The framework's standard internal loading for @ArrayField will load the raw list into the field.
            // However, the original code had complex logic to handle different string formats (JSON array string, comma separated string).
            // We must retain this logic if the external format is not strictly a YAML list.
            if(objectList instanceof String) {
                String s = (String)objectList;
                s = s.trim();
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
                // If loaded as a List (standard YAML or JSON format), assign it directly
                // Note: The framework should have already loaded this into the 'appliedPathPrefixes' field
                // via the ArrayField annotation if the config file was properly formatted YAML/JSON List.
                // We only need to check the raw mappedConfig if the automatic loading failed or was bypassed.
                this.appliedPathPrefixes = (List<String>) objectList; // Direct cast/assignment
            } else {
                throw new ConfigException("appliedPathPrefixes must be a string or a list of strings.");
            }
        } else {
            // Default value is null, which is fine, but we initialize to an empty list for safety.
            if (appliedPathPrefixes == null) {
                appliedPathPrefixes = new ArrayList<>();
            }
        }
    }

}
