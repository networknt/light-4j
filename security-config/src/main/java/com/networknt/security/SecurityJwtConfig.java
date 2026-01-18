package com.networknt.security;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.MapField;
import com.networknt.config.schema.StringField;

import java.io.IOException;
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
    private static class CertificateDeserializer extends JsonDeserializer<Map<String, Object>> {

        @Override
        public Map<String, Object> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
            final var mapper = (ObjectMapper) jsonParser.getCodec();
            final JsonNode root = mapper.readTree(jsonParser);
            if (root.isObject())
                return mapper.convertValue(root, new TypeReference<>(){});
            else if (root.isTextual()) {
                String s = mapper.convertValue(root, String.class);
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
