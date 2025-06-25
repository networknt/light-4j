package com.networknt.reqtrans;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import com.networknt.config.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This is a generic middleware handler to manipulate request based on rule-engine rules so that it can be much more
 * flexible than any other handlers like the header handler to manipulate the headers. The rules will be loaded from
 * the configuration or from the light-portal if portal is implemented.
 *
 * @author Steve Hu
 */
@ConfigSchema(configKey = "request-transformer", configName = "request-transformer", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML})
public class RequestTransformerConfig {
    public static final String CONFIG_NAME = "request-transformer";
    private static final Logger logger = LoggerFactory.getLogger(RequestTransformerConfig.class);

    private static final String ENABLED = "enabled";
    private static final String REQUIRED_CONTENT = "requiredContent";
    private static final String DEFAULT_BODY_ENCODING = "defaultBodyEncoding";
    private static final String APPLIED_PATH_PREFIXES = "appliedPathPrefixes";
    private static final String PATH_PREFIX_ENCODING = "pathPrefixEncoding";

    private Map<String, Object> mappedConfig;
    private final Config config;

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            externalized = true,
            defaultValue = "true",
            description = "indicate if this interceptor is enabled or not."
    )
    private boolean enabled;

    @BooleanField(
            configFieldName = REQUIRED_CONTENT,
            externalizedKeyName = REQUIRED_CONTENT,
            externalized = true,
            defaultValue = "true",
            description = ""
    )
    private boolean requiredContent;

    @StringField(
            configFieldName = DEFAULT_BODY_ENCODING,
            externalizedKeyName = DEFAULT_BODY_ENCODING,
            externalized = true,
            defaultValue = "UTF-8",
            description = "default body encoding for the request body. The default value is UTF-8. Other options is ISO-8859-1."
    )
    private String defaultBodyEncoding;

    @ArrayField(
            configFieldName = APPLIED_PATH_PREFIXES,
            externalizedKeyName = APPLIED_PATH_PREFIXES,
            externalized = true,
            description = "A list of applied request path prefixes, other requests will skip this handler. The value can be a string\n" +
                    "if there is only one request path prefix needs this handler. or a list of strings if there are multiple.",
            items = String.class
    )
    List<String> appliedPathPrefixes;

    @MapField(
            configFieldName = PATH_PREFIX_ENCODING,
            externalizedKeyName = PATH_PREFIX_ENCODING,
            externalized = true,
            description = "For certain path prefixes that are not using the defaultBodyEncoding UTF-8, you can define the customized\n" +
                    "encoding like ISO-8859-1 for the path prefixes here. This is only for the legacy APIs that can only accept\n" +
                    "ISO-8859-1 request body but the consumer is sending the request in UTF-8 as it is standard on the Web.\n" +
                    "pathPrefixEncoding:\n" +
                    "  /v1/pets: ISO-8859-1\n" +
                    "  /v1/party/info: ISO-8859-1",
            valueType = String.class
    )
    Map<String, Object> pathPrefixEncoding;

    private RequestTransformerConfig() {
        this(CONFIG_NAME);
    }

    private RequestTransformerConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
        setConfigMap();
    }

    public static RequestTransformerConfig load() {
        return new RequestTransformerConfig();
    }

    public static RequestTransformerConfig load(String configName) {
        return new RequestTransformerConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setConfigList();
        setConfigMap();
    }


    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRequiredContent() { return requiredContent; }

    public String getDefaultBodyEncoding() {
        return defaultBodyEncoding;
    }

    public List<String> getAppliedPathPrefixes() {
        return appliedPathPrefixes;
    }

    public Map<String, Object> getPathPrefixEncoding() { return pathPrefixEncoding; }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
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
        object = mappedConfig.get(REQUIRED_CONTENT);
        if(object != null) {
            if(object instanceof String) {
                requiredContent = Boolean.parseBoolean((String)object);
            } else if (object instanceof Boolean) {
                requiredContent = (Boolean) object;
            } else {
                throw new ConfigException("requiredContent must be a boolean value.");
            }
        }
        object = mappedConfig.get(DEFAULT_BODY_ENCODING);
        if (object != null) defaultBodyEncoding = (String) object;
    }

    private void setConfigList() {
        if (mappedConfig.get(APPLIED_PATH_PREFIXES) != null) {
            Object object = mappedConfig.get(APPLIED_PATH_PREFIXES);
            appliedPathPrefixes = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        appliedPathPrefixes = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the appliedPathPrefixes json with a list of strings.");
                    }
                } else {
                    // comma separated
                    appliedPathPrefixes = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                List prefixes = (List)object;
                prefixes.forEach(item -> {
                    appliedPathPrefixes.add((String)item);
                });
            } else {
                throw new ConfigException("appliedPathPrefixes must be a string or a list of strings.");
            }
        }
    }

    private void  setConfigMap() {
        if(mappedConfig.get(PATH_PREFIX_ENCODING) != null) {
            Object pathPrefixEncodingObj = mappedConfig.get(PATH_PREFIX_ENCODING);
            if(pathPrefixEncodingObj != null) {
                if(pathPrefixEncodingObj instanceof String) {
                    String s = (String)pathPrefixEncodingObj;
                    s = s.trim();
                    if(logger.isTraceEnabled()) logger.trace("pathPrefixEncoding s = {}", s);
                    if(s.startsWith("{")) {
                        // json format
                        try {
                            pathPrefixEncoding = JsonMapper.string2Map(s);
                        } catch (Exception e) {
                            throw new ConfigException("could not parse the pathPrefixEncoding json with a map of string and object.");
                        }
                    } else {
                        // comma separated
                        pathPrefixEncoding = new HashMap<>();
                        String[] pairs = s.split(",");
                        for (int i = 0; i < pairs.length; i++) {
                            String pair = pairs[i];
                            String[] keyValue = pair.split(":");
                            pathPrefixEncoding.put(keyValue[0], keyValue[1]);
                        }
                    }
                } else if (pathPrefixEncodingObj instanceof Map) {
                    pathPrefixEncoding = (Map)pathPrefixEncodingObj;
                } else {
                    throw new ConfigException("pathPrefixEncoding must be a string object map.");
                }
            }
        }
    }
}
