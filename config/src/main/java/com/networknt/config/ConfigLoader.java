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

    /**
     * Loads a map configuration from a given name and path.
     *
     * @param configName The name of the config file
     * @param path       The path to the config file
     * @return Map of configuration values
     */
    Map<String, Object> loadMapConfig(String configName, String path);

    /**
     * Loads a map configuration from a given name.
     *
     * @param configName The name of the config file
     * @return Map of configuration values
     */
    Map<String, Object> loadMapConfig(String configName);

    /**
     * Loads an object configuration from a given name, class, and path.
     *
     * @param configName The name of the config file
     * @param clazz      The class to load into
     * @param path       The path to the config file
     * @param <T>        The type of the object
     * @return The loaded object
     */
    <T> Object loadObjectConfig(String configName, Class<T> clazz, String path);

    /**
     * Loads an object configuration from a given name and class.
     *
     * @param configName The name of the config file
     * @param clazz      The class to load into
     * @param <T>        The type of the object
     * @return The loaded object
     */
    <T> Object loadObjectConfig(String configName, Class<T> clazz);
}
