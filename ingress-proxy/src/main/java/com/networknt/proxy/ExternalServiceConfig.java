package com.networknt.proxy;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.handler.config.UrlRewriteRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternalServiceConfig {
    public static final String CONFIG_NAME = "external-service";
    private static final String ENABLED = "enabled";
    private static final String PROXY_HOST = "proxyHost";
    private static final String PROXY_PORT = "proxyPort";
    private static final String ENABLE_HTTP2 = "enableHttp2";
    private static final String PATH_HOST_MAPPINGS = "pathHostMappings";

    boolean enabled;
    String proxyHost;
    int proxyPort;
    boolean enableHttp2;
    List<String[]> pathHostMappings;

    List<UrlRewriteRule> urlRewriteRules;
    private Config config;
    private Map<String, Object> mappedConfig;

    public ExternalServiceConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    protected ExternalServiceConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setUrlRewriteRules();
        setConfigList();
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setConfigList();
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

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public void setEnableHttp2(boolean enableHttp2) {
        this.enableHttp2 = enableHttp2;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if (object != null && (Boolean) object) {
            setEnabled((Boolean)object);
        }
        object = mappedConfig.get(PROXY_HOST);
        if (object != null) {
            setProxyHost((String) object);
        }
        object = mappedConfig.get(PROXY_PORT);
        if (object != null) {
            setProxyPort((int) object);
        }
        object = mappedConfig.get(ENABLE_HTTP2);
        if (object != null && (Boolean) object) {
            setEnableHttp2((Boolean)object);
        }
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
        if (mappedConfig.get("urlRewriteRules") !=null && mappedConfig.get("urlRewriteRules") instanceof String) {
            urlRewriteRules.add(UrlRewriteRule.convertToUrlRewriteRule((String)mappedConfig.get("urlRewriteRules")));
        } else {
            List<String> rules = (List)mappedConfig.get("urlRewriteRules");
            if(rules != null) {
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
                // there is only one path to host available, split the string for path and host.
                String[] parts = ((String)object).split(" ");
                if(parts.length != 2) {
                    throw new ConfigException("path host entry must have two elements separated by a space.");
                }
                pathHostMappings.add(parts);
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
