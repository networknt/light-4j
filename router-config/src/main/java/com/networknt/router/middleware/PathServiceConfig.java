package com.networknt.router.middleware;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.MapField;
import com.networknt.config.schema.OutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * create the config class to enable loading from the config-server. With this class, it allows
 * the mapping from the path to the serviceId to be defined as a string. And also a map in YAML.
 *
 * @author Steve Hu
 */
@ConfigSchema(
        configKey = "pathService",
        configName = "pathService",
        configDescription = "Path service configuration",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
public class PathServiceConfig {
    private static final Logger logger = LoggerFactory.getLogger(PathServiceConfig.class);
    public static final String CONFIG_NAME = "pathService";

    // keys in the config file
    private static final String ENABLED = "enabled";
    private static final String MAPPING = "mapping";

    // variables
    private Map<String, Object> mappedConfig;

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            defaultValue = "true",
            description = "indicate if PathServiceHandler is enabled or not"
    )
    private boolean enabled;

    @MapField(
        configFieldName = MAPPING,
        externalizedKeyName = MAPPING,
        description = "mapping from request endpoints to serviceIds.\n" +
                "The following are examples in the values.yml\n" +
                "  /v1/address/{id}@get: party.address-1.0.0\n" +
                "  /v2/address@get: party.address-2.0.0\n" +
                "  /v1/contact@post: party.contact-1.0.0",
        valueType = String.class
    )
    private Map<String, String> mapping;


    // the config object
    private Config config;

    private PathServiceConfig() {
        this(CONFIG_NAME);
    }

    private PathServiceConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setMap();
        setConfigData();
    }

    public static PathServiceConfig load() {
        return new PathServiceConfig();
    }

    public static PathServiceConfig load(String configName) {
        return new PathServiceConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setMap();
        setConfigData();
    }

    public Map<String, String> getMapping() {
        return mapping;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    Config getConfig() {
        return config;
    }

    private void setMap() {
        if(mappedConfig.get(MAPPING) != null) {
            if(mappedConfig.get(MAPPING) instanceof String) {
                String s = (String)mappedConfig.get(MAPPING);
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("{")) {
                    // json map
                    try {
                        mapping = Config.getInstance().getMapper().readValue(s, Map.class);
                    } catch (IOException e) {
                        logger.error("IOException:", e);
                    }
                } else {
                    Map<String, String> map = new LinkedHashMap<>();
                    for(String keyValue : s.split(" *& *")) {
                        String[] pairs = keyValue.split(" *= *", 2);
                        map.put(pairs[0], pairs.length == 1 ? "" : pairs[1]);
                    }
                    mapping = map;
                }
            } else if (mappedConfig.get(MAPPING) instanceof Map) {
                mapping = (Map<String, String>) mappedConfig.get(MAPPING);
            } else {
                logger.error("mapping is the wrong type. Only JSON string and YAML map are supported.");
            }
        }
    }

    private void setConfigData() {
        Object object = getMappedConfig().get(ENABLED);
        if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
    }
}
