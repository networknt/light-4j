package com.networknt.config.yml;

import com.networknt.config.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 * This class is used to construct the config loader according to given config loader class name.
 *
 * @author Jiachen Sun
 */
public class ConfigLoaderConstructor extends Constructor {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoaderConstructor.class);
    public static final String CONFIG_LOADER_CLASS = "configLoaderClass";
    private final ConfigLoader configLoader;

    public ConfigLoaderConstructor(String configLoaderClass) {
        super();
        configLoader = createConfigLoader(configLoaderClass);
    }

    public ConfigLoader getConfigLoader() {
        return configLoader;
    }

    private ConfigLoader createConfigLoader(String configLoaderClass) {
        if (configLoaderClass == null || configLoaderClass.equals("")) {
            return null;
        }
        logger.debug("creating config loader {}.", configLoaderClass);
        try {
            Class<?> typeClass = Class.forName(configLoaderClass);
            if (!typeClass.isInterface()) {
                return (ConfigLoader) typeClass.getConstructor().newInstance();
            } else {
                logger.error("Please specify an implementing class of com.networknt.config.ConfigLoader.");
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new RuntimeException("Unable to construct the class loader.", e);
        }
        return null;
    }
}
