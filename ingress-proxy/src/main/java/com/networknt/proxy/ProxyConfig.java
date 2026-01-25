package com.networknt.proxy;

import com.networknt.config.Config;
import com.networknt.config.schema.ConfigSchema; // REQUIRED IMPORT
import com.networknt.config.schema.OutputFormat; // REQUIRED IMPORT
import com.networknt.config.schema.BooleanField; // REQUIRED IMPORT
import com.networknt.config.schema.IntegerField; // REQUIRED IMPORT
import com.networknt.config.schema.StringField; // REQUIRED IMPORT
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.server.ModuleRegistry;

import java.util.Map;

/**
 * Config class for reverse proxy. This config class supports reload.
 *
 * @author Steve Hu
 */
// <<< REQUIRED ANNOTATION FOR SCHEMA GENERATION >>>
@ConfigSchema(
        configKey = "proxy",
        configName = "proxy",
        configDescription = "Reverse Proxy Handler Configuration",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
public class ProxyConfig {
    private static final Logger logger = LoggerFactory.getLogger(ProxyConfig.class);

    public static final String CONFIG_NAME = "proxy";
    private static final String ENABLED = "enabled";
    private static final String HTTP2_ENABLED = "http2Enabled";
    private static final String HOSTS = "hosts";
    private static final String CONNECTIONS_PER_THREAD = "connectionsPerThread";
    private static final String MAX_REQUEST_TIME = "maxRequestTime";
    private static final String REWRITE_HOST_HEADER = "rewriteHostHeader";
    private static final String REUSE_X_FORWARDED = "reuseXForwarded";
    private static final String MAX_CONNECTION_RETRIES = "maxConnectionRetries";
    private static final String MAX_QUEUE_SIZE = "maxQueueSize";
    private static final String FORWARD_JWT_CLAIMS = "forwardJwtClaims";
    private static final String METRICS_INJECTION = "metricsInjection";
    private static final String METRICS_NAME = "metricsName";

    private Config config;
    private Map<String, Object> mappedConfig;

    // --- Annotated Fields ---

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            description = "Indicate if the proxy handler is enabled or not.",
            defaultValue = "true" // Assuming true is the intended default if not explicitly set in YAML
    )
    boolean enabled;

    @BooleanField(
            configFieldName = HTTP2_ENABLED,
            externalizedKeyName = HTTP2_ENABLED,
            description = "If HTTP 2.0 protocol will be used to connect to target servers. Only if all host are using https\n" +
                    "and support the HTTP2 can set this one to true.\n",
            defaultValue = "false"
    )
    boolean http2Enabled;

    @StringField(
            configFieldName = HOSTS,
            externalizedKeyName = HOSTS,
            description = "Target URIs. Use comma separated string for multiple hosts. You can have mix http and https and\n" +
                    "they will be load balanced. If the host start with https://, then TLS context will be created.\n",
            defaultValue = "http://localhost:8080"
    )
    String hosts;

    @IntegerField(
            configFieldName = CONNECTIONS_PER_THREAD,
            externalizedKeyName = CONNECTIONS_PER_THREAD,
            description = "Connections per thread to the target servers.",
            defaultValue = "20"
    )
    int connectionsPerThread;

    @IntegerField(
            configFieldName = MAX_REQUEST_TIME,
            externalizedKeyName = MAX_REQUEST_TIME,
            description = "Max request time in milliseconds before timeout.",
            defaultValue = "1000"
    )
    int maxRequestTime;

    @BooleanField(
            configFieldName = REWRITE_HOST_HEADER,
            externalizedKeyName = REWRITE_HOST_HEADER,
            description = "Rewrite Host Header with the target host and port and write X_FORWARDED_HOST with original host.",
            defaultValue = "true"
    )
    boolean rewriteHostHeader;

    @BooleanField(
            configFieldName = REUSE_X_FORWARDED,
            externalizedKeyName = REUSE_X_FORWARDED,
            description = "Reuse XForwarded for the target XForwarded header.",
            defaultValue = "false"
    )
    boolean reuseXForwarded;

    @IntegerField(
            configFieldName = MAX_CONNECTION_RETRIES,
            externalizedKeyName = MAX_CONNECTION_RETRIES,
            description = "Max Connection Retries.",
            defaultValue = "3"
    )
    int maxConnectionRetries;

    @IntegerField(
            configFieldName = MAX_QUEUE_SIZE,
            externalizedKeyName = MAX_QUEUE_SIZE,
            description = "The max queue size for the requests if there is no connection to the downstream API in the connection pool.\n" +
                    "The default value is 0 that means there is queued requests. As we have maxConnectionRetries, there is no\n" +
                    "need to use the request queue to increase the memory usage. It should only be used when you see 503 errors\n" +
                    "in the log after maxConnectionRetries to accommodate slow backend API.\n",
            defaultValue = "0"
    )
    int maxQueueSize;

    @BooleanField(
            configFieldName = FORWARD_JWT_CLAIMS,
            externalizedKeyName = FORWARD_JWT_CLAIMS,
            description = "Decode the JWT token claims and forward to the backend api in the form of json string.",
            defaultValue = "false"
    )
    private boolean forwardJwtClaims;

    @BooleanField(
            configFieldName = METRICS_INJECTION,
            externalizedKeyName = METRICS_INJECTION,
            description = "When LightProxyHandler is used in the http-sidecar or light-gateway, it can collect the metrics info for the\n" +
                    "total response time of the downstream API. With this value injected, users can quickly determine how much\n" +
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
                    "metrics info can be categorized in a tree structure under the name. By default, it is proxy-response, and\n" +
                    "users can change it.\n",
            defaultValue = "proxy-response"
    )
    String metricsName;


    // --- Constructor and Loading Logic ---

    private static volatile ProxyConfig instance;

    // --- Constructor and Loading Logic ---

    private ProxyConfig() {
        this(CONFIG_NAME);
    }

    private ProxyConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        setConfigData();
    }

    public static ProxyConfig load() {
        return load(CONFIG_NAME);
    }

    public static ProxyConfig load(String configName) {
        ProxyConfig config = instance;
        if (config == null || config.getMappedConfig() != Config.getInstance().getJsonMapConfig(configName)) {
            synchronized (ProxyConfig.class) {
                config = instance;
                if (config == null || config.getMappedConfig() != Config.getInstance().getJsonMapConfig(configName)) {
                    config = new ProxyConfig(configName);
                    instance = config;
                    // Register the module with the new config
                    ModuleRegistry.registerModule(configName, LightProxyHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfig(configName), null);
                }
            }
        }
        return config;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    // --- Getters and Setters (Original Methods) ---

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isHttp2Enabled() {
        return http2Enabled;
    }

    public void setHttp2Enabled(boolean http2Enabled) {
        this.http2Enabled = http2Enabled;
    }

    public String getHosts() {
        return hosts;
    }

    public void setHosts(String hosts) {
        this.hosts = hosts;
    }

    public int getConnectionsPerThread() {
        return connectionsPerThread;
    }

    public void setConnectionsPerThread(int connectionsPerThread) {
        this.connectionsPerThread = connectionsPerThread;
    }

    public int getMaxRequestTime() {
        return maxRequestTime;
    }

    public void setMaxRequestTime(int maxRequestTime) {
        this.maxRequestTime = maxRequestTime;
    }

    public boolean isRewriteHostHeader() { return rewriteHostHeader; }

    public void setRewriteHostHeader(boolean rewriteHostHeader) { this.rewriteHostHeader = rewriteHostHeader; }

    public boolean isReuseXForwarded() { return reuseXForwarded; }

    public void setReuseXForwarded(boolean reuseXForwarded) { this.reuseXForwarded = reuseXForwarded; }

    public int getMaxConnectionRetries() { return maxConnectionRetries; }

    public void setMaxConnectionRetries(int maxConnectionRetries) { this.maxConnectionRetries = maxConnectionRetries; }

    public int getMaxQueueSize() { return maxQueueSize; }

    public void setMaxQueueSize(int maxQueueSize) { this.maxQueueSize = maxQueueSize; }

    public boolean isForwardJwtClaims() {
        return forwardJwtClaims;
    }

    public void setForwardJwtClaims(boolean forwardJwtClaims) {
        this.forwardJwtClaims = forwardJwtClaims;
    }

    public boolean isMetricsInjection() { return metricsInjection; }

    public void setMetricsInjection(boolean metricsInjection) { this.metricsInjection = metricsInjection; }

    public String getMetricsName() { return metricsName; }

    public void setMetricsName(String metricsName) { this.metricsName = metricsName; }

    private void setConfigData() {
        Object object = getMappedConfig().get(ENABLED);
        if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);

        object = getMappedConfig().get(HTTP2_ENABLED);
        if(object != null) http2Enabled = Config.loadBooleanValue(HTTP2_ENABLED, object);

        object = getMappedConfig().get(REWRITE_HOST_HEADER);
        if(object != null) rewriteHostHeader = Config.loadBooleanValue(REWRITE_HOST_HEADER, object);

        object = getMappedConfig().get(REUSE_X_FORWARDED);
        if(object != null) reuseXForwarded = Config.loadBooleanValue(REUSE_X_FORWARDED, object);

        object = getMappedConfig().get(FORWARD_JWT_CLAIMS);
        if(object != null) forwardJwtClaims = Config.loadBooleanValue(FORWARD_JWT_CLAIMS, object);

        object = getMappedConfig().get(HOSTS);
        if(object != null) hosts = (String)object;

        object = getMappedConfig().get(METRICS_NAME);
        if(object != null ) metricsName = (String)object;

        object = getMappedConfig().get(CONNECTIONS_PER_THREAD);
        if(object != null) connectionsPerThread = Config.loadIntegerValue(CONNECTIONS_PER_THREAD, object);

        object = getMappedConfig().get(MAX_REQUEST_TIME);
        if(object != null) maxRequestTime = Config.loadIntegerValue(MAX_REQUEST_TIME, object);

        object = getMappedConfig().get(MAX_CONNECTION_RETRIES);
        if(object != null) maxConnectionRetries = Config.loadIntegerValue(MAX_CONNECTION_RETRIES, object);

        object = getMappedConfig().get(MAX_QUEUE_SIZE);
        if(object != null) maxQueueSize = Config.loadIntegerValue(MAX_QUEUE_SIZE, object);

        object = getMappedConfig().get(METRICS_INJECTION);
        if(object != null) metricsInjection = Config.loadBooleanValue(METRICS_INJECTION, object);
    }
}
