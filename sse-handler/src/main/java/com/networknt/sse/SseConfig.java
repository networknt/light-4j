package com.networknt.sse;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.*;
import com.networknt.server.ModuleRegistry;

import java.util.List;
import java.util.Map;

@ConfigSchema(
        configKey = "sse",
        configName = "sse",
        configDescription = "SSE Handler Configuration",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
public class SseConfig {
    public static final String CONFIG_NAME = "sse";
    public static final String ENABLED = "enabled";
    public static final String PATH = "path";
    public static final String KEEP_ALIVE_INTERVAL = "keepAliveInterval";
    private static final String METRICS_INJECTION = "metricsInjection";
    private static final String METRICS_NAME = "metricsName";
    private static final String PATH_PREFIXES = "pathPrefixes";

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            description = "Enable SSE Handler",
            defaultValue = "true"
    )
    boolean enabled;

    @StringField(
            configFieldName = PATH,
            externalizedKeyName = PATH,
            description = "Default SSE Endpoint Path if there is only one endpoint for SSE Handler.",
            defaultValue = "/sse"
    )
    String path;

    @IntegerField(
            configFieldName = KEEP_ALIVE_INTERVAL,
            externalizedKeyName = KEEP_ALIVE_INTERVAL,
            description =
                    """
                    Default keep-alive interval in milliseconds for path prefixes not defined in pathPrefixes or there is
                    only one endpoint for SSE Handler.
                    """,
            defaultValue = "10000"
    )
    int keepAliveInterval;

    @ArrayField(
            configFieldName = PATH_PREFIXES,
            externalizedKeyName = PATH_PREFIXES,
            description =
                    """
                    Define path prefix related configuration properties as a list of key/value pairs. Make sure that there is
                    a pathPrefix key in the config along with keepAliveInterval in milliseconds for the pathPrefix. If request
                    path cannot match to one of the pathPrefixes, the request will be skipped.
                      - pathPrefix: /sse/abc
                        keepAliveInterval: 20000
                      - pathPrefix: /sse/def
                        keepAliveInterval: 40000
                    """,
            items = PathPrefix.class
    )
    List<PathPrefix> pathPrefixes;

    @BooleanField(
            configFieldName = METRICS_INJECTION,
            externalizedKeyName = METRICS_INJECTION,
            defaultValue = "false",
            description = "When RouterHandler is used in the http-sidecar or light-gateway, it can collect the metrics info for the\n" +
                    "total response time of the downstream API. With this value injected, users can quickly determine how much\n" +
                    "time the http-sidecar or light-gateway handlers spend and how much time the downstream API spends, including\n" +
                    "the network latency. By default, it is false, and metrics will not be collected and injected into the metrics\n" +
                    "handler configured in the request/response chain."
    )
    boolean metricsInjection;

    @StringField(
            configFieldName = METRICS_NAME,
            externalizedKeyName = METRICS_NAME,
            defaultValue = "router-response",
            description = "When the metrics info is injected into the metrics handler, we need to pass a metric name to it so that the\n" +
                    "metrics info can be categorized in a tree structure under the name. By default, it is router-response, and\n" +
                    "users can change it."
    )
    String metricsName;

    private final Map<String, Object> mappedConfig;
    private static volatile SseConfig instance;

    private SseConfig(String configName) {
        mappedConfig = Config.getInstance().getJsonMapConfig(configName);
        setConfigData();
    }

    private SseConfig() {
        this(CONFIG_NAME);
    }

    public static SseConfig load() {
        return load(CONFIG_NAME);
    }

    public static SseConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (SseConfig.class) {
                mappedConfig = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new SseConfig(configName);
                // Register the module with the configuration.
                ModuleRegistry.registerModule(configName, SseConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), null);
                return instance;
            }
        }
        return new SseConfig(configName);
    }

    private void setConfigData() {
        if (mappedConfig != null) {
            Object object = mappedConfig.get(ENABLED);
            if (object != null) enabled = Config.loadBooleanValue(ENABLED, object);
            object = mappedConfig.get(PATH);
            if (object != null) path = (String) object;
            object = mappedConfig.get(KEEP_ALIVE_INTERVAL);
            if (object != null) keepAliveInterval = Config.loadIntegerValue(KEEP_ALIVE_INTERVAL, object);
            object = getMappedConfig().get(METRICS_INJECTION);
            if(object != null) metricsInjection = Config.loadBooleanValue(METRICS_INJECTION, object);
            object = getMappedConfig().get(METRICS_NAME);
            if(object != null ) metricsName = (String)object;
            setPathPrefixesList();
        }
    }

    private void setPathPrefixesList() {
        if (mappedConfig.get(PATH_PREFIXES) != null) {
            Object object = mappedConfig.get(PATH_PREFIXES);
            pathPrefixes = new java.util.ArrayList<>();
            if (object instanceof String) {
                String s = (String) object;
                s = s.trim();
                if (s.startsWith("[")) {
                    try {
                        List<Map<String, Object>> values = Config.getInstance().getMapper().readValue(s, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                        pathPrefixes = populatePathPrefixes(values);
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the pathPrefixes json with a list of string and object.");
                    }
                } else {
                    throw new ConfigException("pathPrefixes must be a list of string object map.");
                }
            } else if (object instanceof List) {
                List<Map<String, Object>> values = (List<Map<String, Object>>) object;
                pathPrefixes = populatePathPrefixes(values);
            } else {
                throw new ConfigException("pathPrefixes must be a list of string object map.");
            }
        }
    }

    private List<PathPrefix> populatePathPrefixes(List<Map<String, Object>> values) {
        List<PathPrefix> prefixes = new java.util.ArrayList<>();
        for (Map<String, Object> value : values) {
            PathPrefix pathPrefix = new PathPrefix();
            pathPrefix.setPathPrefix((String) value.get("pathPrefix"));
            Object keepAliveIntervalObj = value.get("keepAliveInterval");
            if (keepAliveIntervalObj != null) {
                int keepAliveInterval;
                if (keepAliveIntervalObj instanceof Number) {
                    keepAliveInterval = ((Number) keepAliveIntervalObj).intValue();
                } else if (keepAliveIntervalObj instanceof String) {
                    try {
                        keepAliveInterval = Integer.parseInt((String) keepAliveIntervalObj);
                    } catch (NumberFormatException e) {
                        throw new ConfigException("Invalid keepAliveInterval value for pathPrefix '" +
                                pathPrefix.getPathPrefix() + "': must be an integer, but was '" +
                                keepAliveIntervalObj + "'");
                    }
                } else {
                    throw new ConfigException("Invalid keepAliveInterval type for pathPrefix '" +
                            pathPrefix.getPathPrefix() + "': " + keepAliveIntervalObj.getClass().getName());
                }
                pathPrefix.setKeepAliveInterval(keepAliveInterval);
            }
            prefixes.add(pathPrefix);
        }
        return prefixes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getPath() {
        return path;
    }

    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public boolean isMetricsInjection() {
        return metricsInjection;
    }

    public String getMetricsName() {
        return metricsName;
    }

    public List<PathPrefix> getPathPrefixes() {
        return pathPrefixes;
    }

}
