package com.networknt.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Create this class to load security.yml or openapi-security or graphql-security
 * or hybrid-security configuration file with map as default value.
 *
 * Note: there is no static CONFIG_NAME in this class and user has to pass in the
 * name according to the framework that is used.
 *
 * @author Steve Hu
 */
@ConfigSchema(configName = "security", configKey = "security", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML})
public class SecurityConfig {
    public static final String CONFIG_NAME = "security";
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private static final String ENABLE_VERIFY_JWT = "enableVerifyJwt";
    private static final String ENABLE_VERIFY_SWT = "enableVerifySwt";
    private static final String SWT_CLIENT_ID_HEADER = "swtClientIdHeader";
    private static final String SWT_CLIENT_SECRET_HEADER = "swtClientSecretHeader";
    private static final String ENABLE_EXTRACT_SCOPE_TOKEN = "enableExtractScopeToken";
    private static final String ENABLE_VERIFY_SCOPE = "enableVerifyScope";
    private static final String SKIP_VERIFY_SCOPE_WITHOUT_SPEC = "skipVerifyScopeWithoutSpec";
    private static final String ENABLE_MOCK_JWT = "enableMockJwt";
    private static final String JWT = "jwt";
    private static final String CERTIFICATE = "certificate";
    private static final String CLOCK_SKEW_IN_SECONDS = "clockSkewInSeconds";
    private static final String KEY_RESOLVER = "keyResolver";
    private static final String LOG_JWT_TOKEN = "logJwtToken";
    private static final String LOG_CLIENT_USER_SCOPE = "logClientUserScope";
    private static final String ENABLE_JWT_CACHE = "enableJwtCache";
    private static final String JWT_CACHE_FULL_SIZE = "jwtCacheFullSize";
    private static final String BOOTSTRAP_FROM_KEY_SERVICE = "bootstrapFromKeyService";
    private static final String IGNORE_JWT_EXPIRY = "ignoreJwtExpiry";
    private static final String PROVIDER_ID = "providerId";
    private static final String ENABLE_H2C = "enableH2c";

    private static final String ENABLE_RELAXED_KEY_CONSTRAINTS = "enableRelaxedKeyValidation";
    private static final String SKIP_PATH_PREFIXES = "skipPathPrefixes";
    private static final String PASS_THROUGH_CLAIMS = "passThroughClaims";

    private Map<String, Object> mappedConfig;

    private final Config config;

    @BooleanField(
            configFieldName = ENABLE_VERIFY_JWT,
            externalizedKeyName = ENABLE_VERIFY_JWT,
            externalized = true,
            defaultValue = true,
            description = "Enable the JWT verification flag. The JwtVerifierHandler will skip the JWT token verification\n" +
                    "if this flag is false. It should only be set to false on the dev environment for testing\n" +
                    "purposes. If you have some endpoints that want to skip the JWT verification, you can put the\n" +
                    "request path prefix in skipPathPrefixes."
    )
    private boolean enableVerifyJwt;

    @BooleanField(
            configFieldName = ENABLE_VERIFY_SWT,
            externalizedKeyName = ENABLE_VERIFY_SWT,
            externalized = true,
            defaultValue = true,
            description = "Enable the SWT verification flag. The SwtVerifierHandler will skip the SWT token verification\n" +
                    "if this flag is false. It should only be set to false on the dev environment for testing\n" +
                    "purposes. If you have some endpoints that want to skip the SWT verification, you can put the\n" +
                    "request path prefix in skipPathPrefixes."
    )
    private boolean enableVerifySwt;

    @StringField(
            configFieldName = SWT_CLIENT_ID_HEADER,
            externalizedKeyName = SWT_CLIENT_ID_HEADER,
            externalized = true,
            defaultValue = "swt-client",
            description = "swt clientId header name. When light-gateway is used and the consumer app does not want to save\n" +
                    "the client secret in the configuration file, it can be passed in the header."
    )
    private String swtClientIdHeader;

    @StringField(
            configFieldName = SWT_CLIENT_SECRET_HEADER,
            externalizedKeyName = SWT_CLIENT_SECRET_HEADER,
            externalized = true,
            defaultValue = "swt_secret",
            description = "swt clientSecret header name. When light-gateway is used and the consumer app does not want to save\n" +
                    "the client secret in the configuration file, it can be passed in the header."
    )
    private String swtClientSecretHeader;

    @BooleanField(
            configFieldName = ENABLE_EXTRACT_SCOPE_TOKEN,
            externalizedKeyName = ENABLE_EXTRACT_SCOPE_TOKEN,
            externalized = true,
            defaultValue = true,
            description = "Extract JWT scope token from the X-Scope-Token header and validate the JWT token"
    )
    private boolean enableExtractScopeToken;

    @BooleanField(
            configFieldName = ENABLE_VERIFY_SCOPE,
            externalizedKeyName = ENABLE_VERIFY_SCOPE,
            externalized = true,
            defaultValue = true,
            description = "Enable JWT scope verification. This flag is valid when enableVerifyJwt is true. When using the\n" +
                    "light gateway as a centralized gateway without backend API specifications, you can still enable\n" +
                    "this flag to allow the admin endpoints to have scopes verified. And all backend APIs without\n" +
                    "specifications skip the scope verification if the spec does not exist with the skipVerifyScopeWithoutSpec\n" +
                    "flag to true. Also, you need to have the openapi.yml specification file in the config folder to\n" +
                    "enable it, as the scope verification compares the scope from the JWT token and the scope in the\n" +
                    "endpoint specification."
    )
    private boolean enableVerifyScope;

    @BooleanField(
            configFieldName = SKIP_VERIFY_SCOPE_WITHOUT_SPEC,
            externalizedKeyName = SKIP_VERIFY_SCOPE_WITHOUT_SPEC,
            externalized = true,
            description = "Users should only use this flag in a shared light gateway if the backend API specifications are\n" +
                    "unavailable in the gateway config folder. If this flag is true and the enableVerifyScope is true,\n" +
                    "the security handler will invoke the scope verification for all endpoints. However, if the endpoint\n" +
                    "doesn't have a specification to retrieve the defined scopes, the handler will skip the scope verification."
    )
    private boolean skipVerifyScopeWithoutSpec;

    @BooleanField(
            configFieldName = IGNORE_JWT_EXPIRY,
            externalizedKeyName = IGNORE_JWT_EXPIRY,
            externalized = true,
            description = "If set true, the JWT verifier handler will pass if the JWT token is expired already. Unless\n" +
                    "you have a strong reason, please use it only on the dev environment if your OAuth 2 provider\n" +
                    "doesn't support long-lived token for dev environment or test automation."
    )
    private boolean ignoreJwtExpiry;



    @BooleanField(
            configFieldName = ENABLE_H2C,
            externalizedKeyName = ENABLE_H2C,
            externalized = true,
            defaultValue = true,
            description = "set true if you want to allow http 1/1 connections to be upgraded to http/2 using the UPGRADE method (h2c).\n" +
                    "By default, this is set to false for security reasons. If you choose to enable it make sure you can handle http/2 w/o tls."
    )
    private boolean enableH2c;

    @BooleanField(
            configFieldName = ENABLE_MOCK_JWT,
            externalizedKeyName = ENABLE_MOCK_JWT,
            externalized = true,
            description = "User for test only. should be always be false on official environment."
    )
    private boolean enableMockJwt;

    @BooleanField(
            configFieldName = ENABLE_RELAXED_KEY_CONSTRAINTS,
            externalizedKeyName = ENABLE_RELAXED_KEY_CONSTRAINTS,
            externalized = true,
            defaultValue = true,
            description = ""
    )
    private boolean enableRelaxedKeyValidation;

    // JWT

//    @IntegerField(
//            configFieldName = CLOCK_SKEW_IN_SECONDS,
//            externalizedKeyName = CLOCK_SKEW_IN_SECONDS,
//            externalized = true
//    )
//    private int clockSkewInSeconds;
//
//
//    private Map<String, Object> certificate;
//
//    @StringField(
//            configFieldName = KEY_RESOLVER,
//            externalizedKeyName = KEY_RESOLVER,
//            externalized = true,
//            description = "Key distribution server standard: JsonWebKeySet for other OAuth 2.0 provider| X509Certificate for light-oauth2"
//    )
//    private String keyResolver;

    // ~JWT

    @ObjectField(
            configFieldName = JWT,
            description = "JWT signature public certificates. kid and certificate path mappings.",
            ref = SecurityJwtConfig.class,
            useSubObjectDefault = true
    )
    private SecurityJwtConfig jwt;

    @BooleanField(
            configFieldName = LOG_JWT_TOKEN,
            externalizedKeyName = LOG_JWT_TOKEN,
            externalized = true,
            defaultValue = true,
            description = "Enable or disable JWT token logging for audit. This is to log the entire token\n" +
                    "or choose the next option that only logs client_id, user_id and scope."
    )
    private boolean logJwtToken;

    @BooleanField(
            configFieldName = LOG_CLIENT_USER_SCOPE,
            externalizedKeyName = LOG_CLIENT_USER_SCOPE,
            externalized = true,
            defaultValue = true,
            description = "Enable or disable client_id, user_id and scope logging if you don't want to log\n" +
                    "the entire token. Choose this option or the option above."
    )
    private boolean logClientUserScope;

    @BooleanField(
            configFieldName = ENABLE_JWT_CACHE,
            externalizedKeyName = ENABLE_JWT_CACHE,
            externalized = true,
            defaultValue = true,
            description = "Enable JWT token cache to speed up verification. This will only verify expired time\n" +
                    "and skip the signature verification as it takes more CPU power and a long time. If\n" +
                    "each request has a different jwt token, like authorization code flow, this indicator\n" +
                    "should be turned off. Otherwise, the cached jwt will only be removed after 15 minutes\n" +
                    "and the cache can grow bigger if the number of requests is very high. This will cause\n" +
                    "memory kill in a Kubernetes pod if the memory setting is limited."
    )
    private boolean enableJwtCache;

    @IntegerField(
            configFieldName = JWT_CACHE_FULL_SIZE,
            externalizedKeyName = JWT_CACHE_FULL_SIZE,
            externalized = true,
            description = "If enableJwtCache is true, then an error message will be shown up in the log if the\n" +
                    "cache size is bigger than the jwtCacheFullSize. This helps the developers to detect\n" +
                    "cache problem if many distinct tokens flood the cache in a short period of time. If\n" +
                    "you see JWT cache exceeds the size limit in logs, you need to turn off the enableJwtCache\n" +
                    "or increase the cache full size to a bigger number from the default 100."
    )
    private int jwtCacheFullSize;

    @BooleanField(
            configFieldName = BOOTSTRAP_FROM_KEY_SERVICE,
            externalizedKeyName = BOOTSTRAP_FROM_KEY_SERVICE,
            externalized = true,
            defaultValue = true,
            description = "If you are using light-oauth2, then you don't need to have oauth subfolder for public\n" +
                    "key certificate to verify JWT token, the key will be retrieved from key endpoint once\n" +
                    "the first token is arrived. Default to false for dev environment without oauth2 server\n" +
                    "or official environment that use other OAuth 2.0 providers."
    )
    private boolean bootstrapFromKeyService;



    @StringField(
            configFieldName = PROVIDER_ID,
            externalizedKeyName = PROVIDER_ID,
            externalized = true,
            description = "Used in light-oauth2 and oauth-kafka key service for federated deployment. Each instance\n" +
                    "will have a providerId, and it will be part of the kid to allow each instance to get the\n" +
                    "JWK from other instance based on the providerId in the kid."
    )
    private String providerId;

    @ArrayField(
            configFieldName = SKIP_PATH_PREFIXES,
            externalizedKeyName = SKIP_PATH_PREFIXES,
            externalized = true,
            description = "Define a list of path prefixes to skip the security to ease the configuration for the\n" +
                    "handler.yml so that users can define some endpoint without security even through it uses\n" +
                    "the default chain. This is particularly useful in the light-gateway use case as the same\n" +
                    "instance might be shared with multiple consumers and providers with different security\n" +
                    "requirement. The format is a list of strings separated with commas or a JSON list in\n" +
                    "values.yml definition from config server, or you can use yaml format in this file.",
            items = String.class
    )
    private List<String> skipPathPrefixes;

    @MapField(
            configFieldName = PASS_THROUGH_CLAIMS,
            externalizedKeyName = PASS_THROUGH_CLAIMS,
            externalized = true,
            description = "When light-gateway or http-sidecar is used for security, sometimes, we need to pass some\n" +
                    "claims from the JWT or SWT to the backend API for further verification or audit. You can\n" +
                    "select some claims to pass to the backend API with HTTP headers. The format is a map of\n" +
                    "claim in the token and a header name that the downstream API is expecting. You can use\n" +
                    "both JSON or YAML format.\n" +
                    "When SwtVerifyHandler is used, the claim names are in https://github.com/networknt/light-4j/blob/master/client/src/main/java/com/networknt/client/oauth/TokenInfo.java\n" +
                    "When JwtVerifyHandler is used, the claim names is the JwtClaims claimName.\n" +
                    "YAML\n" +
                    "security.passThroughClaims:\n" +
                    "  clientId: client_id\n" +
                    "  tokenType: token_type\n" +
                    "JSON\n" +
                    "security.passThroughClaims: {\"clientId\":\"client_id\",\"tokenType\":\"token_type\"}",
            valueType = String.class
    )
    private Map<String, String> passThroughClaims;

    private SecurityConfig() {
        this(CONFIG_NAME);
    }

    private SecurityConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setCertificate();
        setConfigData();
        setSkipPathPrefixes();
        setPassThroughClaims();
    }

    public static SecurityConfig load() {
        return new SecurityConfig();
    }

    /**
     * This method is only used in the test case to load different configuration files. Please use load() instead.
     * @param configName String
     * @return SecurityConfig
     */
    @Deprecated
    public static SecurityConfig load(String configName) {
        return new SecurityConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setCertificate();
        setConfigData();
        setSkipPathPrefixes();
        setPassThroughClaims();
    }

    @Deprecated
    public void reload(String configName) {
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setCertificate();
        setConfigData();
        setSkipPathPrefixes();
        setPassThroughClaims();
    }

    public Map<String, Object> getCertificate() {
        return jwt.getCertificate();
    }

    public boolean isEnableVerifyJwt() {
        return enableVerifyJwt;
    }
    public boolean isEnableVerifySwt() {
        return enableVerifySwt;
    }
    public String getSwtClientIdHeader() { return swtClientIdHeader; }
    public String getSwtClientSecretHeader() { return swtClientSecretHeader; }

    public boolean isEnableH2c() { return enableH2c; }

    public boolean isEnableRelaxedKeyValidation() { return enableRelaxedKeyValidation; }

    public boolean isEnableExtractScopeToken() {
        return enableExtractScopeToken;
    }

    public boolean isEnableVerifyScope() {
        return enableVerifyScope;
    }

    public boolean isSkipVerifyScopeWithoutSpec() {
        return skipVerifyScopeWithoutSpec;
    }

    public boolean isIgnoreJwtExpiry() {
        return ignoreJwtExpiry;
    }

    public boolean isEnableMockJwt() {
        return enableMockJwt;
    }

    public int getClockSkewInSeconds() {
        return jwt.getClockSkewInSeconds();
    }

    public String getKeyResolver() {
        return jwt.getKeyResolver();
    }

    public boolean isLogJwtToken() {
        return logJwtToken;
    }

    public boolean isLogClientUserScope() {
        return logClientUserScope;
    }

    public boolean isEnableJwtCache() {
        return enableJwtCache;
    }

    public int getJwtCacheFullSize() {
        return jwtCacheFullSize;
    }
    public boolean isBootstrapFromKeyService() {
        return bootstrapFromKeyService;
    }
    public List<String> getSkipPathPrefixes() {
        return skipPathPrefixes;
    }
    public Map<String, String> getPassThroughClaims() { return passThroughClaims; }
    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }
    public String getProviderId() {
        return providerId;
    }
    Config getConfig() {
        return config;
    }

    private void setCertificate() {

        if(getMappedConfig() != null) {
            Map<String, Object> jwtMap = (Map)getMappedConfig().get(JWT);
            final var mapper = Config.getInstance().getMapper();
            this.jwt = mapper.convertValue(jwtMap, new TypeReference<>(){});
//            Object obj = jwtMap.get(CERTIFICATE);
//            if(obj instanceof String) {
//                String s = (String)obj;
//                if(logger.isTraceEnabled()) logger.trace("s = " + s);
//                Map<String, Object> map = new LinkedHashMap<>();
//                for(String keyValue : s.split(" *& *")) {
//                    String[] pairs = keyValue.split(" *= *", 2);
//                    map.put(pairs[0], pairs.length == 1 ? "" : pairs[1]);
//                }
//                certificate = map;
//            } else if (obj instanceof Map) {
//                certificate = (Map)obj;
//            } else {
//                certificate = new HashMap<>();
//            }
        }
    }

    private void setConfigData() {
        if(getMappedConfig() != null) {
            Object object = getMappedConfig().get(ENABLE_VERIFY_JWT);
            if(object != null) enableVerifyJwt = Config.loadBooleanValue(ENABLE_VERIFY_JWT, object);
            object = getMappedConfig().get(ENABLE_VERIFY_SWT);
            if(object != null) enableVerifySwt = Config.loadBooleanValue(ENABLE_VERIFY_SWT, object);
            object = getMappedConfig().get(SWT_CLIENT_ID_HEADER);
            if(object != null) swtClientIdHeader = (String)object;
            object = getMappedConfig().get(SWT_CLIENT_SECRET_HEADER);
            if(object != null) swtClientSecretHeader = (String)object;
            object = getMappedConfig().get(ENABLE_H2C);
            if(object != null) enableH2c = Config.loadBooleanValue(ENABLE_H2C, object);
            object = getMappedConfig().get(ENABLE_RELAXED_KEY_CONSTRAINTS);
            if(object != null) enableRelaxedKeyValidation = Config.loadBooleanValue(ENABLE_RELAXED_KEY_CONSTRAINTS, object);
            object = getMappedConfig().get(ENABLE_EXTRACT_SCOPE_TOKEN);
            if(object != null) enableExtractScopeToken = Config.loadBooleanValue(ENABLE_EXTRACT_SCOPE_TOKEN, object);
            object = getMappedConfig().get(ENABLE_VERIFY_SCOPE);
            if(object != null) enableVerifyScope = Config.loadBooleanValue(ENABLE_VERIFY_SCOPE, object);
            object = getMappedConfig().get(SKIP_VERIFY_SCOPE_WITHOUT_SPEC);
            if(object != null) skipVerifyScopeWithoutSpec = Config.loadBooleanValue(SKIP_VERIFY_SCOPE_WITHOUT_SPEC, object);
            object = getMappedConfig().get(ENABLE_MOCK_JWT);
            if(object != null) enableMockJwt = Config.loadBooleanValue(ENABLE_MOCK_JWT, object);
            object = getMappedConfig().get(LOG_JWT_TOKEN);
            if(object != null) logJwtToken = Config.loadBooleanValue(LOG_JWT_TOKEN, object);
            object = getMappedConfig().get(LOG_CLIENT_USER_SCOPE);
            if(object != null) logClientUserScope = Config.loadBooleanValue(LOG_CLIENT_USER_SCOPE, object);
            object = getMappedConfig().get(ENABLE_JWT_CACHE);
            if(object != null) enableJwtCache = Config.loadBooleanValue(ENABLE_JWT_CACHE, object);
            object = getMappedConfig().get(JWT_CACHE_FULL_SIZE);
            if(object != null ) jwtCacheFullSize = Config.loadIntegerValue(JWT_CACHE_FULL_SIZE, object);
            object = getMappedConfig().get(BOOTSTRAP_FROM_KEY_SERVICE);
            if(object != null) bootstrapFromKeyService = Config.loadBooleanValue(BOOTSTRAP_FROM_KEY_SERVICE, object);
            object = getMappedConfig().get(IGNORE_JWT_EXPIRY);
            if(object != null) ignoreJwtExpiry = Config.loadBooleanValue(IGNORE_JWT_EXPIRY, object);
            object = getMappedConfig().get(PROVIDER_ID);
            if(object != null) providerId = (String)object;
//            Map<String, Object> jwtMap = (Map)getMappedConfig().get(JWT);
//            if(jwtMap != null) {
//                object = jwtMap.get(CLOCK_SKEW_IN_SECONDS);
//                if(object != null) clockSkewInSeconds = Config.loadIntegerValue(CLOCK_SKEW_IN_SECONDS, object);
//                keyResolver = (String) jwtMap.get(KEY_RESOLVER);
//            }
        }
    }

    private void setSkipPathPrefixes() {
        if (mappedConfig != null && mappedConfig.get(SKIP_PATH_PREFIXES) != null) {
            Object object = mappedConfig.get(SKIP_PATH_PREFIXES);
            skipPathPrefixes = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        skipPathPrefixes = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the skipPathPrefixes json with a list of strings.");
                    }
                } else {
                    // comma separated
                    skipPathPrefixes = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                List prefixes = (List)object;
                prefixes.forEach(item -> {
                    skipPathPrefixes.add((String)item);
                });
            } else {
                throw new ConfigException("skipPathPrefixes must be a string or a list of strings.");
            }
        }
    }

    private void setPassThroughClaims() {
        if(mappedConfig != null && mappedConfig.get(PASS_THROUGH_CLAIMS) != null) {
            Object obj = mappedConfig.get(PASS_THROUGH_CLAIMS);
            if(obj instanceof String) {
                String s = (String)obj;
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("{")) {
                    // json map
                    try {
                        passThroughClaims = Config.getInstance().getMapper().readValue(s, Map.class);
                    } catch (IOException e) {
                        logger.error("IOException:", e);
                    }
                } else {
                    passThroughClaims = new HashMap<>();
                    for(String keyValue : s.split(" *& *")) {
                        String[] pairs = keyValue.split(" *= *", 2);
                        passThroughClaims.put(pairs[0], pairs[1]);
                    }
                }
            } else if (obj instanceof Map) {
                passThroughClaims = (Map)obj;
            } else {
                logger.error("passThroughClaims is the wrong type. Only JSON map or YAML map is supported.");
            }
        }
    }

}
