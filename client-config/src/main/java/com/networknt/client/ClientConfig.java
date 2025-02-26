package com.networknt.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.config.MapLoadable;
import com.networknt.config.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


@ConfigSchema(configKey = "client", configName = "client", outputFormats = {OutputFormat.DEBUG, OutputFormat.JSON_SCHEMA, OutputFormat.YAML})
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
//    public static final String TIMEOUT = "timeout";
//    public static final String MAX_REQUEST_RETRY = "maxRequestRetry";
//    public static final String REQUEST_RETRY_DELAY = "requestRetryDelay";
    public static final String TOKEN = "token";
    public static final int DEFAULT_BUFFER_SIZE = 24; // 24*1024 buffer size will be good for most of the app.
    public static final int DEFAULT_ERROR_THRESHOLD = 5;
    public static final int DEFAULT_TIMEOUT = 3000;
    public static final int DEFAULT_RESET_TIMEOUT = 600000;
//    public static final int DEFAULT_MAX_REQUEST_RETRY = 3;
//    public static final int DEFAULT_REQUEST_RETRY_DELAY = 1000;
//    public static final boolean DEFAULT_INJECT_OPEN_TRACING = false;
//    public static final boolean DEFAULT_INJECT_CALLER_ID = false;
//    private static final String ERROR_THRESHOLD = "errorThreshold";
//    private static final String RESET_TIMEOUT = "resetTimeout";
//    private static final String INJECT_OPEN_TRACING = "injectOpenTracing";
//    private static final String INJECT_CALLER_ID = "injectCallerId";
//    public static final int DEFAULT_CONNECTION_POOL_SIZE = 1000;
//    public static final boolean DEFAULT_REQUEST_ENABLE_HTTP2 = true;
//    public static final int DEFAULT_MAX_REQUEST_PER_CONNECTION = 1000000;
//    public static final long DEFAULT_CONNECTION_EXPIRE_TIME = 1000000;
//    public static final int DEFAULT_MAX_CONNECTION_PER_HOST = 1000;
//    public static final int DEFAULT_MIN_CONNECTION_PER_HOST = 250;
    public static final String TOKEN_RENEW_BEFORE_EXPIRED = "tokenRenewBeforeExpired";
    public static final String EXPIRED_REFRESH_RETRY_DELAY = "expiredRefreshRetryDelay";
    public static final String EARLY_REFRESH_RETRY_DELAY = "earlyRefreshRetryDelay";
//    private static final String CONNECTION_POOL_SIZE = "connectionPoolSize";
//    private static final String MAX_REQUEST_PER_CONNECTION = "maxReqPerConn";
//    private static final String CONNECTION_EXPIRE_TIME = "connectionExpireTime";
//    private static final String MAX_CONNECTION_NUM_PER_HOST = "maxConnectionNumPerHost";
//    private static final String MIN_CONNECTION_NUM_PER_HOST = "minConnectionNumPerHost";
//    public static final String DEREF = "deref";
//    public static final String SIGN = "sign";
//    private int resetTimeout = DEFAULT_RESET_TIMEOUT;
//    private int timeout = DEFAULT_TIMEOUT;
//    private int maxRequestRetry = DEFAULT_MAX_REQUEST_RETRY;
//    private int requestRetryDelay = DEFAULT_REQUEST_RETRY_DELAY;
//    private int errorThreshold = DEFAULT_ERROR_THRESHOLD;
//    private boolean injectOpenTracing = DEFAULT_INJECT_OPEN_TRACING;
//    private boolean injectCallerId = DEFAULT_INJECT_CALLER_ID;
//    private int connectionPoolSize = DEFAULT_CONNECTION_POOL_SIZE;
//    private int maxReqPerConn = DEFAULT_MAX_REQUEST_PER_CONNECTION;
//    private boolean requestEnableHttp2 = DEFAULT_REQUEST_ENABLE_HTTP2;
//    private long connectionExpireTime = DEFAULT_CONNECTION_EXPIRE_TIME;
//    private int maxConnectionNumPerHost = DEFAULT_MAX_CONNECTION_PER_HOST;
//    private int minConnectionNumPerHost = DEFAULT_MIN_CONNECTION_PER_HOST;

    private final Config config;
    private Map<String, Object> mappedConfig;

    @ObjectField(
            configFieldName = "tls",
            useSubObjectDefault = true,
            ref = TlsConfig.class,
            description = "Settings for TLS"
    )
    private TlsConfig tls;

    @ObjectField(
            configFieldName = "oauth",
            useSubObjectDefault = true,
            ref = OAuthConfig.class,
            description = "Settings for OAuth2 server communication."
    )
    private OAuthConfig oauthConfig;

    @MapField(
            configFieldName = "pathPrefixServices",
            externalizedKeyName = "pathPrefixServices",
            valueType = String.class,
            externalized = true,
            description = "If you have multiple OAuth 2.0 providers and use path prefix to decide which OAuth 2.0 server\n" +
                    "to get the token or JWK. If two or more services have the same path, you must use serviceId in\n" +
                    "the request header to use the serviceId to find the OAuth 2.0 provider configuration."
    )
    private Map<String, String> pathPrefixServices;

    @ObjectField(
            configFieldName = "request",
            useSubObjectDefault = true,
            ref = RequestConfig.class,
            description = "Circuit breaker configuration for the client"
    )
    private RequestConfig request;
//    private boolean multipleAuthServers;
//    private Map<String, Object> oauthConfigMap;
//    private Map<String, Object> tlsConfigMap;
//    private Map<String, Object> tokenConfigMap;
//    private Map<String, Object> derefConfigMap;
//    private Map<String, Object> signConfigMap;

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
//            setRequestConfig();
//            setTlsConfig();
//            setOAuthConfig();
//            if(multipleAuthServers) {
//                setPathPrefixServices();
//            }
            setValues();
        }
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        if(mappedConfig != null) {
//            setRequestConfig();
//            setTlsConfig();
//            setOAuthConfig();
//            if(multipleAuthServers) {
//                setPathPrefixServices();
//            }
            setValues();
        }
    }

    private void setValues() {
        final var mapper = Config.getInstance().getMapper();
        if (mappedConfig.get(REQUEST) instanceof Map)
            this.request = mapper.convertValue(mappedConfig.get(REQUEST), RequestConfig.class);
        if (mappedConfig.get(TLS) instanceof Map)
            this.tls = mapper.convertValue(mappedConfig.get(TLS), TlsConfig.class);
        if (mappedConfig.get(OAUTH) instanceof Map)
            this.oauthConfig = mapper.convertValue(mappedConfig.get(OAUTH), OAuthConfig.class);

        if (mappedConfig.get(PATH_PREFIX_SERVICES) != null && mappedConfig.get(PATH_PREFIX_SERVICES) instanceof Map) {
            this.pathPrefixServices = (Map)mappedConfig.get(PATH_PREFIX_SERVICES);
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

//    private void setRequestConfig() {
//        if (!mappedConfig.containsKey(REQUEST)) {
//            return;
//        }
//        Map<String, Object> requestConfig = (Map<String, Object>) mappedConfig.get(REQUEST);
//        Object object = requestConfig.get(ERROR_THRESHOLD);
//        if(object != null) errorThreshold = Config.loadIntegerValue(ERROR_THRESHOLD, object);
//        object = requestConfig.get(TIMEOUT);
//        if(object != null) timeout = Config.loadIntegerValue(TIMEOUT, object);
//        object = requestConfig.get(RESET_TIMEOUT);
//        if(object != null) resetTimeout = Config.loadIntegerValue(RESET_TIMEOUT, object);
//        object = requestConfig.get(MAX_REQUEST_RETRY);
//        if(object != null) maxRequestRetry = Config.loadIntegerValue(MAX_REQUEST_RETRY, object);
//        object = requestConfig.get(REQUEST_RETRY_DELAY);
//        if(object != null) requestRetryDelay = Config.loadIntegerValue(REQUEST_RETRY_DELAY, object);
//        object = requestConfig.get(INJECT_OPEN_TRACING);
//        if(object != null) injectOpenTracing = Config.loadBooleanValue(INJECT_OPEN_TRACING, object);
//        object = requestConfig.get(INJECT_CALLER_ID);
//        if(object != null) injectCallerId = Config.loadBooleanValue(INJECT_CALLER_ID, object);
//        object = requestConfig.get(ENABLE_HTTP2);
//        if(object != null) requestEnableHttp2 = Config.loadBooleanValue(ENABLE_HTTP2, object);
//        object = requestConfig.get(CONNECTION_POOL_SIZE);
//        if(object != null) connectionPoolSize = Config.loadIntegerValue(CONNECTION_POOL_SIZE, object);
//        object = requestConfig.get(CONNECTION_EXPIRE_TIME);
//        if(object != null) connectionExpireTime = Config.loadLongValue(CONNECTION_EXPIRE_TIME, object);
//        object = requestConfig.get(MAX_REQUEST_PER_CONNECTION);
//        if(object != null) maxReqPerConn = Config.loadIntegerValue(MAX_REQUEST_PER_CONNECTION, object);
//        object = requestConfig.get(MAX_CONNECTION_NUM_PER_HOST);
//        if(object != null) maxConnectionNumPerHost = Config.loadIntegerValue(MAX_CONNECTION_NUM_PER_HOST, object);
//        object = requestConfig.get(MIN_CONNECTION_NUM_PER_HOST);
//        if(object != null) minConnectionNumPerHost = Config.loadIntegerValue(MIN_CONNECTION_NUM_PER_HOST, object);
//    }

//    private void setPathPrefixServices() {
//        if (mappedConfig.get(PATH_PREFIX_SERVICES) != null && mappedConfig.get(PATH_PREFIX_SERVICES) instanceof Map) {
//            pathPrefixServices = (Map)mappedConfig.get(PATH_PREFIX_SERVICES);
//        }
//    }

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

//    private void setOAuthConfig() {
//        oauthConfigMap = (Map<String, Object>)mappedConfig.get(OAUTH);
//        if (oauthConfigMap != null) {
//            tokenConfigMap = (Map<String, Object>)oauthConfigMap.get(TOKEN);
//            derefConfigMap = (Map<String, Object>)oauthConfigMap.get(DEREF);
//            signConfigMap = (Map<String, Object>)oauthConfigMap.get(SIGN);
//            if(oauthConfigMap.get(MULTIPLE_AUTH_SERVERS) != null) {
//                multipleAuthServers = Config.loadBooleanValue(MULTIPLE_AUTH_SERVERS, oauthConfigMap.get(MULTIPLE_AUTH_SERVERS));
//            } else {
//                multipleAuthServers = false;
//            }
//        }
//    }

    public OAuthConfig getOAuth() {
        return this.oauthConfig;
    }

    public RequestConfig getRequest() {
        return this.request;
    }

    public Map<String, String> getPathPrefixServices() { return pathPrefixServices; }

//    @Deprecated(since = "2.2.1")
//    private void setTlsConfig() {
//        final var mappedTls = (Map<String, Object>)mappedConfig.get(TLS);
//        this.tlsConfigMap = mappedTls;
//    }

    @Deprecated(since = "2.2.1")
    public Map<String, Object> getTlsConfig() {
        final var mapper = Config.getInstance().getMapper();
        return mapper.convertValue(tls, new TypeReference<>() {});
    }

    /**
     * @deprecated since 2.2.1 - Should use the POJO instead of a hashmap.
     * @return oauth config hashmap
     */
    @Deprecated(since = "2.2.1")
    public Map<String, Object> getOauthConfig() {
        final var mapper = Config.getInstance().getMapper();
        return mapper.convertValue(oauthConfig, new TypeReference<>() {});
    }


    /**
     * @deprecated since 2.2.1 - Should use the POJO instead of a hashmap.
     * @return token config hashmap
     */
    @Deprecated(since = "2.2.1")
    public Map<String, Object> getTokenConfig() {
        final var mapper = Config.getInstance().getMapper();
        return mapper.convertValue(oauthConfig.getToken(), new TypeReference<>() {});
    }

    /**
     * @deprecated since 2.2.1 - Should use the POJO instead of a hashmap.
     * @return deref config hashmap
     */
    @Deprecated(since = "2.2.1")
    public Map<String, Object> getDerefConfig() {
        final var mapper = Config.getInstance().getMapper();
        return mapper.convertValue(oauthConfig.getDeref(), new TypeReference<>() {});
    }

    /**
     * @deprecated since 2.2.1 - Should use the POJO instead of a hashmap.
     * @return sign config hashmap
     */
    @Deprecated(since = "2.2.1")
    public Map<String, Object> getSignConfig() {
        final var mapper = Config.getInstance().getMapper();
        return mapper.convertValue(oauthConfig.getSign(), new TypeReference<>() {});
    }

    /**
     * @deprecated since 2.2.1 - should get this through {@link #getRequest()} instead.
     * @return request timeout
     */
    @Deprecated(since = "2.2.1")
    public int getTimeout() {
        return this.request.getTimeout();
    }

    /**
     * @deprecated since 2.2.1 - should get this through {@link #getRequest()} instead.
     * @return reset timeout
     */
    @Deprecated(since = "2.2.1")
    public int getResetTimeout() {
        return this.request.getResetTimeout();
    }

    /**
     * @deprecated since 2.2.1 - should get this through {@link #getRequest()} instead.
     * @return max request retry
     */
    @Deprecated(since = "2.2.1")
    public int getMaxRequestRetry() {
        return this.request.getMaxRequestRetry();
    }

    /**
     * @deprecated since 2.2.1 - should get this through {@link #getRequest()} instead.
     * @return request retry delay
     */
    @Deprecated(since = "2.2.1")
    public int getRequestRetryDelay() {
        return request.getRequestRetryDelay();
    }

    /**
     * @deprecated since 2.2.1 - should get this through {@link #getRequest()} instead.
     * @return error threshold
     */
    @Deprecated(since = "2.2.1")
    public int getErrorThreshold() {
        return request.getErrorThreshold();
    }

    /**
     * @deprecated since 2.2.1 - should get this through {@link #getRequest()} instead.
     * @return true if inject open tracing is enabled
     */
    @Deprecated(since = "2.2.1")
    public boolean isInjectOpenTracing() {
        return request.isInjectOpenTracing();
    }

    /**
     * @deprecated since 2.2.1 - should get this through {@link #getRequest()} instead.
     * @return true if inject caller id is enabled
     */
    @Deprecated(since = "2.2.1")
    public boolean isInjectCallerId() {
        return request.isInjectCallerId();
    }

    /**
     * @deprecated since 2.2.1 - should get this through {@link #getRequest()} instead.
     * @return request connection pool size
     */
    @Deprecated(since = "2.2.1")
    public int getConnectionPoolSize() {
        return request.getConnectionPoolSize();
    }

    /**
     * @deprecated since 2.2.1 - should get this through {@link #getRequest()} instead.
     * @return request max request per connection
     */
    @Deprecated(since = "2.2.1")
    public int getMaxRequestPerConnection() {
        return request.getMaxReqPerConn();
    }

    /**
     * @deprecated since 2.2.1 - should get this through {@link #getRequest()} instead.
     * @return request enable http2
     */
    @Deprecated(since = "2.2.1")
    public boolean getRequestEnableHttp2() {
        return request.isEnableHttp2();
    }

    /**
     * @deprecated since 2.2.1 - should get this through {@link #getRequest()} instead.
     */
    public void setRequestEnableHttp2(boolean requestEnableHttp2) {
        request.setIsEnableHttp2(requestEnableHttp2);
    }

    /**
     * @deprecated since 2.2.1 - should get this through {@link #getRequest()} instead.
     * @return request connection expire time
     */
    @Deprecated(since = "2.2.1")
    public long getConnectionExpireTime() {
        return request.getConnectionExpireTime();
    }

    /**
     * @deprecated since 2.2.1 - should get this through {@link #getRequest()} instead.
     * @return max connection number per host
     */
    @Deprecated(since = "2.2.1")
    public int getMaxConnectionNumPerHost() {
        return request.getMaxConnectionNumPerHost();
    }

    /**
     * @deprecated since 2.2.1 - should get this through {@link #getRequest()} instead.
     * @return min connection number per host
     */
    @Deprecated(since = "2.2.1")
    public int getMinConnectionNumPerHost() {
        return this.request.getMinConnectionNumPerHost();
    }

    /**
     * @deprecated since 2.2.1 - should get this through {@link #getOAuth()} instead.
     * @return true if multiple auth servers
     */
    @Deprecated(since = "2.2.1")
    public boolean isMultipleAuthServers() {
        return this.oauthConfig.isMultipleAuthServers();
    }


    public int getBufferSize() {
        return DEFAULT_BUFFER_SIZE;
    }
}
