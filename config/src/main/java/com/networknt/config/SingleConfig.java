package com.networknt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This class is used to check whether load config from single configuration file
 * `application.yaml, or yml or json` is valid and then load object config or map
 * config from `application.yaml or yml or json`.
 *
 * Created by jiachen on 2019-01-23.
 */
public abstract class SingleConfig {

    static final Logger logger = LoggerFactory.getLogger(SingleConfig.class);

    private static final String SINGLE_CONFIG = "application";

    // Load configuration from application.yaml or yml or json and cache it.
    private static final Map<String, Object> singleConfigMap = Config.getInstance().getJsonMapConfig(SINGLE_CONFIG);

    // Flag used to enable or disable this feature. Generally this feature is enabled, only disabled when doing unit test.
    private static boolean singleConfigEnabled = true;

    /**
     * This method is used to validate whether loading config from `application.yaml
     * or yml or json' is valid.
     */
    public static boolean isPresent() {
        return singleConfigEnabled && singleConfigMap != null;
    }

    /**
     * This method is used to load map config from `application.yaml or yml or json`
     * @param configName the name of the module to load
     * @return the map contains the config information
     */
    public static Map<String, Object> getMapConfigFromSingleConfig(String configName) {
        Map<String, Object> mapConfig = (Map<String, Object>)singleConfigMap.get(configName);
        if (mapConfig == null) {
            logger.info("Cannot load config from application extended with json, yaml or yml for " + configName + " module, " +
                    "try to load it from " + configName + " extended with json, yaml or yml");
            return null;
        }
        return mapConfig;
    }

    /**
     * This method is used to load object config from `application.yaml or yml or json`
     * @param configName the name of the module to load
     * @return the object contains the config information
     */
    public static Object getObjectConfigFromSingleConfig(String configName, Class clazz) {
        Map<String, Object> mapConfig = getMapConfigFromSingleConfig(configName);
        return Config.convertMapToObj(mapConfig, clazz);
    }

    /**
     * This method is used to disable or enable single config mode
     */
    public static void setSingleConfigEnabled(boolean enable) {
        singleConfigEnabled = enable;
    }
}
