package com.networknt.config;

import java.util.Map;

/**
 * By implementing this interface and configuring the corresponding implementation class name in config.yml.
 * Users can customize the configured loading strategy and mode.
 * <p>
 * An example of an implementation class can be found at: com.networknt.config.TestConfigLoader
 * This implementation class provides a single configuration file loading strategy similar to spring.
 */
public interface ConfigLoader {

    Map<String, Object> loadMapConfig(String configName, String path);

    Map<String, Object> loadMapConfig(String configName);

    <T> Object loadObjectConfig(String configName, Class<T> clazz, String path);

    <T> Object loadObjectConfig(String configName, Class<T> clazz);
}
