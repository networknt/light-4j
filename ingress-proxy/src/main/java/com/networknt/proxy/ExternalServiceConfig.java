package com.networknt.proxy;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import com.networknt.handler.config.UrlRewriteRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExternalServiceConfig {
    public static final String CONFIG_NAME = "external-service";
    private static final String ENABLED = "enabled";
    private static final String PROXY_HOST = "proxyHost";
    private static final String PROXY_PORT = "proxyPort";
    private static final String ENABLE_HTTP2 = "enableHttp2";
    private static final String PATH_HOST_MAPPINGS = "pathHostMappings";
    private static final String METRICS_INJECTION = "metricsInjection";
    private static final String METRICS_NAME = "metricsName";
    private static final String MAX_CONNECTION_RETRIES = "maxConnectionRetries";

    boolean enabled;
    String proxyHost;
    int proxyPort;
    boolean enableHttp2;
    boolean metricsInjection;
    String metricsName;
    int maxConnectionRetries;

    List<String[]> pathHostMappings;

    List<UrlRewriteRule> urlRewriteRules;
    private final Config config;
    private Map<String, Object> mappedConfig;

    public ExternalServiceConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private ExternalServiceConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setUrlRewriteRules();
        setConfigList();
    }
    public static ExternalServiceConfig load() {
        return new ExternalServiceConfig();
    }

    public static ExternalServiceConfig load(String configName) {
        return new ExternalServiceConfig(configName);
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setUrlRewriteRules();
        setConfigList();
    }
    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public int getMaxConnectionRetries() { return maxConnectionRetries; }

    public void setMaxConnectionRetries(int maxConnectionRetries) { this.maxConnectionRetries = maxConnectionRetries; }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public void setEnableHttp2(boolean enableHttp2) {
        this.enableHttp2 = enableHttp2;
    }

    public boolean isMetricsInjection() { return metricsInjection; }
    public String getMetricsName() { return metricsName; }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if (object != null) enabled = Config.loadBooleanValue(ENABLED, object);
        object = mappedConfig.get(PROXY_HOST);
        if (object != null) setProxyHost((String) object);
        object = mappedConfig.get(PROXY_PORT);
        if (object != null) proxyPort = Config.loadIntegerValue(PROXY_PORT, object);
        object = mappedConfig.get(MAX_CONNECTION_RETRIES);
        if (object != null) maxConnectionRetries = Config.loadIntegerValue(MAX_CONNECTION_RETRIES, object);
        object = mappedConfig.get(ENABLE_HTTP2);
        if (object != null) enableHttp2 = Config.loadBooleanValue(ENABLE_HTTP2, object);
        object = getMappedConfig().get(METRICS_INJECTION);
        if(object != null) metricsInjection = Config.loadBooleanValue(METRICS_INJECTION, object);
        object = getMappedConfig().get(METRICS_NAME);
        if(object != null ) metricsName = (String)object;
    }

    public List<String[]> getPathHostMappings() {
        return pathHostMappings;
    }

    public void setPathHostMappings(List<String[]> pathHostMappings) {
        this.pathHostMappings = pathHostMappings;
    }

    public List<UrlRewriteRule> getUrlRewriteRules() {
        return urlRewriteRules;
    }

    public void setUrlRewriteRules() {
        this.urlRewriteRules = new ArrayList<>();
        if(mappedConfig.get("urlRewriteRules") != null) {
            if (mappedConfig.get("urlRewriteRules") instanceof String) {
                String s = (String)mappedConfig.get("urlRewriteRules");
                s = s.trim();
                // There are two formats for the urlRewriteRules. One is a string separated by a space
                // and the other is a list of strings separated by a space in JSON list format.
                if(s.startsWith("[")) {
                    // multiple rules
                    List<String> rules = (List<String>) JsonMapper.fromJson(s, List.class);
                    for (String rule : rules) {
                        urlRewriteRules.add(UrlRewriteRule.convertToUrlRewriteRule(rule));
                    }
                } else {
                    // single rule
                    urlRewriteRules.add(UrlRewriteRule.convertToUrlRewriteRule(s));
                }
            } else if (mappedConfig.get("urlRewriteRules") instanceof List) {
                List<String> rules = (List)mappedConfig.get("urlRewriteRules");
                for (String s : rules) {
                    urlRewriteRules.add(UrlRewriteRule.convertToUrlRewriteRule(s));
                }
            }

        }
    }

    public void setUrlRewriteRules(List<UrlRewriteRule> urlRewriteRules) {
        this.urlRewriteRules = urlRewriteRules;
    }

    private void setConfigList() {
        if (mappedConfig.get(PATH_HOST_MAPPINGS) != null) {
            Object object = mappedConfig.get(PATH_HOST_MAPPINGS);
            pathHostMappings = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(s.startsWith("[")) {
                    // multiple path to host mappings
                    List<String> mappings = (List<String>) JsonMapper.fromJson(s, List.class);
                    for (String mapping : mappings) {
                        String[] parts = mapping.split(" ");
                        if(parts.length != 2) {
                            throw new ConfigException("path host entry must have two elements separated by a space.");
                        }
                        pathHostMappings.add(parts);
                    }
                } else {
                    // there is only one path to host available, split the string for path and host.
                    String[] parts = s.split(" ");
                    if(parts.length != 2) {
                        throw new ConfigException("path host entry must have two elements separated by a space.");
                    }
                    pathHostMappings.add(parts);
                }
            } else if (object instanceof List) {
                List<String> maps = (List<String>)object;
                for(String s: maps) {
                    String[] parts = s.split(" ");
                    if(parts.length != 2) {
                        throw new ConfigException("path host entry must have two elements separated by a space.");
                    }
                    pathHostMappings.add(parts);
                }
            } else {
                throw new ConfigException("pathHostMappings must be a string or a list of strings.");
            }
        }
    }

}
