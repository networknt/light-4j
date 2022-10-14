package com.networknt.proxy.mras;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import com.networknt.handler.config.UrlRewriteRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MrasConfig {
    private static final Logger logger = LoggerFactory.getLogger(MrasConfig.class);
    public static final String CONFIG_NAME = "mras";
    public static final String ENABLED = "enabled";

    public static final String ACCESS_TOKEN = "accessToken";

    public static final String BASIC_AUTH = "basicAuth";

    public static final String ANONYMOUS = "anonymous";

    public static final String MICROSOFT = "microsoft";

    public static final String TOKEN_URL = "tokenUrl";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String RESOURCE = "resource";
    public static final String CACHE_ENABLED = "cacheEnabled";
    public static final String MEM_KEY = "memKey";
    public static final String GRACE_PERIOD = "gracePeriod";
    public static final String SERVICE_HOST = "serviceHost";

    public static final String KEY_STORE_NAME = "keyStoreName";
    public static final String KEY_STORE_PASS = "keyStorePass";
    public static final String KEY_PASS = "keyPass";
    public static final String TRUST_STORE_NAME = "trustStoreName";
    public static final String TRUST_STORE_PASS = "trustStorePass";
    public static final String PROXY_HOST = "proxyHost";
    public static final String PROXY_PORT = "proxyPort";
    public static final String ENABLE_HTTP2 = "enableHttp2";
    public static final String PATH_PREFIX_AUTH = "pathPrefixAuth";

    boolean enabled;
    String keyStoreName;
    String keyStorePass;
    String keyPass;
    String trustStoreName;
    String trustStorePass;
    String proxyHost;
    int proxyPort;
    boolean enableHttp2;
    List<UrlRewriteRule> urlRewriteRules;
    Map<String, Object> pathPrefixAuth;

    Map<String, Object> accessToken;

    Map<String, Object> anonymous;

    Map<String, Object> microsoft;

    Map<String, Object> basicAuth;

    String serviceHost;
    private Config config;
    private Map<String, Object> mappedConfig;

    private MrasConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private MrasConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setUrlRewriteRules();
        setConfigMap();
    }

    public static MrasConfig load() {
        return new MrasConfig();
    }

    public static MrasConfig load(String configName) {
        return new MrasConfig(configName);
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setUrlRewriteRules();
        setConfigMap();
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public String getKeyStoreName() {
        return keyStoreName;
    }
    public String getKeyStorePass() {
        return keyStorePass;
    }
    public String getKeyPass() {
        return keyPass;
    }
    public String getProxyHost() {
        return proxyHost;
    }
    public int getProxyPort() {
        return proxyPort;
    }
    public boolean isEnableHttp2() {
        return enableHttp2;
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

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if (object != null && (Boolean) object) {
            enabled = (Boolean)object;
        }
        object = mappedConfig.get(KEY_STORE_NAME);
        if (object != null) {
            keyStoreName = (String)object;
        }
        object = mappedConfig.get(KEY_STORE_PASS);
        if (object != null) {
            keyStorePass = (String)object;
        }
        object = mappedConfig.get(KEY_PASS);
        if (object != null) {
            keyPass = (String)object;
        }
        object = mappedConfig.get(TRUST_STORE_NAME);
        if (object != null) {
            trustStoreName = (String)object;
        }
        object = mappedConfig.get(TRUST_STORE_PASS);
        if (object != null) {
            trustStorePass = (String)object;
        }
        object = mappedConfig.get(PROXY_HOST);
        if (object != null) {
            proxyHost = (String) object;
        }
        object = mappedConfig.get(PROXY_PORT);
        if (object != null) {
            proxyPort = (Integer) object;
        }
        object = mappedConfig.get(ENABLE_HTTP2);
        if (object != null && (Boolean) object) {
            enableHttp2 = (Boolean)object;
        }
        object = mappedConfig.get(SERVICE_HOST);
        if (object != null) {
            serviceHost = (String) object;
        }
    }

    public Map<String, Object> getPathPrefixAuth() {
        return pathPrefixAuth;
    }

    public void setPathPrefixAuth(Map<String, Object> pathPrefixAuth) {
        this.pathPrefixAuth = pathPrefixAuth;
    }

    public Map<String, Object> getAccessToken() {
        return accessToken;
    }
    public Map<String, Object> getBasicAuth() { return basicAuth; }

    public Map<String, Object> getAnonymous() { return anonymous; }

    public Map<String, Object> getMicrosoft() { return microsoft; }

    private void setConfigMap() {
        // path prefix auth mapping
        if (mappedConfig.get(PATH_PREFIX_AUTH) != null) {
            Object object = mappedConfig.get(PATH_PREFIX_AUTH);
            pathPrefixAuth = new HashMap<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("pathPrefixAuth s = " + s);
                if(s.startsWith("{")) {
                    // json format
                    try {
                        pathPrefixAuth = JsonMapper.string2Map(s);
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the pathPrefixAuth json with a map of string and object.");
                    }
                } else {
                    // comma separated
                    String[] pairs = s.split(",");
                    for (int i = 0; i < pairs.length; i++) {
                        String pair = pairs[i];
                        String[] keyValue = pair.split(":");
                        pathPrefixAuth.put(keyValue[0], keyValue[1]);
                    }
                }
            } else if (object instanceof Map) {
                pathPrefixAuth = (Map)object;
            } else {
                throw new ConfigException("pathPrefixAuth must be a string object map.");
            }
        }
        // accessToken map
        if (mappedConfig.get(ACCESS_TOKEN) != null) {
            Object object = mappedConfig.get(ACCESS_TOKEN);
            accessToken = new HashMap<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("accessToken s = " + s);
                if(s.startsWith("{")) {
                    // json format
                    try {
                        accessToken = JsonMapper.string2Map(s);
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the accessToken json with a map of string and object.");
                    }
                } else {
                    // comma separated
                    String[] pairs = s.split(",");
                    for (int i = 0; i < pairs.length; i++) {
                        String pair = pairs[i];
                        String[] keyValue = pair.split(":");
                        accessToken.put(keyValue[0], keyValue[1]);
                    }
                }
            } else if (object instanceof Map) {
                accessToken = (Map)object;
            } else {
                throw new ConfigException("accessToken must be a string object map.");
            }
        }
        // anonymous
        if (mappedConfig.get(ANONYMOUS) != null) {
            Object object = mappedConfig.get(ANONYMOUS);
            anonymous = new HashMap<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("anonymous string = " + s);
                if(s.startsWith("{")) {
                    // json format
                    try {
                        anonymous = JsonMapper.string2Map(s);
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the anonymous json with a map of string and object.");
                    }
                } else {
                    // comma separated
                    String[] pairs = s.split(",");
                    for (int i = 0; i < pairs.length; i++) {
                        String pair = pairs[i];
                        String[] keyValue = pair.split(":");
                        anonymous.put(keyValue[0], keyValue[1]);
                    }
                }
            } else if (object instanceof Map) {
                anonymous = (Map)object;
            } else {
                throw new ConfigException("anonymous must be a string object map.");
            }
        }

        // basicAuth
        if (mappedConfig.get(BASIC_AUTH) != null) {
            Object object = mappedConfig.get(BASIC_AUTH);
            basicAuth = new HashMap<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("basicAuth string = " + s);
                if(s.startsWith("{")) {
                    // json format
                    try {
                        basicAuth = JsonMapper.string2Map(s);
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the basicAuth json with a map of string and object.");
                    }
                } else {
                    // comma separated
                    String[] pairs = s.split(",");
                    for (int i = 0; i < pairs.length; i++) {
                        String pair = pairs[i];
                        String[] keyValue = pair.split(":");
                        basicAuth.put(keyValue[0], keyValue[1]);
                    }
                }
            } else if (object instanceof Map) {
                basicAuth = (Map)object;
            } else {
                throw new ConfigException("basicAuth must be a string object map.");
            }
        }

        // Microsoft
        if (mappedConfig.get(MICROSOFT) != null) {
            Object object = mappedConfig.get(MICROSOFT);
            microsoft = new HashMap<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("microsoft string = " + s);
                if(s.startsWith("{")) {
                    // json format
                    try {
                        microsoft = JsonMapper.string2Map(s);
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the microsoft json with a map of string and object.");
                    }
                } else {
                    // comma separated
                    String[] pairs = s.split(",");
                    for (int i = 0; i < pairs.length; i++) {
                        String pair = pairs[i];
                        String[] keyValue = pair.split(":");
                        microsoft.put(keyValue[0], keyValue[1]);
                    }
                }
            } else if (object instanceof Map) {
                microsoft = (Map)object;
            } else {
                throw new ConfigException("microsoft must be a string object map.");
            }
        }

    }

}
