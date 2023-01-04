package com.networknt.registry.support;

import com.networknt.config.Config;
import com.networknt.registry.URL;
import com.networknt.registry.URLImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class DirectRegistryConfig {
    private static final Logger logger = LoggerFactory.getLogger(DirectRegistryConfig.class);

    public static final String CONFIG_NAME = "direct-registry";
    private static final String DIRECT_URLS = "directUrls";
    Map<String, List<URL>> directUrls;
    private Config config;
    private Map<String, Object> mappedConfig;

    public DirectRegistryConfig() {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setMap();
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    public DirectRegistryConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setMap();
    }

    static DirectRegistryConfig load() {
        return new DirectRegistryConfig();
    }

    static DirectRegistryConfig load(String configName) {
        return new DirectRegistryConfig(configName);
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setMap();
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    Config getConfig() {
        return config;
    }

    public Map<String, List<URL>> getDirectUrls() {
        return directUrls;
    }

    public void setDirectUrls(Map<String, List<URL>> directUrls) {
        this.directUrls = directUrls;
    }

    private void setMap() {
        Map<String, String> map = new LinkedHashMap<>();
        if(getMappedConfig() != null) {
            if (getMappedConfig().get(DIRECT_URLS) instanceof String) {
                String s = (String) getMappedConfig().get(DIRECT_URLS);
                s = s.trim();
                if (logger.isTraceEnabled()) logger.trace("s = " + s);
                if (s.startsWith("{")) {
                    // json map
                    try {
                        map = Config.getInstance().getMapper().readValue(s, Map.class);
                    } catch (IOException e) {
                        logger.error("IOException:", e);
                    }
                } else {
                    for (String keyValue : s.split(" *& *")) {
                        String[] pairs = keyValue.split(" *= *", 2);
                        map.put(pairs[0], pairs.length == 1 ? "" : pairs[1]);
                    }
                }
            } else if (getMappedConfig().get(DIRECT_URLS) instanceof Map) {
                map = (Map<String, String>) getMappedConfig().get(DIRECT_URLS);
            } else {
                logger.error("mapping is missing or wrong type.");
            }
            // now convert the value of the map to a list of URLs.
            directUrls = new HashMap<>();
            map.entrySet().stream().forEach(x -> {
                List<String> urls = Arrays.asList(x.getValue().split(","));
                directUrls.put(x.getKey(), urls.stream().map(URLImpl::valueOf).collect(Collectors.toUnmodifiableList()));
            });
        }
    }
}
