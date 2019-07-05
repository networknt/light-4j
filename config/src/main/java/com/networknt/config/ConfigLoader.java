package com.networknt.config;

import java.util.Map;

public interface ConfigLoader {

    Map<String, Object> loadMapConfig(String configName, String path);

    Map<String, Object> loadMapConfig(String configName);

    <T> Object loadObjectConfig(String configName, Class<T> clazz, String path);

    <T> Object loadObjectConfig(String configName, Class<T> clazz);
}
