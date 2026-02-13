package com.networknt.proxy;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import com.networknt.config.schema.ConfigSchema; // REQUIRED IMPORT
import com.networknt.config.schema.OutputFormat; // REQUIRED IMPORT
import com.networknt.config.schema.BooleanField; // REQUIRED IMPORT
import com.networknt.config.schema.IntegerField; // REQUIRED IMPORT
import com.networknt.config.schema.StringField; // REQUIRED IMPORT
import com.networknt.config.schema.ArrayField; // REQUIRED IMPORT
import com.networknt.handler.config.UrlRewriteRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Config class for ExternalServiceHandler
 *
 * @author Steve Hu
 */
@ConfigSchema(
        configKey = "externalService",
        configName = "external-service",
        configDescription = "Configuration for external service handler to access third party services through proxy/gateway.",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
    )
public class ExternalServiceConfig {
    private static final Logger logger = LoggerFactory.getLogger(ExternalServiceConfig.class);
    /** Config Name */
    public static final String CONFIG_NAME = "external-service";
    private static final String ENABLED = "enabled";
    private static final String PROXY_HOST = "proxyHost";
    private static final String PROXY_PORT = "proxyPort";
    private static final String ENABLE_HTTP2 = "enableHttp2";
    private static final String PATH_HOST_MAPPINGS = "pathHostMappings";
    private static final String METRICS_INJECTION = "metricsInjection";
    private static final String METRICS_NAME = "metricsName";
    private static final String MAX_CONNECTION_RETRIES = "maxConnectionRetries";
    private static final String URL_REWRITE_RULES = "urlRewriteRules"; // Constant for rewrite rules
    private static final String CONNECT_TIMEOUT = "connectTimeout";
    private static final String TIMEOUT = "timeout";
    private static final String PATH_PREFIXES = "pathPrefixes";
    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            description = "Indicate if the handler is enabled or not",
            defaultValue = "false"
    )
    boolean enabled;

    @StringField(
            configFieldName = PROXY_HOST,
            externalizedKeyName = PROXY_HOST,
            description = "Proxy Host if calling within the corp network with a gateway like Mcafee gateway."
    )
    String proxyHost;

    @IntegerField(
            configFieldName = PROXY_PORT,
            externalizedKeyName = PROXY_PORT,
            description = "Proxy Port if proxy host is used. default value will be 443 which means HTTPS."
    )
    int proxyPort;

    @IntegerField(
            configFieldName = CONNECT_TIMEOUT,
            externalizedKeyName = CONNECT_TIMEOUT,
            description = "Connect Timeout in milliseconds. It is used to overwrite the connectTimeout in the client.yml. The default\n" +
                    "value is 3000.\n",
            defaultValue = "3000"
    )
    int connectTimeout;

    @IntegerField(
            configFieldName = TIMEOUT,
            externalizedKeyName = TIMEOUT,
            description = "Timeout in milliseconds. It is used to overwrite the timeout in the client.yml. The default value is 5000.",
            defaultValue = "5000"
    )
    int timeout;

    @BooleanField(
            configFieldName = ENABLE_HTTP2,
            externalizedKeyName = ENABLE_HTTP2,
            description = "If HTTP2 is used to connect to the external service.",
            defaultValue = "false"
    )
    boolean enableHttp2;

    @IntegerField(
            configFieldName = MAX_CONNECTION_RETRIES,
            externalizedKeyName = MAX_CONNECTION_RETRIES,
            description = "Max Connection Retries",
            defaultValue = "3"
    )
    int maxConnectionRetries;

    // Path Host Mappings (String[])
    @ArrayField(
            configFieldName = PATH_HOST_MAPPINGS,
            externalizedKeyName = PATH_HOST_MAPPINGS,
            description = "A list of request path to the service host mappings. Other requests will skip this handler. The value is\n" +
                    "a string with two parts. The first part is the path and the second is the target host the request is\n" +
                    "finally routed to.\n",
            items = String.class // Items are strings that will be split later
    )
    List<String[]> pathHostMappings; // Keep as List<String[]>

    // URL Rewrite Rules (UrlRewriteRule)
    @ArrayField(
            configFieldName = URL_REWRITE_RULES,
            externalizedKeyName = URL_REWRITE_RULES,
            description = "URL rewrite rules, each line will have two parts: the regex pattern and replace string separated\n" +
                    "with a space. For details, please refer to the light-router router.yml configuration.\n" +
                    "Test your rules at https://www.freeformatter.com/java-regex-tester.html\n",
            items = String.class // Items are strings that will be converted to UrlRewriteRule later
    )
    List<UrlRewriteRule> urlRewriteRules; // Keep as List<UrlRewriteRule>

    @ArrayField(
            configFieldName = PATH_PREFIXES,
            externalizedKeyName = PATH_PREFIXES,
            description =
            """
            Define path prefix related configuration properties as a list of key/value pairs. Make sure that there is
            a pathPrefix key in the config along with timeout for request/response exchange timeout in milliseconds and
            host for target host of the external API. If request path cannot match to one of the pathPrefixes, the request
            will be skipped.
            Note that this config properties will replace the pathHostMappings which is for single purpose only.
              - pathPrefix: /sharepoint
                timeout: 2000
                host: https://sharepoint.microsoft.com
              - pathPrefix: /v1/petstore
                timeout: 4000
                host: https://petstore.example.com
            """,
            items = PathPrefix.class
    )
    List<PathPrefix> pathPrefixes;

    @BooleanField(
            configFieldName = METRICS_INJECTION,
            externalizedKeyName = METRICS_INJECTION,
            description = "When ExternalServiceHandler is used in the http-sidecar or light-gateway, it can collect the metrics info for\n" +
                    "the total response time of the downstream API. With this value injected, users can quickly determine how much\n" +
                    "time the http-sidecar or light-gateway handlers spend and how much time the downstream API spends, including\n" +
                    "the network latency. By default, it is false, and metrics will not be collected and injected into the metrics\n" +
                    "handler configured in the request/response chain.\n",
            defaultValue = "false"
    )
    boolean metricsInjection;

    @StringField(
            configFieldName = METRICS_NAME,
            externalizedKeyName = METRICS_NAME,
            description = "When the metrics info is injected into the metrics handler, we need to pass a metric name to it so that the\n" +
                    "metrics info can be categorized in a tree structure under the name. By default, it is external-response, and\n" +
                    "users can change it.\n",
            defaultValue = "external-response"
    )
    String metricsName;

    private static final String VERIFY_HOSTNAME = "verifyHostname";

    @BooleanField(
            configFieldName = VERIFY_HOSTNAME,
            externalizedKeyName = VERIFY_HOSTNAME,
            description = "Indicate if the hostname verification is enabled or not",
            defaultValue = "true"
    )
    boolean verifyHostname = true;


    private final Config config;
    private Map<String, Object> mappedConfig;

    // --- Constructor and Loading Logic ---

    /**
     * Construct ExternalServiceConfig with default config name
     */
    public ExternalServiceConfig() {
        this(CONFIG_NAME);
    }

    private ExternalServiceConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        setConfigData(); // Load annotated fields first
        setUrlRewriteRules(); // Load and convert rewrite rules
        setConfigList(); // Load and convert pathHostMappings
    }

    /**
     * Load config
     * @return ExternalServiceConfig
     */
    public static ExternalServiceConfig load() {
        return new ExternalServiceConfig();
    }

    /**
     * Load config
     * @param configName config name
     * @return ExternalServiceConfig
     */
    public static ExternalServiceConfig load(String configName) {
        return new ExternalServiceConfig(configName);
    }

    // --- Private Setters for Annotated Fields ---

    private void setConfigData() {
        // Load simple annotated fields using standard Config loader/mapper logic
        Object object = mappedConfig.get(ENABLED);
        if (object != null) enabled = Config.loadBooleanValue(ENABLED, object);

        object = mappedConfig.get(PROXY_HOST);
        if (object != null) proxyHost = (String) object; // String field, no load call needed if directly read

        object = mappedConfig.get(PROXY_PORT);
        if (object != null) proxyPort = Config.loadIntegerValue(PROXY_PORT, object);

        object = mappedConfig.get(CONNECT_TIMEOUT);
        if (object != null) connectTimeout = Config.loadIntegerValue(CONNECT_TIMEOUT, object);

        object = mappedConfig.get(TIMEOUT);
        if (object != null) timeout = Config.loadIntegerValue(TIMEOUT, object);

        object = mappedConfig.get(MAX_CONNECTION_RETRIES);
        if (object != null) maxConnectionRetries = Config.loadIntegerValue(MAX_CONNECTION_RETRIES, object);

        object = mappedConfig.get(ENABLE_HTTP2);
        if (object != null) enableHttp2 = Config.loadBooleanValue(ENABLE_HTTP2, object);

        object = mappedConfig.get(METRICS_INJECTION);
        if(object != null) metricsInjection = Config.loadBooleanValue(METRICS_INJECTION, object);

        object = mappedConfig.get(METRICS_NAME);
        if(object != null ) metricsName = (String)object; // String field, no load call needed

        object = mappedConfig.get(VERIFY_HOSTNAME);
        if(object != null) verifyHostname = Config.loadBooleanValue(VERIFY_HOSTNAME, object);
    }

    // --- Custom Setters for Complex List Fields ---

    // Renamed setConfigList to setPathHostMappingsList for clarity
    private void setPathHostMappingsList() {
        if (mappedConfig.get(PATH_HOST_MAPPINGS) != null) {
            Object object = mappedConfig.get(PATH_HOST_MAPPINGS);
            pathHostMappings = new ArrayList<>();
            // The original logic handles String (single/json array) and List<String> inputs.
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(s.startsWith("[")) {
                    // multiple path to host mappings (JSON list of strings)
                    List<String> mappings = (List<String>) JsonMapper.fromJson(s, List.class);
                    for (String mapping : mappings) {
                        String[] parts = mapping.split(" ");
                        if(parts.length != 2) {
                            throw new ConfigException("path host entry must have two elements separated by a space: " + mapping);
                        }
                        pathHostMappings.add(parts);
                    }
                } else {
                    // single path to host mapping (space separated string)
                    String[] parts = s.split(" ");
                    if(parts.length != 2) {
                        throw new ConfigException("path host entry must have two elements separated by a space: " + s);
                    }
                    pathHostMappings.add(parts);
                }
            } else if (object instanceof List) {
                List<String> maps = (List<String>)object;
                for(String s: maps) {
                    String[] parts = s.split(" ");
                    if(parts.length != 2) {
                        throw new ConfigException("path host entry must have two elements separated by a space: " + s);
                    }
                    pathHostMappings.add(parts);
                }
            } else {
                throw new ConfigException("pathHostMappings must be a string or a list of strings.");
            }
        } else {
            pathHostMappings = Collections.emptyList(); // Default to empty list if not configured
        }
    }

    /**
     * set url rewrite rules
     */
    public void setUrlRewriteRules() {
        this.urlRewriteRules = new ArrayList<>();
        if(mappedConfig.get(URL_REWRITE_RULES) != null) {
            // The original logic handles String (single/json array) and List<String> inputs.
            Object object = mappedConfig.get(URL_REWRITE_RULES);
            if (object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(s.startsWith("[")) {
                    // multiple rules (JSON list of strings)
                    List<String> rules = (List<String>) JsonMapper.fromJson(s, List.class);
                    for (String rule : rules) {
                        urlRewriteRules.add(UrlRewriteRule.convertToUrlRewriteRule(rule));
                    }
                } else {
                    // single rule (space separated string)
                    urlRewriteRules.add(UrlRewriteRule.convertToUrlRewriteRule(s));
                }
            } else if (object instanceof List) {
                List<String> rules = (List)object;
                for (String s : rules) {
                    urlRewriteRules.add(UrlRewriteRule.convertToUrlRewriteRule(s));
                }
            } else {
                throw new ConfigException("urlRewriteRules must be a string or a list of strings.");
            }
        } else {
            urlRewriteRules = Collections.emptyList(); // Default to empty list if not configured
        }
    }

    // Combine list loading into a single method called by constructors
    private void setConfigList() {
        setPathHostMappingsList();
        setUrlRewriteRules();
        setPathPrefixesList();
    }

    private void setPathPrefixesList() {
        if (mappedConfig.get(PATH_PREFIXES) != null) {
            Object object = mappedConfig.get(PATH_PREFIXES);
            pathPrefixes = new ArrayList<>();
            if (object instanceof String) {
                String s = (String) object;
                s = s.trim();
                if (s.startsWith("[")) {
                    try {
                        List<Map<String, Object>> values = Config.getInstance().getMapper().readValue(s, new com.fasterxml.jackson.core.type.TypeReference<>() {});
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
        List<PathPrefix> prefixes = new ArrayList<>();
        for (Map<String, Object> value : values) {
            PathPrefix pathPrefix = new PathPrefix();
            pathPrefix.setPathPrefix((String) value.get("pathPrefix"));
            pathPrefix.setHost((String) value.get("host"));
            Object timeoutObj = value.get("timeout");
            if (timeoutObj != null) {
                int timeout;
                if (timeoutObj instanceof Number) {
                    timeout = ((Number) timeoutObj).intValue();
                } else if (timeoutObj instanceof String) {
                    try {
                        timeout = Integer.parseInt((String) timeoutObj);
                    } catch (NumberFormatException e) {
                        throw new ConfigException("Invalid timeout value for pathPrefix '" +
                                pathPrefix.getPathPrefix() + "': must be an integer, but was '" +
                                timeoutObj + "'");
                    }
                } else {
                    throw new ConfigException("Invalid timeout type for pathPrefix '" +
                            pathPrefix.getPathPrefix() + "': " + timeoutObj.getClass().getName());
                }
                pathPrefix.setTimeout(timeout);
            }
            prefixes.add(pathPrefix);
        }
        return prefixes;
    }


    // --- Getters and Setters (Original Methods) ---

    /**
     * Get mapped config
     * @return Map
     */
    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    /**
     * is enabled
     * @return boolean
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * set enabled
     * @param enabled boolean
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * get proxy host
     * @return String
     */
    public String getProxyHost() {
        return proxyHost;
    }

    /**
     * set proxy host
     * @param proxyHost String
     */
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    /**
     * get proxy port
     * @return int
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * get connect timeout
     * @return int
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * get timeout
     * @return int
     */
    public int getTimeout() { return timeout; }

    /**
     * get max connection retries
     * @return int
     */
    public int getMaxConnectionRetries() { return maxConnectionRetries; }

    /**
     * set max connection retries
     * @param maxConnectionRetries int
     */
    public void setMaxConnectionRetries(int maxConnectionRetries) { this.maxConnectionRetries = maxConnectionRetries; }

    /**
     * set proxy port
     * @param proxyPort int
     */
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    /**
     * is enable http2
     * @return boolean
     */
    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    /**
     * set enable http2
     * @param enableHttp2 boolean
     */
    public void setEnableHttp2(boolean enableHttp2) {
        this.enableHttp2 = enableHttp2;
    }

    /**
     * is metrics injection
     * @return boolean
     */
    public boolean isMetricsInjection() { return metricsInjection; }

    /**
     * get metrics name
     * @return String
     */
    public String getMetricsName() { return metricsName; }

    /**
     * get path host mappings
     * @return List
     */
    public List<String[]> getPathHostMappings() {
        return pathHostMappings;
    }

    /**
     * set path host mappings
     * @param pathHostMappings List
     */
    public void setPathHostMappings(List<String[]> pathHostMappings) {
        this.pathHostMappings = pathHostMappings;
    }

    /**
     * get url rewrite rules
     * @return List
     */
    public List<UrlRewriteRule> getUrlRewriteRules() {
        return urlRewriteRules;
    }

    /**
     * set url rewrite rules
     * @param urlRewriteRules List
     */
    public void setUrlRewriteRules(List<UrlRewriteRule> urlRewriteRules) {
        this.urlRewriteRules = urlRewriteRules;
    }

    /**
     * get path prefixes
     * @return List
     */
    public List<PathPrefix> getPathPrefixes() {
        return pathPrefixes;
    }

    /**
     * set path prefixes
     * @param pathPrefixes List
     */
    public void setPathPrefixes(List<PathPrefix> pathPrefixes) {
        this.pathPrefixes = pathPrefixes;
    }

    public boolean isVerifyHostname() {
        return verifyHostname;
    }

    public void setVerifyHostname(boolean verifyHostname) {
        this.verifyHostname = verifyHostname;
    }
}
