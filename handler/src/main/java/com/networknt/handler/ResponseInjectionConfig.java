package com.networknt.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import com.networknt.server.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The config class for the ResponseInterceptorInjectionHandler middleware handler.
 */
@ConfigSchema(configName = "response-injection", configKey = "response-injection", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD})
public class ResponseInjectionConfig {
    private static final Logger logger = LoggerFactory.getLogger(ResponseInjectionConfig.class);

    /** Config name constant */
    public static final String CONFIG_NAME = "response-injection";
    private static final String ENABLED = "enabled";
    private static final String APPLIED_BODY_INJECTION_PATH_PREFIXES = "appliedBodyInjectionPathPrefixes";

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            defaultValue = "true",
            description = "indicator of enabled"
    )
    private boolean enabled;

    @ArrayField(
            configFieldName = APPLIED_BODY_INJECTION_PATH_PREFIXES,
            externalizedKeyName = APPLIED_BODY_INJECTION_PATH_PREFIXES,
            description = "response body injection applied path prefixes. Injecting the response body and output into the audit log is very heavy operation,\n" +
                    "and it should only be enabled when necessary or for diagnose session to resolve issues. This list can be updated on the config\n" +
                    "server or local values.yml, then an API call to the config-reload endpoint to apply the changes from light-portal control pane.\n" +
                    "Please be aware that big response body will only log the beginning part of it in the audit log and gzip encoded response body can\n" +
                    "not be injected. Even the body injection is not applied, you can still transform the response for headers, query parameters, path\n" +
                    "parameters etc. The format is a list of strings separated with commas or a JSON list in values.yml definition from config server,\n" +
                    "or you can use yaml format in this file or values.yaml on local filesystem. The following are the examples.\n" +
                    "response-injection.appliedBodyInjectionPathPrefixes: [\"/v1/cats\", \"/v1/dogs\"]\n" +
                    "response-injection.appliedBodyInjectionPathPrefixes: /v1/cats, /v1/dogs\n" +
                    "response-injection.appliedBodyInjectionPathPrefixes:\n" +
                    "  - /v1/cats\n" +
                    "  - /v1/dogs",
            items = String.class
    )
    private List<String> appliedBodyInjectionPathPrefixes;

    private Map<String, Object> mappedConfig;

    /**
     * Instance of ResponseInjectionConfig.
     */
    private static volatile ResponseInjectionConfig instance;

    /**
     * Default constructor for ResponseInjectionConfig.
     */
    public ResponseInjectionConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Constructor for ResponseInjectionConfig with a custom config name.
     * @param configName name of the configuration file
     */
    private ResponseInjectionConfig(String configName) {
        mappedConfig = Config.getInstance().getJsonMapConfig(configName);
        setConfigData();
        setConfigList();
    }

    /**
     * Loads the default response injection configuration.
     *
     * @return An instance of ResponseInjectionConfig.
     */
    public static ResponseInjectionConfig load() {
        return load(CONFIG_NAME);
    }

    /**
     * Loads the response injection configuration by name.
     *
     * @param configName The name of the configuration.
     * @return An instance of ResponseInjectionConfig.
     */
    public static ResponseInjectionConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (ResponseInjectionConfig.class) {
                mappedConfig = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new ResponseInjectionConfig(configName);
                // Register the module with the configuration.
                ModuleRegistry.registerModule(configName, ResponseInjectionConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), null);
                return instance;
            }
        }
        return new ResponseInjectionConfig(configName);
    }

    /**
     * Returns true if the response injection is enabled.
     *
     * @return true if enabled, false otherwise.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the list of applied body injection path prefixes.
     *
     * @return A list of strings representing the path prefixes.
     */
    public List<String> getAppliedBodyInjectionPathPrefixes() {
        return appliedBodyInjectionPathPrefixes;
    }

    /**
     * Returns the mapped config.
     *
     * @return A map of configuration values.
     */
    Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
        Object object = getMappedConfig().get(ENABLED);
        if (object != null) enabled = Config.loadBooleanValue(ENABLED, object);
    }

    private void setConfigList() {
        if (mappedConfig != null && mappedConfig.get(APPLIED_BODY_INJECTION_PATH_PREFIXES) != null) {
            var object = mappedConfig.get(APPLIED_BODY_INJECTION_PATH_PREFIXES);
            appliedBodyInjectionPathPrefixes = new ArrayList<>();

            if (object instanceof String) {
                var s = (String) object;
                s = s.trim();

                if (logger.isTraceEnabled())
                    logger.trace("s = " + s);

                if (s.startsWith("["))
                    try {
                        appliedBodyInjectionPathPrefixes = Config.getInstance().getMapper().readValue(s, new TypeReference<>() {
                        });
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the appliedBodyInjectionPathPrefixes json with a list of strings.");
                    }
                else
                    // comma separated
                    appliedBodyInjectionPathPrefixes = Arrays.asList(s.split("\\s*,\\s*"));

            } else if (object instanceof List) {

                var prefixes = (List) object;
                prefixes.forEach(item -> {
                    appliedBodyInjectionPathPrefixes.add((String) item);
                });

            } else throw new ConfigException("appliedBodyInjectionPathPrefixes must be a string or a list of strings.");
        }
    }

}
