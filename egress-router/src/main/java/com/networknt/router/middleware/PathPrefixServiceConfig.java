package com.networknt.router.middleware;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * create the config class to enable loading from the config-server. With this class, it allows
 * the mapping from the prefix to the serviceId to be defined as a string. And also a map in YAML.
 *
 * @author Steve Hu
 */
public class PathPrefixServiceConfig {
    private static final Logger logger = LoggerFactory.getLogger(PathPrefixServiceConfig.class);
    public static final String CONFIG_NAME = "pathPrefixService";

    // keys in the config file
    private static final String ENABLED = "enabled";
    private static final String MAPPING = "mapping";

    // variables
    private  Map<String, Object> mappedConfig;
    private Map<String, String> mapping;
    private boolean enabled;

    // the config object
    private Config config;

    private PathPrefixServiceConfig() {
        this(CONFIG_NAME);
    }

    private PathPrefixServiceConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setMap();
        setConfigData();
    }

    public static PathPrefixServiceConfig load() {
        return new PathPrefixServiceConfig();
    }

    public static PathPrefixServiceConfig load(String configName) {
        return new PathPrefixServiceConfig(configName);
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
        if(getMappedConfig().get(MAPPING) instanceof String) {
            String s = (String)getMappedConfig().get(MAPPING);
            if(logger.isTraceEnabled()) logger.trace("s = " + s);
            Map<String, String> map = new LinkedHashMap<>();
            for(String keyValue : s.split(" *& *")) {
                String[] pairs = keyValue.split(" *= *", 2);
                map.put(pairs[0], pairs.length == 1 ? "" : pairs[1]);
            }
            mapping = map;
        } else if (getMappedConfig().get(MAPPING) instanceof Map) {
            mapping = (Map<String, String>) getMappedConfig().get(MAPPING);
        } else {
            throw new ConfigException("mapping is missing or wrong type.");
        }
    }

    private void setConfigData() {
        Object object = getMappedConfig().get(ENABLED);
        if(object != null && (Boolean) object) {
            enabled = true;
        }
    }
}
