package com.networknt.registry.support;

import com.networknt.config.Config;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.MapField;
import com.networknt.config.schema.OutputFormat;
import com.networknt.registry.URL;
import com.networknt.registry.URLImpl;
import com.networknt.server.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Configuration for direct registry that defines service to hosts mapping.
 */
@ConfigSchema(
        configKey = "direct-registry",
        configName = "direct-registry",
        configDescription = "Direct registry configuration",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD})
public class DirectRegistryConfig {
    private static final Logger logger = LoggerFactory.getLogger(DirectRegistryConfig.class);

    /** Constant for configuration name */
    public static final String CONFIG_NAME = "direct-registry";
    private static final String DIRECT_URLS = "directUrls";

    @MapField(
            configFieldName = DIRECT_URLS,
            externalizedKeyName = DIRECT_URLS,
            description = "For light-gateway or http-sidecar that needs to reload configuration for the router hosts, you can define the\n" +
                    "service to hosts mapping in this configuration to overwrite the definition in the service.yml file as part of\n" +
                    "the parameters. This configuration will only be used if parameters in the service.yml for DirectRegistry is null.\n" +
                    "\n" +
                    "directUrls is the mapping between the serviceId to the hosts separated by comma. If environment tag is used, you\n" +
                    "can add it to the serviceId separated with a vertical bar |\n" +
                    "The following is in YAML format.\n" +
                    "  code: http://192.168.1.100:6881,http://192.168.1.101:6881\n" +
                    "  token: http://192.168.1.100:6882\n" +
                    "  com.networknt.test-1.0.0: http://localhost,https://localhost\n" +
                    "  command|0000: https://192.168.1.142:8440\n" +
                    "  command|0001: https://192.168.1.142:8441\n" +
                    "  command|0002: https://192.168.1.142:8442\n" +
                    "\n" +
                    "The following is in JSON string format.\n" +
                    "directUrls: {\"code\":\"http://192.168.1.100:6881,http://192.168.1.101:6881\",\"token\":\"http://192.168.1.100:6882\",\"com.networknt.test-1.0.0\":\"http://localhost,https://localhost\",\"command|0000\":\"https://192.168.1.142:8440\",\"command|0001\":\"https://192.168.1.142:8441\",\"command|0002\":\"https://192.168.1.142:8442\"}\n" +
                    "\n" +
                    "The following is in string map format.\n" +
                    "directUrls: code=http://192.168.1.100:6881,http://192.168.1.101:6881&token=http://192.168.1.100:6882&com.networknt.test-1.0.0=http://localhost,https://localhost&command|0000=https://192.168.1.142:8440&command|0001=https://192.168.1.142:8441&command|0002=https://192.168.1.142:8442",
            valueType = List.class
    )
    Map<String, List<URL>> directUrls;

    private static volatile DirectRegistryConfig instance;
    private final Config config;
    private Map<String, Object> mappedConfig;

    private DirectRegistryConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private DirectRegistryConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        setMap();
    }

    /**
     * Loads the direct registry configuration from the default config name.
     *
     * @return DirectRegistryConfig object
     */
    public static DirectRegistryConfig load() {
        return load(CONFIG_NAME);
    }

    /**
     * Loads the direct registry configuration from a specific config name.
     *
     * @param configName config name
     * @return DirectRegistryConfig object
     */
    public static DirectRegistryConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (DirectRegistryConfig.class) {
                mappedConfig = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new DirectRegistryConfig(configName);
                ModuleRegistry.registerModule(CONFIG_NAME, DirectRegistryConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
                return instance;
            }
        }
        return new DirectRegistryConfig(configName);
    }

    /**
     * Gets the mapped configuration.
     *
     * @return Map mapped configuration
     */
    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    Config getConfig() {
        return config;
    }

    /**
     * Gets the direct URLs mapping.
     *
     * @return Map direct URLs mapping
     */
    public Map<String, List<URL>> getDirectUrls() {
        return directUrls;
    }

    /**
     * Sets the direct URLs mapping.
     *
     * @param directUrls map of direct URLs
     */
    public void setDirectUrls(Map<String, List<URL>> directUrls) {
        this.directUrls = directUrls;
    }

    private void setMap() {
        directUrls = new HashMap<>();
        Map<String, Object> map = new LinkedHashMap<>();
        if(getMappedConfig() != null) {
            Object directUrlsObject = getMappedConfig().get(DIRECT_URLS);
            if (directUrlsObject instanceof String) {
                String s = ((String) directUrlsObject).trim();
                if (logger.isTraceEnabled()) logger.trace("s = " + s);
                if (s.isEmpty()) {
                    return;
                }
                if (s.startsWith("{")) {
                    // json map
                    try {
                        map = Config.getInstance().getMapper().readValue(s, Map.class);
                    } catch (IOException e) {
                        logger.error("IOException:", e);
                    }
                } else {
                    for (String keyValue : s.split(" *& *")) {
                        if (keyValue.isBlank()) {
                            continue;
                        }
                        String[] pairs = keyValue.split(" *= *", 2);
                        if (pairs[0].isBlank()) {
                            continue;
                        }
                        map.put(pairs[0], pairs.length == 1 ? "" : pairs[1]);
                    }
                }
            } else if (directUrlsObject instanceof Map<?, ?> rawMap) {
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    if (entry.getKey() != null) {
                        map.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
            } else if (directUrlsObject != null) {
                // change this to warning as the service.yml configuration is still supported.
                logger.warn("mapping is wrong type.");
            }
            // now convert the value of the map to a list of URLs.
            map.entrySet().stream().forEach(x -> {
                List<URL> urls = parseDirectUrls(x.getValue());
                if (!x.getKey().isBlank() && !urls.isEmpty()) {
                    directUrls.put(x.getKey(), urls);
                }
            });
        }
    }

    private List<URL> parseDirectUrls(Object value) {
        List<URL> urls = new ArrayList<>();
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> addDirectUrls(urls, item));
        } else {
            addDirectUrls(urls, value);
        }
        return urls.stream().collect(Collectors.toUnmodifiableList());
    }

    private void addDirectUrls(List<URL> urls, Object value) {
        if (value == null) {
            return;
        }
        for (String directUrl : String.valueOf(value).split(",")) {
            String trimmed = directUrl.trim();
            if (!trimmed.isEmpty()) {
                urls.add(URLImpl.valueOf(trimmed));
            }
        }
    }
}
