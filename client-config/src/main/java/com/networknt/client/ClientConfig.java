package com.networknt.client;

import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


public final class ClientConfig {
    private static final Logger logger = LoggerFactory.getLogger(ClientConfig.class);
    public static final String CONFIG_NAME = "client";
    public static final String REQUEST = "request";
    public static final String SERVER_URL = "server_url";
    public static final String PROXY_HOST = "proxyHost";
    public static final String PROXY_PORT = "proxyPort";
    public static final String SERVICE_ID = "serviceId";
    public static final String URI = "uri";
    public static final String TLS = "tls";
    public static final String CLIENT_ID = "client_id";
    public static final String SCOPE = "scope";
    public static final String AUDIENCE = "audience";
    public static final String CSRF = "csrf";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String SAML_BEARER = "saml_bearer";
    public static final String CLIENT_CREDENTIALS = "client_credentials";
    public static final String AUTHORIZATION_CODE = "authorization_code";
    public static final String CLIENT_AUTHENTICATED_USER = "client_authenticated_user";
    public static final String CACHE = "cache";
    public static final String CAPACITY = "capacity";
    public static final String OAUTH = "oauth";
    public static final String MULTIPLE_AUTH_SERVERS = "multipleAuthServers";
    public static final String PATH_PREFIX_SERVICES = "pathPrefixServices";
    public static final String SERVICE_ID_AUTH_SERVERS = "serviceIdAuthServers";
    public static final String KEY = "key";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String ENABLE_HTTP2 = "enableHttp2";
    public static final String TIMEOUT = "timeout";
    public static final String TOKEN = "token";
    public static final int DEFAULT_BUFFER_SIZE = 24; // 24*1024 buffer size will be good for most of the app.
    public static final int DEFAULT_ERROR_THRESHOLD = 5;
    public static final int DEFAULT_TIMEOUT = 3000;
    public static final int DEFAULT_RESET_TIMEOUT = 600000;
    public static final boolean DEFAULT_INJECT_OPEN_TRACING = false;
    public static final boolean DEFAULT_INJECT_CALLER_ID = false;
    private static final String ERROR_THRESHOLD = "errorThreshold";
    private static final String RESET_TIMEOUT = "resetTimeout";
    private static final String INJECT_OPEN_TRACING = "injectOpenTracing";
    private static final String INJECT_CALLER_ID = "injectCallerId";
    public static final int DEFAULT_CONNECTION_POOL_SIZE = 1000;
    public static final boolean DEFAULT_REQUEST_ENABLE_HTTP2 = true;
    public static final int DEFAULT_MAX_REQUEST_PER_CONNECTION = 1000000;
    public static final long DEFAULT_CONNECTION_EXPIRE_TIME = 1000000;
    public static final int DEFAULT_MAX_CONNECTION_PER_HOST = 1000;
    public static final int DEFAULT_MIN_CONNECTION_PER_HOST = 250;
    public static final String TOKEN_RENEW_BEFORE_EXPIRED = "tokenRenewBeforeExpired";
    public static final String EXPIRED_REFRESH_RETRY_DELAY = "expiredRefreshRetryDelay";
    public static final String EARLY_REFRESH_RETRY_DELAY = "earlyRefreshRetryDelay";
    private static final String CONNECTION_POOL_SIZE = "connectionPoolSize";
    private static final String MAX_REQUEST_PER_CONNECTION = "maxReqPerConn";
    private static final String CONNECTION_EXPIRE_TIME = "connectionExpireTime";
    private static final String MAX_CONNECTION_NUM_PER_HOST = "maxConnectionNumPerHost";
    private static final String MIN_CONNECTION_NUM_PER_HOST = "minConnectionNumPerHost";
    public static final String DEREF = "deref";
    public static final String SIGN = "sign";
    private int resetTimeout = DEFAULT_RESET_TIMEOUT;
    private int timeout = DEFAULT_TIMEOUT;
    private int errorThreshold = DEFAULT_ERROR_THRESHOLD;
    private boolean injectOpenTracing = DEFAULT_INJECT_OPEN_TRACING;
    private boolean injectCallerId = DEFAULT_INJECT_CALLER_ID;
    private int connectionPoolSize = DEFAULT_CONNECTION_POOL_SIZE;
    private int maxReqPerConn = DEFAULT_MAX_REQUEST_PER_CONNECTION;
    private boolean requestEnableHttp2 = DEFAULT_REQUEST_ENABLE_HTTP2;
    private long connectionExpireTime = DEFAULT_CONNECTION_EXPIRE_TIME;
    private int maxConnectionNumPerHost = DEFAULT_MAX_CONNECTION_PER_HOST;
    private int minConnectionNumPerHost = DEFAULT_MIN_CONNECTION_PER_HOST;

    private final Config config;
    private Map<String, Object> mappedConfig;
    private Map<String, Object> tlsConfig;
    private boolean multipleAuthServers;
    private Map<String, Object> oauthConfig;
    private Map<String, Object> tokenConfig;
    private Map<String, Object> derefConfig;
    private Map<String, Object> signConfig;
    private Map<String, String> pathPrefixServices;
    private static volatile ClientConfig instance;

    private ClientConfig() {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(CONFIG_NAME);
        load();
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private ClientConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        load();
    }


    private void load() {
        if(mappedConfig != null) {
            setRequestConfig();
            setTlsConfig();
            setOAuthConfig();
            if(multipleAuthServers) {
                setPathPrefixServices();
            }
        }
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        if(mappedConfig != null) {
            setRequestConfig();
            setTlsConfig();
            setOAuthConfig();
            if(multipleAuthServers) {
                setPathPrefixServices();
            }
        }
    }

    public static ClientConfig get() {
        if (instance == null) {
            synchronized (ClientConfig.class) {
                if (instance == null) {
                    instance = new ClientConfig();
                }
            }
        }
        return instance;
    }

    /**
     * This method is not supposed to be used in production but only in testing.
     * @param configName String
     * @return ClientConfig object
     */
    public static ClientConfig get(String configName) {
        instance = new ClientConfig(configName);
        return instance;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setTlsConfig() {
        tlsConfig = (Map<String, Object>)mappedConfig.get(TLS);
    }

    public Map<String, Object> getTlsConfig() {
        return tlsConfig;
    }

    private void setRequestConfig() {
        if (!mappedConfig.containsKey(REQUEST)) {
            return;
        }
        Map<String, Object> requestConfig = (Map<String, Object>) mappedConfig.get(REQUEST);
        Object object = requestConfig.get(ERROR_THRESHOLD);
        if(object != null) errorThreshold = Config.loadIntegerValue(ERROR_THRESHOLD, object);
        object = requestConfig.get(TIMEOUT);
        if(object != null) timeout = Config.loadIntegerValue(TIMEOUT, object);
        object = requestConfig.get(RESET_TIMEOUT);
        if(object != null) resetTimeout = Config.loadIntegerValue(RESET_TIMEOUT, object);
        object = requestConfig.get(INJECT_OPEN_TRACING);
        if(object != null) injectOpenTracing = Config.loadBooleanValue(INJECT_OPEN_TRACING, object);
        object = requestConfig.get(INJECT_CALLER_ID);
        if(object != null) injectCallerId = Config.loadBooleanValue(INJECT_CALLER_ID, object);
        object = requestConfig.get(ENABLE_HTTP2);
        if(object != null) requestEnableHttp2 = Config.loadBooleanValue(ENABLE_HTTP2, object);
        object = requestConfig.get(CONNECTION_POOL_SIZE);
        if(object != null) connectionPoolSize = Config.loadIntegerValue(CONNECTION_POOL_SIZE, object);
        object = requestConfig.get(CONNECTION_EXPIRE_TIME);
        if(object != null) connectionExpireTime = Config.loadLongValue(CONNECTION_EXPIRE_TIME, object);
        object = requestConfig.get(MAX_REQUEST_PER_CONNECTION);
        if(object != null) maxReqPerConn = Config.loadIntegerValue(MAX_REQUEST_PER_CONNECTION, object);
        object = requestConfig.get(MAX_CONNECTION_NUM_PER_HOST);
        if(object != null) maxConnectionNumPerHost = Config.loadIntegerValue(MAX_CONNECTION_NUM_PER_HOST, object);
        object = requestConfig.get(MIN_CONNECTION_NUM_PER_HOST);
        if(object != null) minConnectionNumPerHost = Config.loadIntegerValue(MIN_CONNECTION_NUM_PER_HOST, object);
    }

    private void setPathPrefixServices() {
        if (mappedConfig.get(PATH_PREFIX_SERVICES) != null && mappedConfig.get(PATH_PREFIX_SERVICES) instanceof Map) {
            pathPrefixServices = (Map)mappedConfig.get(PATH_PREFIX_SERVICES);
        }
    }
    public Map<String, String> getPathPrefixServices() { return pathPrefixServices; }
    public static Map<String, Object> getServiceIdAuthServers(Object object) {
        Map<String, Object> serviceIdAuthServers = new HashMap<>();
        if (object instanceof Map) {
            serviceIdAuthServers = (Map) object;
        } else if (object instanceof String) {
            String s = (String) object;
            s = s.trim();
            if (s.startsWith("{")) {
                serviceIdAuthServers = JsonMapper.string2Map(s);
            } else {
                logger.error("The serviceIdAuthServers in client.yml is not a map or a JSON string.");
            }
        } else {
            logger.error("The serviceIdAuthServers in client.yml is not a map or a JSON string.");
        }
        return serviceIdAuthServers;
    }

    private void setOAuthConfig() {
        oauthConfig = (Map<String, Object>)mappedConfig.get(OAUTH);
        if (oauthConfig != null) {
            tokenConfig = (Map<String, Object>)oauthConfig.get(TOKEN);
            derefConfig = (Map<String, Object>)oauthConfig.get(DEREF);
            signConfig = (Map<String, Object>)oauthConfig.get(SIGN);
            if(oauthConfig.get(MULTIPLE_AUTH_SERVERS) != null) {
                multipleAuthServers = Config.loadBooleanValue(MULTIPLE_AUTH_SERVERS, oauthConfig.get(MULTIPLE_AUTH_SERVERS));
            } else {
                multipleAuthServers = false;
            }
        }
    }
    public Map<String, Object> getOauthConfig() {
        return oauthConfig;
    }
    public Map<String, Object> getTokenConfig() {
        return tokenConfig;
    }

    public Map<String, Object> getDerefConfig() {
        return derefConfig;
    }

    public Map<String, Object> getSignConfig() {
        return signConfig;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getResetTimeout() {
        return resetTimeout;
    }

    public int getErrorThreshold() {
        return errorThreshold;
    }

    public boolean isInjectOpenTracing() { return injectOpenTracing; }

    public boolean isInjectCallerId() {
        return injectCallerId;
    }

    public int getConnectionPoolSize() {
        return connectionPoolSize;
    }

    public int getMaxRequestPerConnection() {
        return maxReqPerConn;
    }

    public boolean getRequestEnableHttp2() {
        return requestEnableHttp2;
    }
    public void setRequestEnableHttp2(boolean requestEnableHttp2) {
        this.requestEnableHttp2 = requestEnableHttp2;
    }

    public long getConnectionExpireTime() {
        return connectionExpireTime;
    }

    public int getMaxConnectionNumPerHost() {
        return maxConnectionNumPerHost;
    }

    public int getMinConnectionNumPerHost() {
        return minConnectionNumPerHost;
    }

    public boolean isMultipleAuthServers() { return multipleAuthServers; }

    public int getBufferSize() {
        return DEFAULT_BUFFER_SIZE;
    }
}
