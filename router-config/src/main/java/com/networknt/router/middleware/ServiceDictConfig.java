package com.networknt.router.middleware;

import com.networknt.config.Config;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.MapField;
import com.networknt.config.schema.OutputFormat;
import com.networknt.handler.config.HandlerUtils;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * create the config class to enable loading from the config-server. With this class, it allows
 * the mapping from the path to the serviceId to be defined as a string. And also a map in YAML.
 *
 * @author Steve Hu
 */
@ConfigSchema(configKey = "serviceDict", configName = "serviceDict", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML})
public class ServiceDictConfig {
    private static final Logger logger = LoggerFactory.getLogger(ServiceDictConfig.class);
    public static final String CONFIG_NAME = "serviceDict";

    // keys in the config file
    private static final String ENABLED = "enabled";
    private static final String MAPPING = "mapping";

    // variables
    private Map<String, Object> mappedConfig;

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            externalized = true,
            defaultValue = true,
            description = "indicate if ServiceDictHandler is enabled or not"
    )
    private boolean enabled;

    @MapField(
            configFieldName = MAPPING,
            externalizedKeyName = MAPPING,
            externalized = true,
            description = "mapping from 'pathPrefix@requestMethod' to serviceIds",
            valueType = String.class
    )
    private Map<String, String> mapping;

    // the config object
    private Config config;

    private ServiceDictConfig() {
        this(CONFIG_NAME);
    }

    private ServiceDictConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setMap();
        setConfigData();
    }

    public static ServiceDictConfig load() {
        return new ServiceDictConfig();
    }

    public static ServiceDictConfig load(String configName) {
        return new ServiceDictConfig(configName);
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
        Map<String, String> rawMapping = null;
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
                    rawMapping = map;
                }
            } else if (mappedConfig.get(MAPPING) instanceof Map) {
                rawMapping = (Map<String, String>) mappedConfig.get(MAPPING);
            } else {
                logger.error("mapping is the wrong type. Only JSON string and YAML map are supported.");
            }
            // convert the mapping to internal format.
            mapping = new HashMap<>();
            for (Map.Entry<String, String> entry : rawMapping.entrySet()) {
                mapping.put(HandlerUtils.toInternalKey(entry.getKey()), entry.getValue());
            }
            mapping = Collections.unmodifiableMap(mapping);
        }
    }

    private void setConfigData() {
        Object object = getMappedConfig().get(ENABLED);
        if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
    }
}
