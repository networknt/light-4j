package com.networknt.router.middleware;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@ConfigSchema(
        configKey = "token",
        configName = "token",
        configDescription = "This is the configuration file for the TokenHandler that is responsible for getting\n" +
                "a client credentials token in http-sidecar and light-gateway when calling others.\n" +
                "The configuration for one or multiple OAuth 2.0 providers is in the client.yml file.\n",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML}
)
public class TokenConfig {
    private static final Logger logger = LoggerFactory.getLogger(TokenConfig.class);
    public static final String CONFIG_NAME = "token";
    private static final String ENABLED = "enabled";
    private static final String APPLIED_PATH_PREFIXES = "appliedPathPrefixes";

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            externalized = true,
            defaultValue = "false",
            description = "Indicate if the handler is enabled."
    )
    boolean enabled;

    @ArrayField(
            configFieldName = APPLIED_PATH_PREFIXES,
            externalizedKeyName = APPLIED_PATH_PREFIXES,
            externalized = true,
            description = "applied path prefixes for the token handler. Only the path prefixes listed here will\n" +
                    "get the token based on the configuration in the client.yml section. This will allow\n" +
                    "the share gateway to define only one default chain with some endpoints get the token\n" +
                    "and others bypass this handler.",
            items = String.class
    )
    List<String> appliedPathPrefixes;

    private final Config config;
    private Map<String, Object> mappedConfig;

    private TokenConfig() {
        this(CONFIG_NAME);
    }

    private TokenConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigList();
        setConfigData();
    }

    public static TokenConfig load() {
        return new TokenConfig();
    }

    public static TokenConfig load(String configName) {
        return new TokenConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigList();
        setConfigData();
    }
    public void setConfigData() {
        Object object = getMappedConfig().get(ENABLED);
        if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getAppliedPathPrefixes() {
        return appliedPathPrefixes;
    }

    public void setAppliedPathPrefixes(List<String> appliedPathPrefixes) {
        this.appliedPathPrefixes = appliedPathPrefixes;
    }

    private void setConfigList() {
        if (mappedConfig.get(APPLIED_PATH_PREFIXES) != null) {
            Object object = mappedConfig.get(APPLIED_PATH_PREFIXES);
            appliedPathPrefixes = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
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
            } else if (object instanceof List) {
                List prefixes = (List)object;
                prefixes.forEach(item -> {
                    appliedPathPrefixes.add((String)item);
                });
            } else {
                throw new ConfigException("appliedPathPrefixes must be a string or a list of strings.");
            }
        }
    }

}
