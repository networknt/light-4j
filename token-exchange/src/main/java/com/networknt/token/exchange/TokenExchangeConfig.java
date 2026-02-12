package com.networknt.token.exchange;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.schema.*;
import com.networknt.token.exchange.extract.AuthType;
import com.networknt.token.exchange.schema.TokenSchema;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigSchema(
        configKey = "token-exchange",
        configName = "token-exchange",
        outputFormats = {
                OutputFormat.JSON_SCHEMA,
                OutputFormat.YAML,
                OutputFormat.CLOUD
        }
)
public class TokenExchangeConfig {

    public static final String ENABLED = "enabled";
    public static final String CONFIG_NAME = "token-exchange";
    public static final String TOKEN_SCHEMA = "tokenSchemas";
    public static final String CLIENT_MAPPINGS = "clientMappings";
    public static final String PATH_AUTH_MAPPINGS = "pathAuthMappings";
    public static final String DEFAULT_AUTH_TYPE = "defaultAuthType";
    public static final String PROXY_HOST = "proxyHost";
    public static final String PROXY_PORT = "proxyPort";
    public static final String ENABLE_HTTP2 = "enableHttp2";
    public static final String MODULE_MASKS = "moduleMasks";

    private final Config config;
    private final Map<String, Object> mappedConfig;

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            defaultValue = "true"
    )
    @JsonProperty(ENABLED)
    private boolean enabled;

    @IntegerField(
            configFieldName = PROXY_PORT,
            externalizedKeyName = PROXY_PORT,
            description = "Default proxy port."
    )
    @JsonProperty(PROXY_PORT)
    private int proxyPort;

    @StringField(
            configFieldName = PROXY_HOST,
            externalizedKeyName = PROXY_HOST,
            description = "Default proxy host used in token requests."
    )
    @JsonProperty(PROXY_HOST)
    private String proxyHost;

    @BooleanField(
            configFieldName = ENABLE_HTTP2,
            externalizedKeyName = ENABLE_HTTP2,
            defaultValue = "true",
            description = "Enable/disable http2. Default enabled."
    )
    @JsonProperty(ENABLE_HTTP2)
    private boolean enableHttp2;

    @ArrayField(
            configFieldName = MODULE_MASKS,
            externalizedKeyName = MODULE_MASKS,
            items = String.class
    )
    @JsonProperty(MODULE_MASKS)
    private List<String> moduleMasks;

    @MapField(
            configFieldName = TOKEN_SCHEMA,
            externalizedKeyName = TOKEN_SCHEMA,
            valueType = TokenSchema.class
    )
    @JsonProperty(TOKEN_SCHEMA)
    private Map<String, TokenSchema> tokenSchemas;

    @MapField(
            configFieldName = CLIENT_MAPPINGS,
            externalizedKeyName = CLIENT_MAPPINGS,
            description = """
                    Maps client IDs (from Authorization headers) to token schema names.
                    This allows automatic schema selection based on the incoming request's credentials."""
    )
    @JsonProperty(CLIENT_MAPPINGS)
    private Map<String, String> clientMappings;

    @MapField(
            configFieldName = PATH_AUTH_MAPPINGS,
            externalizedKeyName = PATH_AUTH_MAPPINGS,
            description = """
                    Maps path prefixes to authentication types.
                    This determines how to extract client identity from the Authorization header."""
    )
    @JsonProperty(PATH_AUTH_MAPPINGS)
    private Map<String, AuthType> pathAuthMappings = new HashMap<>();

    @StringField(
            configFieldName = DEFAULT_AUTH_TYPE,
            externalizedKeyName = DEFAULT_AUTH_TYPE,
            description = "Default auth type to use when no path prefix matches."
    )
    @JsonProperty(DEFAULT_AUTH_TYPE)
    private AuthType defaultAuthType;

    public TokenExchangeConfig() {
        this(CONFIG_NAME);
    }

    public TokenExchangeConfig(final String configName) {
        this.config = Config.getInstance();
        this.mappedConfig = this.config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    public static TokenExchangeConfig load() {
        return new TokenExchangeConfig();
    }

    public static TokenExchangeConfig load(final String configName) {
        return new TokenExchangeConfig(configName);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public Map<String, TokenSchema> getTokenSchemas() {
        return tokenSchemas;
    }

    public List<String> getModuleMasks() {
        return moduleMasks;
    }

    public Map<String, String> getClientMappings() {
        return clientMappings;
    }

    public Map<String, AuthType> getPathAuthMappings() {
        return pathAuthMappings;
    }

    public AuthType getDefaultAuthType() {
        return defaultAuthType;
    }

    /**
     * Resolves the authentication type for a given request path.
     * Checks path prefixes and returns the auth type for the first matching prefix.
     *
     * @param requestPath the request path
     * @return the resolved AuthType, or defaultAuthType if no prefix matches, or null if no default
     */
    public AuthType resolveAuthTypeFromPath(final String requestPath) {
        if (requestPath == null || pathAuthMappings == null || pathAuthMappings.isEmpty()) {
            return defaultAuthType;
        }

        for (final var entry : pathAuthMappings.entrySet()) {
            if (requestPath.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        return defaultAuthType;
    }

    /**
     * Resolves a token schema name from a client ID using the clientMappings configuration.
     *
     * @param clientId the client ID extracted from the Authorization header
     * @return the token schema name if a mapping exists, null otherwise
     */
    public String resolveSchemaFromClientId(final String clientId) {
        if (clientMappings == null || clientId == null) {
            return null;
        }
        return clientMappings.get(clientId);
    }

    private void setModuleMasks(List<String> moduleMasks) {
        this.moduleMasks = moduleMasks;
    }

    private void setTokenSchemas(Map<String, TokenSchema> tokenSchemas) {
        this.tokenSchemas = tokenSchemas;
    }

    private void setPathAuthMappings(Map<String, AuthType> pathAuthMappings) {
        this.pathAuthMappings = pathAuthMappings != null ? pathAuthMappings : new HashMap<>();
    }

    private void setDefaultAuthType(AuthType defaultAuthType) {
        this.defaultAuthType = defaultAuthType;
    }

    @SuppressWarnings("unchecked")
    private void setConfigData() {
        var object = this.mappedConfig.get(ENABLED);
        if (object instanceof Boolean enabledValue)
            this.enabled = enabledValue;

        object = this.mappedConfig.get(PROXY_HOST);
        if (object instanceof String host)
            this.proxyHost = host;

        object = this.mappedConfig.get(PROXY_PORT);
        if (object instanceof Integer port)
            this.proxyPort = port;

        object = this.mappedConfig.get(ENABLE_HTTP2);
        if (object instanceof Boolean enabled)
            this.enableHttp2 = enabled;

        object = this.mappedConfig.get(MODULE_MASKS);
        if (object instanceof List
                && !((List<?>) object).isEmpty()
                && ((List<?>) object).get(0) instanceof String)
            setModuleMasks((List<String>) object);

        if (this.mappedConfig.get(TOKEN_SCHEMA) != null) {
            final var rawTokenSchemas = this.mappedConfig.get(TOKEN_SCHEMA);
            if (rawTokenSchemas instanceof Map) {
                final var converted = Config.getInstance().getMapper().convertValue(rawTokenSchemas, new TypeReference<Map<String, TokenSchema>>() {});
                setTokenSchemas(converted);
            } else if (rawTokenSchemas instanceof String) {
                // TODO - handle string.
                throw new NotImplementedException("tokenSchema string-to-pojo not implemented!");
            }
        }

        // Load client mappings
        object = this.mappedConfig.get(CLIENT_MAPPINGS);
        if (object instanceof Map mappings)
            this.clientMappings = (Map<String, String>) mappings;

        // Load path auth mappings
        object = this.mappedConfig.get(PATH_AUTH_MAPPINGS);
        if (object instanceof Map) {
            final var pathMappings = new HashMap<String, AuthType>();
            for (final var entry : ((Map<String, Object>) object).entrySet()) {
                final var authType = parseAuthType(entry.getValue());
                if (authType != null) {
                    pathMappings.put(entry.getKey(), authType);
                }
            }
            setPathAuthMappings(pathMappings);
        }

        // Load default auth type
        object = this.mappedConfig.get(DEFAULT_AUTH_TYPE);
        if (object != null) {
            this.defaultAuthType = this.parseAuthType(object);
        }
    }

    private AuthType parseAuthType(final Object value) {
        if (value instanceof AuthType auth) {
            return auth;
        }
        if (value instanceof String authStr) {
            try {
                return AuthType.valueOf(authStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }
}
