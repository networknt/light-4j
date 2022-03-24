package com.networknt.client;

import com.networknt.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public final class ClientConfig {

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
    public static final String DEREF = "deref";
    public static final String SIGN = "sign";
    public static final String ENABLE_HTTP2 = "enableHttp2";
    public static final String TIMEOUT = "timeout";
    public static final String TOKEN = "token";
    public static final String TOKEN_RENEW_BEFORE_EXPIRED = "tokenRenewBeforeExpired";
    public static final String EXPIRED_REFRESH_RETRY_DELAY = "expiredRefreshRetryDelay";
    public static final String EARLY_REFRESH_RETRY_DELAY = "earlyRefreshRetryDelay";
    public static final int DEFAULT_BUFFER_SIZE = 24; // 24*1024 buffer size will be good for most of the app.
    public static final int DEFAULT_ERROR_THRESHOLD = 5;
    public static final int DEFAULT_TIMEOUT = 3000;
    public static final int DEFAULT_RESET_TIMEOUT = 600000;
    public static final boolean DEFAULT_INJECT_OPEN_TRACING = false;
    public static final boolean DEFAULT_INJECT_CALLER_ID = false;
    private static final String BUFFER_SIZE = "bufferSize";
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
    public static final String DEFAULT_TRANSFER_ENCODING = "chunked";

    private static final String CONNECTION_POOL_SIZE = "connectionPoolSize";
    private static final String MAX_REQUEST_PER_CONNECTION = "maxReqPerConn";
    private static final String CONNECTION_EXPIRE_TIME = "connectionExpireTime";
    private static final String MAX_CONNECTION_NUM_PER_HOST = "maxConnectionNumPerHost";
    private static final String MIN_CONNECTION_NUM_PER_HOST = "minConnectionNumPerHost";
    private static final String TRANSFER_ENCODING = "transferEncoding";

    private final Config config;
    private final Map<String, Object> mappedConfig;

    private Map<String, Object> tlsConfig;
    private boolean multipleAuthServers;
    private Map<String, Object> oauthConfig;
    private Map<String, Object> tokenConfig;
    private Map<String, Object> derefConfig;
    private Map<String, Object> signConfig;
    private Map<String, String> pathPrefixServices;
    private int bufferSize = DEFAULT_BUFFER_SIZE;
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
    private String transferEncoding = DEFAULT_TRANSFER_ENCODING;

    private static ClientConfig instance;

    private void load() {
        if (mappedConfig != null) {
            setTlsConfig();
            setBufferSize();
            setOAuthConfig();
            setTokenConfig();
            setRequestConfig();
            setDerefConfig();
            setSignConfig();
            if(multipleAuthServers) {
                setPathPrefixServices();
            }
        }
    }
    private ClientConfig() {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(CONFIG_NAME);
        load();
    }

    /**
     * This method is not supposed to be used in production but only in testing. No cache method is used
     * to make sure that multiple client.yml files are not mixed together.
     * @param configName
     */
    @Deprecated
    private ClientConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        System.err.println("reload with " + configName);
        load();
    }

    public static ClientConfig get() {
        if (instance == null) {
            instance = new ClientConfig();
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

    /**
     * For testing purpose
     */
    static void reset() {
        instance = null;
    }

    private void setRequestConfig() {
        if (!mappedConfig.containsKey(REQUEST)) {
            return;
        }
        Map<String, Object> requestConfig = (Map<String, Object>) mappedConfig.get(REQUEST);

        if (requestConfig.containsKey(RESET_TIMEOUT)) {
            resetTimeout = (int) requestConfig.get(RESET_TIMEOUT);
        }
        if (requestConfig.containsKey(ERROR_THRESHOLD)) {
            errorThreshold = (int) requestConfig.get(ERROR_THRESHOLD);
        }
        if (requestConfig.containsKey(TIMEOUT)) {
            timeout = (int) requestConfig.get(TIMEOUT);
        }
        if(requestConfig.containsKey(INJECT_OPEN_TRACING)) {
            injectOpenTracing = (Boolean) requestConfig.get(INJECT_OPEN_TRACING);
        }
        if(requestConfig.containsKey(INJECT_CALLER_ID)) {
            injectCallerId = (Boolean) requestConfig.get(INJECT_CALLER_ID);
        }
        if (requestConfig.containsKey(CONNECTION_POOL_SIZE)) {
            connectionPoolSize = (int) requestConfig.get(CONNECTION_POOL_SIZE);
        }
        if (requestConfig.containsKey(ENABLE_HTTP2)) {
            requestEnableHttp2 = (boolean) requestConfig.get(ENABLE_HTTP2);
        }
        if (requestConfig.containsKey(MAX_REQUEST_PER_CONNECTION)) {
            maxReqPerConn = (int) requestConfig.get(MAX_REQUEST_PER_CONNECTION);
        }
        if (requestConfig.containsKey(CONNECTION_EXPIRE_TIME)) {
            connectionExpireTime = Long.parseLong(requestConfig.get(CONNECTION_EXPIRE_TIME).toString());
        }
        if (requestConfig.containsKey(MAX_CONNECTION_NUM_PER_HOST)) {
            maxConnectionNumPerHost = (int) requestConfig.get(MAX_CONNECTION_NUM_PER_HOST);
        }
        if (requestConfig.containsKey(MIN_CONNECTION_NUM_PER_HOST)) {
            minConnectionNumPerHost = (int) requestConfig.get(MIN_CONNECTION_NUM_PER_HOST);
        }
        if(requestConfig.containsKey(TRANSFER_ENCODING)) {
            transferEncoding = (String)requestConfig.get(TRANSFER_ENCODING);
        }
    }

    private void setBufferSize() {
        Object bufferSizeObject = mappedConfig.get(BUFFER_SIZE);
        if (bufferSizeObject != null) {
            bufferSize = (int) bufferSizeObject;
        }
    }

    private void setTlsConfig() {
        tlsConfig = (Map<String, Object>)mappedConfig.get(TLS);
    }

    private void setOAuthConfig() {
        oauthConfig = (Map<String, Object>)mappedConfig.get(OAUTH);
        if (oauthConfig != null && oauthConfig.get(MULTIPLE_AUTH_SERVERS) != null) {
            multipleAuthServers = (Boolean)oauthConfig.get(MULTIPLE_AUTH_SERVERS);
        } else {
            multipleAuthServers = false;
        }
    }

    private void setTokenConfig() {
        if (oauthConfig != null) {
            tokenConfig = (Map<String, Object>)oauthConfig.get(TOKEN);
        }
    }

    private void setDerefConfig() {
        if (oauthConfig != null) {
            derefConfig = (Map<String, Object>)oauthConfig.get(DEREF);
        }
    }

    private void setSignConfig() {
        if (oauthConfig != null) {
            signConfig = (Map<String, Object>)oauthConfig.get(SIGN);
        }
    }

    private void setPathPrefixServices() {
        if (mappedConfig.get(PATH_PREFIX_SERVICES) != null && mappedConfig.get(PATH_PREFIX_SERVICES) instanceof Map) {
            pathPrefixServices = (Map)mappedConfig.get(PATH_PREFIX_SERVICES);
        }
    }

    public Map<String, Object> getTlsConfig() { return tlsConfig; }

    public Map<String, String> getPathPrefixServices() { return pathPrefixServices; }

    public int getBufferSize() {
        return bufferSize;
    }

    public Config getConfig() {
        return config;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
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

    public boolean getRequestEnableHttp2() {
        return requestEnableHttp2;
    }

    public int getMaxRequestPerConnection() {
        return maxReqPerConn;
    }

    protected void setRequestEnableHttp2(boolean isRequestEnableHttp2) {
        this.requestEnableHttp2 = isRequestEnableHttp2;
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

    public String getTransferEncoding() { return transferEncoding; }
}
