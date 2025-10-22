package com.networknt.header;


import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.Config;
import com.networknt.config.schema.*;

import java.io.IOException;
import java.util.*;

@ConfigSchema(configKey = "header", configName = "header", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML})
public class HeaderConfig {
    public static final String CONFIG_NAME = "header";
    public static final String ENABLED = "enabled";
    public static final String REQUEST = "request";
    public static final String RESPONSE = "response";
    public static final String PATH_PREFIX_HEADER = "pathPrefixHeader";

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            externalized = true,
            description = "Enable header handler or not. The default to false and it can be enabled in the externalized\n" +
                    "values.yml file. It is mostly used in the http-sidecar, light-proxy or light-router.",
            defaultValue = "false"
    )
    boolean enabled;

    @ObjectField(
            configFieldName = "request",
            description = "Request header manipulation",
            useSubObjectDefault = true,
            ref = HeaderRequestConfig.class
    )
    HeaderRequestConfig request;

    @ObjectField(
            configFieldName = "response",
            description = "Response header manipulation",
            useSubObjectDefault = true,
            ref = HeaderResponseConfig.class
    )
    HeaderResponseConfig response;

    @MapField(
            configFieldName = "pathPrefixHeader",
            externalizedKeyName = "pathPrefixHeader",
            externalized = true,
            description = "requestPath specific header configuration. The entire object is a map with path prefix as the\n" +
                    "key and request/response like above as the value. For config format, please refer to test folder.",
            valueType = HeaderPathPrefixConfig.class
    )
    Map<String, HeaderPathPrefixConfig> pathPrefixHeader;

    private Config config;
    private Map<String, Object> mappedConfig;

    private HeaderConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private HeaderConfig(String configName) {
        this.config = Config.getInstance();
        this.mappedConfig = this.config.getJsonMapConfigNoCache(configName);
        if (this.mappedConfig != null) {
            this.setValues();
        }
    }

    public static HeaderConfig load() {
        return new HeaderConfig();
    }

    public static HeaderConfig load(String configName) {
        return new HeaderConfig(configName);
    }

    void reload() {
        this.mappedConfig = this.config.getJsonMapConfigNoCache(CONFIG_NAME);
        if (this.mappedConfig != null) {
            this.setValues();
        }
    }

    private void setValues() {
        final var mapper = Config.getInstance().getMapper();
        if (this.mappedConfig.get(ENABLED) != null) {
            this.enabled = Config.loadBooleanValue(ENABLED, this.mappedConfig.get(ENABLED));
        }
        if (this.mappedConfig.get(REQUEST) instanceof Map) {
            this.request = mapper.convertValue(mappedConfig.get(REQUEST), HeaderRequestConfig.class);
        }

        if (this.mappedConfig.get(RESPONSE) instanceof Map) {
            this.response = mapper.convertValue(mappedConfig.get(RESPONSE), HeaderResponseConfig.class);
        }

        if (this.mappedConfig.get(PATH_PREFIX_HEADER) instanceof Map) {
            this.pathPrefixHeader = mapper.convertValue(mappedConfig.get(PATH_PREFIX_HEADER), new TypeReference<>(){});
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, HeaderPathPrefixConfig> getPathPrefixHeader() {
        return pathPrefixHeader;
    }

    public List<String> getRequestRemoveList() {
        return this.request.getRemove();
    }

    public Map<String, String> getRequestUpdateMap() {
        return this.request.getUpdate();
    }

    public List<String> getResponseRemoveList() {
        return this.response.getRemove();
    }

    public Map<String, String> getResponseUpdateMap() {
        return this.response.getUpdate();
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    /**
     * This is used to resolve the type field for remove headers. Either in list or string format.
     */
    protected static class HeaderRemoveDeserializer extends JsonDeserializer<List<String>> {

        @Override
        public List<String> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
            final var mapper = (ObjectMapper) jsonParser.getCodec();
            final JsonNode root = mapper.readTree(jsonParser);

            if (root.isArray()) {
                return mapper.convertValue(root, new TypeReference<>() {});

            } else if (root.isTextual()) {
                final var s = mapper.convertValue(root, String.class);
                final List<String> list = new ArrayList<>();
                for (String header : s.split(",")) {
                    list.add(header.trim());
                }
                return list;
            }
            return List.of();
        }
    }

    /**
     * This is used to resolve the type of field for update headers. Either in map or string format.
     */
    protected static class HeaderUpdateDeserializer extends JsonDeserializer<Map<String, String>> {

        @Override
        public Map<String, String> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
            final var mapper = (ObjectMapper) jsonParser.getCodec();
            final JsonNode root = mapper.readTree(jsonParser);
            if (root.isObject())
                return mapper.convertValue(root, new TypeReference<>(){});
            else if (root.isTextual()) {
                String s = mapper.convertValue(root, String.class);
                Map<String, String> map = new LinkedHashMap<>();
                for(String keyValue : s.split(",")) {
                    String[] pairs = keyValue.split(":", 2);
                    map.put(pairs[0], pairs.length == 1 ? "" : pairs[1]);
                }
                return map;
            }
            return Map.of();
        }
    }
}
