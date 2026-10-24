package com.networknt.security;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.node.ObjectNode;
import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.MapField;
import com.networknt.config.schema.StringField;

import java.util.LinkedHashMap;
import java.util.Map;

public class SecurityJwtConfig {

    private static final String CLOCK_SKEW_IN_SECONDS = "clockSkewInSeconds";
    private static final String KEY_RESOLVER = "keyResolver";
    private static final String CERTIFICATE = "certificate";

    // NOTE: This field can be deserialized as a string or a map.
    // This is to keep backwards compatibility
    @MapField(
            configFieldName = CERTIFICATE,
            externalizedKeyName = CERTIFICATE,
            defaultValue = "100=primary.crt&101=secondary.crt",
            description =
                    " '100': primary.crt\n" +
                    " '101': secondary.crt\n",
            valueType = String.class
    )
    @JsonDeserialize(using = CertificateDeserializer.class)
    private Map<String, Object> certificate;

    @IntegerField(
            configFieldName = CLOCK_SKEW_IN_SECONDS,
            externalizedKeyName = CLOCK_SKEW_IN_SECONDS,
            defaultValue = "60"
    )
    private int clockSkewInSeconds;

    @StringField(
            configFieldName = KEY_RESOLVER,
            externalizedKeyName = KEY_RESOLVER,
            defaultValue = "JsonWebKeySet",
            description = "Key distribution server standard: JsonWebKeySet for other OAuth 2.0 provider| X509Certificate for light-oauth2"
    )
    private String keyResolver;

    public int getClockSkewInSeconds() {
        return clockSkewInSeconds;
    }

    public Map<String, Object> getCertificate() {
        return certificate;
    }

    public String getKeyResolver() {
        return keyResolver;
    }

    /**
     * This is used to resolve the type of field for certificates. Either in map or string format.
     */
    private static class CertificateDeserializer extends ValueDeserializer<Map<String, Object>> {

        @Override
        public Map<String, Object> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws JacksonException {
            final JsonNode root = deserializationContext.readTree(jsonParser);
            if (root.isObject())
                return deserializationContext.readTreeAsValue(root, deserializationContext.getTypeFactory()
                        .constructMapType(Map.class, String.class, Object.class));
            else if (root.isString()) {
                String s = root.asString();
                Map<String, Object> map = new LinkedHashMap<>();
                for(String keyValue : s.split(" *& *")) {
                    String[] pairs = keyValue.split(" *= *", 2);
                    map.put(pairs[0], pairs.length == 1 ? "" : pairs[1]);
                }
                return map;
            }
            return Map.of();
        }
    }
}
