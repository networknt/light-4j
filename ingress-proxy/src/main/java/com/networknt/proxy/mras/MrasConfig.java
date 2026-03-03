package com.networknt.proxy.mras;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import com.networknt.handler.config.UrlRewriteRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Config class for MrasHandler
 *
 * @author Steve Hu
 */
@Deprecated
public class MrasConfig {
    private static final Logger logger = LoggerFactory.getLogger(MrasConfig.class);
    /** Config Name */
    public static final String CONFIG_NAME = "mras";
    /** Enabled */
    public static final String ENABLED = "enabled";

    /** Access Token */
    public static final String ACCESS_TOKEN = "accessToken";

    /** Basic Auth */
    public static final String BASIC_AUTH = "basicAuth";

    /** Anonymous */
    public static final String ANONYMOUS = "anonymous";

    /** Microsoft */
    public static final String MICROSOFT = "microsoft";

    /** Token URL */
    public static final String TOKEN_URL = "tokenUrl";
    /** Username */
    public static final String USERNAME = "username";
    /** Password */
    public static final String PASSWORD = "password";
    /** Client ID */
    public static final String CLIENT_ID = "clientId";
    /** Client Secret */
    public static final String CLIENT_SECRET = "clientSecret";
    /** Resource */
    public static final String RESOURCE = "resource";
    /** Cache Enabled */
    public static final String CACHE_ENABLED = "cacheEnabled";
    /** Mem Key */
    public static final String MEM_KEY = "memKey";
    /** Grace Period */
    public static final String GRACE_PERIOD = "gracePeriod";
    /** Service Host */
    public static final String SERVICE_HOST = "serviceHost";

    /** Key Store Name */
    public static final String KEY_STORE_NAME = "keyStoreName";
    /** Key Store Pass */
    public static final String KEY_STORE_PASS = "keyStorePass";
    /** Key Pass */
    public static final String KEY_PASS = "keyPass";
    /** Trust Store Name */
    public static final String TRUST_STORE_NAME = "trustStoreName";
    /** Trust Store Pass */
    public static final String TRUST_STORE_PASS = "trustStorePass";
    /** Proxy Host */
    public static final String PROXY_HOST = "proxyHost";
    /** Proxy Port */
    public static final String PROXY_PORT = "proxyPort";
    /** Enable HTTP2 */
    public static final String ENABLE_HTTP2 = "enableHttp2";
    /** Path Prefix Auth */
    public static final String PATH_PREFIX_AUTH = "pathPrefixAuth";
    private static final String METRICS_INJECTION = "metricsInjection";
    private static final String METRICS_NAME = "metricsName";
    private static final String CONNECT_TIMEOUT = "connectTimeout";
    private static final String TIMEOUT = "timeout";

    boolean enabled;
    String keyStoreName;
    String keyStorePass;
    String keyPass;
    String trustStoreName;
    String trustStorePass;
    String proxyHost;
    int proxyPort;
    boolean enableHttp2;
    boolean metricsInjection;
    String metricsName;
    int connectTimeout;
    int timeout;

    List<UrlRewriteRule> urlRewriteRules;
    Map<String, Object> pathPrefixAuth;

    Map<String, Object> accessToken;

    Map<String, Object> anonymous;

    Map<String, Object> microsoft;

    Map<String, Object> basicAuth;

    String serviceHost;
    private final Config config;
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
        mappedConfig = config.getJsonMapConfig(configName);
        setConfigData();
        setUrlRewriteRules();
        setConfigMap();
    }

    /**
     * Load config
     * @return MrasConfig
     */
    public static MrasConfig load() {
        return new MrasConfig();
    }

    /**
     * Load config
     * @param configName config name
     * @return MrasConfig
     */
    public static MrasConfig load(String configName) {
        return new MrasConfig(configName);
    }

    /**
     * get mapped config
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
     * get key store name
     * @return String
     */
    public String getKeyStoreName() {
        return keyStoreName;
    }

    /**
     * get key store pass
     * @return String
     */
    public String getKeyStorePass() {
        return keyStorePass;
    }

    /**
     * get key pass
     * @return String
     */
    public String getKeyPass() {
        return keyPass;
    }

    /**
     * get proxy host
     * @return String
     */
    public String getProxyHost() {
        return proxyHost;
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
    public int getTimeout() {
        return timeout;
    }

    /**
     * is enable http2
     * @return boolean
     */
    public boolean isEnableHttp2() {
        return enableHttp2;
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
     * get url rewrite rules
     * @return List
     */
    public List<UrlRewriteRule> getUrlRewriteRules() {
        return urlRewriteRules;
    }

    /**
     * set url rewrite rules
     */
    public void setUrlRewriteRules() {
        this.urlRewriteRules = new ArrayList<>();
        if (mappedConfig.get("urlRewriteRules") != null) {
           if(mappedConfig.get("urlRewriteRules") instanceof String) {
               String s = (String)mappedConfig.get("urlRewriteRules");
               s = s.trim();
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

    /**
     * set url rewrite rules
     * @param urlRewriteRules List
     */
    public void setUrlRewriteRules(List<UrlRewriteRule> urlRewriteRules) {
        this.urlRewriteRules = urlRewriteRules;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if (object != null) enabled = Config.loadBooleanValue(ENABLED, object);
        object = mappedConfig.get(KEY_STORE_NAME);
        if (object != null) keyStoreName = (String)object;
        object = mappedConfig.get(KEY_STORE_PASS);
        if (object != null) keyStorePass = (String)object;
        object = mappedConfig.get(KEY_PASS);
        if (object != null) keyPass = (String)object;
        object = mappedConfig.get(TRUST_STORE_NAME);
        if (object != null) trustStoreName = (String)object;
        object = mappedConfig.get(TRUST_STORE_PASS);
        if (object != null) trustStorePass = (String)object;
        object = mappedConfig.get(PROXY_HOST);
        if (object != null) proxyHost = (String) object;
        object = mappedConfig.get(PROXY_PORT);
        if (object != null) proxyPort = Config.loadIntegerValue(PROXY_PORT, object);
        object = mappedConfig.get(CONNECT_TIMEOUT);
        if (object != null) connectTimeout = Config.loadIntegerValue(CONNECT_TIMEOUT, object);
        object = mappedConfig.get(TIMEOUT);
        if (object != null) timeout = Config.loadIntegerValue(TIMEOUT, object);
        object = mappedConfig.get(ENABLE_HTTP2);
        if (object != null) enableHttp2 = Config.loadBooleanValue(ENABLE_HTTP2, object);
        object = mappedConfig.get(SERVICE_HOST);
        if (object != null) serviceHost = (String) object;
        object = mappedConfig.get(METRICS_INJECTION);
        if(object != null) metricsInjection = Config.loadBooleanValue(METRICS_INJECTION, object);
        object = mappedConfig.get(METRICS_NAME);
        if(object != null ) metricsName = (String)object;
    }

    /**
     * get path prefix auth
     * @return Map
     */
    public Map<String, Object> getPathPrefixAuth() {
        return pathPrefixAuth;
    }

    /**
     * set path prefix auth
     * @param pathPrefixAuth Map
     */
    public void setPathPrefixAuth(Map<String, Object> pathPrefixAuth) {
        this.pathPrefixAuth = pathPrefixAuth;
    }

    /**
     * get access token
     * @return Map
     */
    public Map<String, Object> getAccessToken() {
        return accessToken;
    }

    /**
     * get basic auth
     * @return Map
     */
    public Map<String, Object> getBasicAuth() { return basicAuth; }

    /**
     * get anonymous
     * @return Map
     */
    public Map<String, Object> getAnonymous() { return anonymous; }

    /**
     * get microsoft
     * @return Map
     */
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
