package com.networknt.registry.support;

import com.networknt.config.Config;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.MapField;
import com.networknt.config.schema.OutputFormat;
import com.networknt.registry.URL;
import com.networknt.registry.URLImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@ConfigSchema(configKey = "direct-registry", configName = "direct-registry", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML})
public class DirectRegistryConfig {
    private static final Logger logger = LoggerFactory.getLogger(DirectRegistryConfig.class);

    public static final String CONFIG_NAME = "direct-registry";
    private static final String DIRECT_URLS = "directUrls";

    @MapField(
            configFieldName = DIRECT_URLS,
            externalizedKeyName = DIRECT_URLS,
            externalized = true,
            description = "For light-gateway or http-sidecar that needs to reload configuration for the router hosts, you can define the\n" +
                    "service to hosts mapping in this configuration to overwrite the definition in the service.yml file as part of\n" +
                    "the parameters. This configuration will only be used if parameters in the service.yml for DirectRegistry is null.\n" +
                    "\n" +
                    "directUrls is the mapping between the serviceId to the hosts separated by comma. If environment tag is used, you\n" +
                    "can add it to the serviceId separated with a vertical bar |",
            valueType = List.class
    )
    Map<String, List<URL>> directUrls;

    private final Config config;
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
                // change this to warning as the service.yml configuration is still supported.
                logger.warn("mapping is missing or wrong type.");
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
