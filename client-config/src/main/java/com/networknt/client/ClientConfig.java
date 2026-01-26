package com.networknt.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.schema.*;
import com.networknt.server.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@ConfigSchema(
        configKey = "client",
        configName = "client",
        configDescription = "This is the configuration file for light-4j Http2Client and jdk 11 http-client.",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ClientConfig {
    private static final Logger logger = LoggerFactory.getLogger(ClientConfig.class);
    public static final String TOKEN = "token";
    public static final String MULTIPLE_AUTH_SERVERS = "multipleAuthServers";
    public static final String DEREF = "deref";
    public static final String SIGN = "sign";
    public static final String CACHE = "cache";
    public static final String TOKEN_RENEW_BEFORE_EXPIRED = "tokenRenewBeforeExpired";
    public static final String EXPIRED_REFRESH_RETRY_DELAY = "expiredRefreshRetryDelay";
    public static final String EARLY_REFRESH_RETRY_DELAY = "earlyRefreshRetryDelay";
    public static final String SERVER_URL = "server_url";
    public static final String SERVICE_ID = "serviceId";
    public static final String PROXY_HOST = "proxyHost";
    public static final String PROXY_PORT = "proxyPort";
    public static final String ENABLE_HTTP_2 = "enableHttp2";
    public static final String AUTHORIZATION_CODE = "authorization_code";
    public static final String CLIENT_CREDENTIALS = "client_credentials";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String TOKEN_EXCHANGE = "token_exchange";

    public static final String KEY = "key";
    public static final String CONFIG_NAME = "client";
    public static final String REQUEST = "request";
    public static final String URI = "uri";
    public static final String TLS = "tls";
    public static final String CLIENT_ID = "client_id";
    public static final String SCOPE = "scope";
    public static final String AUDIENCE = "audience";
    public static final String CSRF = "csrf";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String SAML_BEARER = "saml_bearer";
    public static final String CLIENT_AUTHENTICATED_USER = "client_authenticated_user";
    public static final String CAPACITY = "capacity";
    public static final String OAUTH = "oauth";
    public static final String PATH_PREFIX_SERVICES = "pathPrefixServices";
    public static final String SERVICE_ID_AUTH_SERVERS = "serviceIdAuthServers";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String ENABLE_HTTP2 = "enableHttp2";
    public static final String CONNECT_TIMEOUT = "connectTimeout";
    public static final String TIMEOUT = "timeout";
    public static final String MAX_REQUEST_RETRY = "maxRequestRetry";
    public static final String REQUEST_RETRY_DELAY = "requestRetryDelay";
    public static final int DEFAULT_BUFFER_SIZE = 24; // 24*1024 buffer size will be good for most of the app.
    public static final int DEFAULT_ERROR_THRESHOLD = 2;
    public static final int DEFAULT_CONNECT_TIMEOUT = 2000;
    public static final int DEFAULT_TIMEOUT = 3000;
    public static final int DEFAULT_RESET_TIMEOUT = 7000;
    public static final String ERROR_THRESHOLD = "errorThreshold";
    public static final String RESET_TIMEOUT = "resetTimeout";
    public static final String INJECT_OPEN_TRACING = "injectOpenTracing";
    public static final String INJECT_CALLER_ID = "injectCallerId";
    public static final String CONNECTION_POOL_SIZE = "connectionPoolSize";
    public static final String CONNECTION_EXPIRE_TIME = "connectionExpireTime";
    public static final String MAX_REQ_PER_CONN = "maxReqPerConn";
    public static final String MAX_CONNECTION_NUM_PER_HOST = "maxConnectionNumPerHost";
    public static final String MIN_CONNECTION_NUM_PER_HOST = "minConnectionNumPerHost";
    public static final String VERIFY_HOSTNAME = "verifyHostname";
    public static final String LOAD_DEFAULT_TRUST_STORE = "loadDefaultTrustStore";
    public static final String LOAD_TRUST_STORE = "loadTrustStore";
    public static final String TRUST_STORE = "trustStore";
    public static final String TRUST_STORE_PASS = "trustStorePass";
    public static final String LOAD_KEY_STORE = "loadKeyStore";
    public static final String KEY_STORE = "keyStore";
    public static final String KEY_STORE_PASS = "keyStorePass";
    public static final String KEY_PASS = "keyPass";
    public static final String DEFAULT_CERT_PASSWORD = "defaultCertPassword";
    public static final String TLS_VERSION = "tlsVersion";
    public static final String SUBJECT_TOKEN = "subjectToken";
    public static final String SUBJECT_TOKEN_TYPE = "subjectTokenType";


    private Map<String, Object> mappedConfig;

    @ObjectField(
            configFieldName = ClientConfig.TLS,
            useSubObjectDefault = true,
            ref = TlsConfig.class,
            description = "Settings for TLS"
    )
    @JsonProperty(ClientConfig.TLS)
    private TlsConfig tls = null;

    @ObjectField(
            configFieldName = ClientConfig.OAUTH,
            useSubObjectDefault = true,
            ref = OAuthConfig.class,
            description = "Settings for OAuth2 server communication."
    )
    @JsonProperty(ClientConfig.OAUTH)
    private OAuthConfig oauthConfig = null;

    @MapField(
            configFieldName = ClientConfig.PATH_PREFIX_SERVICES,
            externalizedKeyName = ClientConfig.PATH_PREFIX_SERVICES,
            valueType = String.class,
            description = "If you have multiple OAuth 2.0 providers and use path prefix to decide which OAuth 2.0 server\n" +
                    "to get the token or JWK. If two or more services have the same path, you must use serviceId in\n" +
                    "the request header to use the serviceId to find the OAuth 2.0 provider configuration."
    )
    @JsonProperty(ClientConfig.PATH_PREFIX_SERVICES)
    private Map<String, String> pathPrefixServices = null;

    @ObjectField(
            configFieldName = ClientConfig.REQUEST,
            useSubObjectDefault = true,
            ref = RequestConfig.class,
            description = "Circuit breaker configuration for the client"
    )
    @JsonProperty(ClientConfig.REQUEST)
    private RequestConfig request = null;

    private final String configName;

    private static volatile ClientConfig instance;

    private ClientConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private ClientConfig(String configName) {
        this.configName = configName;
        mappedConfig = Config.getInstance().getJsonMapConfig(configName);
        if (mappedConfig != null) {
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
        if(instance != null) return instance;
        else return get(CONFIG_NAME);
    }

    public static ClientConfig get(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (ClientConfig.class) {
                mappedConfig = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new ClientConfig(configName);
                // Register the module with the configuration.
                List<String> masks = new ArrayList<>();
                masks.add("client_secret");
                masks.add("trustStorePass");
                masks.add("keyStorePass");
                masks.add("keyPass");
                masks.add("defaultCertPassword");
                ModuleRegistry.registerModule(configName, "com.networknt.client.Http2Client", Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), masks);
                return instance;
            }
        } else {
            instance = new ClientConfig(configName);
            return instance;
        }
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public OAuthConfig getOAuth() {
        return this.oauthConfig;
    }

    public RequestConfig getRequest() {
        return this.request;
    }

    public Map<String, String> getPathPrefixServices() { return this.pathPrefixServices; }

    @Deprecated(since = "2.2.1")
    private void setTlsConfig() {
        final var mapper = Config.getInstance().getMapper();
        this.tls = mapper.convertValue(this.mappedConfig.get(TLS), TlsConfig.class);
    }

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
        return mapper.convertValue(this.oauthConfig, new TypeReference<>() {});
    }


    /**
     * @deprecated since 2.2.1 - Should use the POJO instead of a hashmap.
     * @return token config hashmap
     */
    @Deprecated(since = "2.2.1")
    public Map<String, Object> getTokenConfig() {
        final var mapper = Config.getInstance().getMapper();
        return mapper.convertValue(this.oauthConfig.getToken(), new TypeReference<>() {});
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
