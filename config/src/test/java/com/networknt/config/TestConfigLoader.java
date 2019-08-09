package com.networknt.config;

import java.util.Map;

public class TestConfigLoader implements ConfigLoader {
    private final static String APPLICATION_CONFIG = "application";

    private Map<String, Object> applicationConfigMap;

    @Override
    public Map<String, Object> loadMapConfig(String configName, String path) {
        if (applicationConfigMap == null) {
            applicationConfigMap = Config.getInstance().getDefaultJsonMapConfig(APPLICATION_CONFIG, path);
        }
        Map<String, Object> mapConfig = (Map<String, Object>) applicationConfigMap.get(configName);
        Map<String, Object> defaultConfig = Config.getInstance().getDefaultJsonMapConfigNoCache(configName, path);
        if (defaultConfig != null && mapConfig != null) {
            defaultConfig.putAll(mapConfig);
        }
        return defaultConfig != null ? defaultConfig : mapConfig;
    }

    @Override
    public Map<String, Object> loadMapConfig(String configName) {
        return loadMapConfig(configName, "");
    }

    @Override
    public <T> Object loadObjectConfig(String configName, Class<T> clazz, String path) {
        Map<String, Object> mapConfig = loadMapConfig(configName, path);
        return Config.getInstance().getMapper().convertValue(mapConfig, clazz);
    }

    @Override
    public <T> Object loadObjectConfig(String configName, Class<T> clazz) {
        return loadObjectConfig(configName, clazz, "");
    }
}
