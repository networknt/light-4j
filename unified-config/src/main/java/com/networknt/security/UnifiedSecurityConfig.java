package com.networknt.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import com.networknt.server.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ConfigSchema(
        configKey = "unified-security",
        configName = "unified-security",
        configDescription = "Unified security configuration.",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
public class UnifiedSecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(UnifiedSecurityConfig.class);
    public static final String CONFIG_NAME = "unified-security";
    public static final String ENABLED = "enabled";
    public static final String ANONYMOUS_PREFIXES = "anonymousPrefixes";
    public static final String PATH_PREFIX_AUTHS = "pathPrefixAuths";
    public static final String PREFIX = "prefix";
    public static final String BASIC = "basic";
    public static final String JWT = "jwt";
    public static final String SJWT = "sjwt";
    public static final String SWT = "swt";
    public static final String APIKEY = "apikey";
    public static final String JWK_SERVICE_IDS = "jwkServiceIds";
    public static final String SJWK_SERVICE_IDS = "sjwkServiceIds";
    public static final String SWT_SERVICE_IDS = "swtServiceIds";

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            defaultValue = "true",
            description = "indicate if this handler is enabled. By default, it will be enabled if it is injected into the\n" +
                          "request/response chain in the handler.yml configuration."
    )
    boolean enabled;

    @ArrayField(
            configFieldName = ANONYMOUS_PREFIXES,
            externalizedKeyName = ANONYMOUS_PREFIXES,
            items = String.class,
            description = "Anonymous prefixes configuration. A list of request path prefixes. The anonymous prefixes will be checked\n" +
                          "first, and if any path is matched, all other security checks will be bypassed, and the request goes to\n" +
                          "the next handler in the chain. You can use json array or string separated by comma or YAML format."
    )
    List<String> anonymousPrefixes;

    @ArrayField(
            configFieldName = PATH_PREFIX_AUTHS,
            externalizedKeyName = PATH_PREFIX_AUTHS,
            items = UnifiedPathPrefixAuth.class,
            description =
                    "String format with comma separator\n" +
                    "/v1/pets,/v1/cats,/v1/dogs\n" +
                    "JSON format as a string\n" +
                    "[\"/v1/pets\", \"/v1/dogs\", \"/v1/cats\"]\n" +
                    "YAML format\n" +
                    "  - /v1/pets\n" +
                    "  - /v1/dogs\n" +
                    "  - /v1/cats\n" +
                    "format as a string for config server.\n" +
                    "[{\"prefix\":\"/salesforce\",\"basic\":true,\"jwt\":true,\"apikey\":true,\"jwkServiceIds\":\"com.networknt.petstore-1.0.0, com.networknt.market-1.0.0\"},{\"prefix\":\"/blackrock\",\"basic\":true,\"jwt\":true,\"jwkServiceIds\":[\"com.networknt.petstore-1.0.0\",\"com.networknt.market-1.0.0\"]}]\n" +
                    "format with YAML for readability\n" +
                    "   path prefix security configuration.\n" +
                    "   - pathPrefix: /salesforce\n" +
                    "     # indicate if the basic auth is enabled for this path prefix\n" +
                    "     basic: true\n" +
                    "     # indicate if the jwt token verification is enabled for this path prefix\n" +
                    "     jwt: true\n" +
                    "     # indicate if the simple jwt (jwt token without scopes) is used. The UnifiedSecurityHandler will pre-parse the jwt token if this is true to\n" +
                    "     # make decision which verifier will be invoked. If this is false, the UnifiedSecurityHandler will invoke the JwtVerifier to verify the jwt token.\n" +
                    "     sjwt: true\n" +
                    "     # indicate if the apikey is enabled for this path prefix\n" +
                    "     apikey: true\n" +
                    "     # if jwt is true and there are two or more jwk servers for the path prefix, then list all the\n" +
                    "     # serviceIds for jwk in client.yml for the jwt token\n" +
                    "     jwkServiceIds: service1,service2\n" +
                    "     # if sjwt is true and there are two or more jwk servers for the path prefix, then list all the\n" +
                    "     # serviceIds for jwk in client.yml for the sjwt token\n" +
                    "     sjwkServiceIds: service1,service2\n" +
                    "     # if swt is true and there are two or more introspection servers for the path prefix, then list all the\n" +
                    "     # serviceIds for swt in client.yml for the swt token\n" +
                    "     swtServiceIds: service1,service2"
    )
    List<UnifiedPathPrefixAuth> pathPrefixAuths;

    private static volatile UnifiedSecurityConfig instance;
    private final Config config;
    private final Map<String, Object> mappedConfig;

    private UnifiedSecurityConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private UnifiedSecurityConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        if (mappedConfig != null) {
            setConfigData();
            setConfigList();
        }
    }

    public static UnifiedSecurityConfig load() {
        return load(CONFIG_NAME);
    }

    public static UnifiedSecurityConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            if (instance != null && instance.getMappedConfig() == Config.getInstance().getJsonMapConfig(configName)) {
                return instance;
            }
            synchronized (UnifiedSecurityConfig.class) {
                if (instance != null && instance.getMappedConfig() == Config.getInstance().getJsonMapConfig(configName)) {
                    return instance;
                }
                instance = new UnifiedSecurityConfig(configName);
                ModuleRegistry.registerModule(CONFIG_NAME, UnifiedSecurityConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
                return instance;
            }
        }
        return new UnifiedSecurityConfig(configName);
    }



    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getAnonymousPrefixes() {
        return anonymousPrefixes;
    }

    public void setAnonymousPrefixes(List<String> anonymousPrefixes) {
        this.anonymousPrefixes = anonymousPrefixes;
    }

    public List<UnifiedPathPrefixAuth> getPathPrefixAuths() {
        return pathPrefixAuths;
    }

    public void setPathPrefixAuths(List<UnifiedPathPrefixAuth> pathPrefixAuths) {
        this.pathPrefixAuths = pathPrefixAuths;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    Config getConfig() {
        return config;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if(object != null) {
            if(object instanceof String) {
                enabled = Boolean.parseBoolean((String)object);
            } else if (object instanceof Boolean) {
                enabled = (Boolean) object;
            } else {
                throw new ConfigException("enabled must be a boolean value.");
            }
        }
    }

    private void setConfigList() {
        // anonymous prefixes
        if (mappedConfig != null && mappedConfig.get(ANONYMOUS_PREFIXES) != null) {
            Object object = mappedConfig.get(ANONYMOUS_PREFIXES);
            anonymousPrefixes = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        anonymousPrefixes = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        logger.error("could not parse the anonymousPrefixes json with a list of strings.", e);
                        throw new ConfigException("could not parse the anonymousPrefixes json with a list of strings.");
                    }
                } else {
                    // comma separated
                    anonymousPrefixes = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                List prefixes = (List)object;
                prefixes.forEach(item -> {
                    anonymousPrefixes.add((String)item);
                });
            } else {
                throw new ConfigException("anonymousPrefixes must be a string or a list of strings.");
            }
        }

        // path prefix auth mapping
        if (mappedConfig.get(PATH_PREFIX_AUTHS) != null) {
            Object object = mappedConfig.get(PATH_PREFIX_AUTHS);
            pathPrefixAuths = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("pathPrefixAuth s = " + s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        List<Map<String, Object>> values = Config.getInstance().getMapper().readValue(s, new TypeReference<>() {});
                        pathPrefixAuths = populatePathPrefixAuths(values);
                    } catch (Exception e) {
                        logger.error("could not parse the pathPrefixAuths json with a list of string and object.", e);
                        throw new ConfigException("could not parse the pathPrefixAuths json with a list of string and object.");
                    }
                } else {
                    throw new ConfigException("pathPrefixAuths must be a list of string object map.");
                }
            } else if (object instanceof List) {
                // the object is a list of map, we need convert it to PathPrefixAuth object.
                pathPrefixAuths = populatePathPrefixAuths((List<Map<String, Object>>)object);
            } else {
                throw new ConfigException("pathPrefixAuth must be a list of string object map.");
            }
        }
    }

    public static List<UnifiedPathPrefixAuth> populatePathPrefixAuths(List<Map<String, Object>> values) {
        List<UnifiedPathPrefixAuth> pathPrefixAuths = new ArrayList<>();
        for(Map<String, Object> value: values) {
            UnifiedPathPrefixAuth unifiedPathPrefixAuth = new UnifiedPathPrefixAuth();
            unifiedPathPrefixAuth.setPrefix((String)value.get(PREFIX));
            unifiedPathPrefixAuth.setBasic(value.get(BASIC) == null ? false : (Boolean)value.get(BASIC));
            unifiedPathPrefixAuth.setJwt(value.get(JWT) == null ? false : (Boolean)value.get(JWT));
            unifiedPathPrefixAuth.setSjwt(value.get(SJWT) == null ? false : (Boolean)value.get(SJWT));
            unifiedPathPrefixAuth.setSwt(value.get(SWT) == null ? false : (Boolean)value.get(SWT));
            unifiedPathPrefixAuth.setApikey(value.get(APIKEY) == null ? false : (Boolean)value.get(APIKEY));
            Object jwkIds = value.get(JWK_SERVICE_IDS);
            if(jwkIds instanceof String) {
                String s = (String)value.get(JWK_SERVICE_IDS);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        unifiedPathPrefixAuth.setJwkServiceIds(Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {}));
                    } catch (Exception e) {
                        logger.error("could not parse the jwkServiceIds json with a list of strings.", e);
                        throw new ConfigException("could not parse the jwkServiceIds json with a list of strings.");
                    }
                } else {
                    // comma separated
                    unifiedPathPrefixAuth.setJwkServiceIds(Arrays.asList(s.split("\\s*,\\s*")));
                }
            } else if(jwkIds instanceof List ) {
                // it must be a json array.
                unifiedPathPrefixAuth.setJwkServiceIds((List)jwkIds);
            }
            Object sjwkIds = value.get(SJWK_SERVICE_IDS);
            if(sjwkIds instanceof String) {
                String s = (String)value.get(SJWK_SERVICE_IDS);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        unifiedPathPrefixAuth.setSjwkServiceIds(Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {}));
                    } catch (Exception e) {
                        logger.error("could not parse the sjwkServiceIds json with a list of strings.", e);
                        throw new ConfigException("could not parse the sjwkServiceIds json with a list of strings.");
                    }
                } else {
                    // comma separated
                    unifiedPathPrefixAuth.setSjwkServiceIds(Arrays.asList(s.split("\\s*,\\s*")));
                }
            } else if(jwkIds instanceof List ) {
                // it must be a json array.
                unifiedPathPrefixAuth.setSjwkServiceIds((List)sjwkIds);
            }
            Object swtIds = value.get(SWT_SERVICE_IDS);
            if(swtIds instanceof String) {
                String s = (String)value.get(SWT_SERVICE_IDS);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        unifiedPathPrefixAuth.setSwtServiceIds(Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {}));
                    } catch (Exception e) {
                        logger.error("could not parse the swtServiceIds json with a list of strings.", e);
                        throw new ConfigException("could not parse the swtServiceIds json with a list of strings.");
                    }
                } else {
                    // comma separated
                    unifiedPathPrefixAuth.setSwtServiceIds(Arrays.asList(s.split("\\s*,\\s*")));
                }
            } else if(swtIds instanceof List ) {
                // it must be a json array.
                unifiedPathPrefixAuth.setSwtServiceIds((List)swtIds);
            }
            pathPrefixAuths.add(unifiedPathPrefixAuth);
        }
        return pathPrefixAuths;
    }
}
