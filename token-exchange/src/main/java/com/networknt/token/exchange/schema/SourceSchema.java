package com.networknt.token.exchange.schema;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.networknt.config.JsonMapper;
import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.ObjectField;
import com.networknt.token.exchange.VariableResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SourceSchema {

    private static final Logger LOG = LoggerFactory.getLogger(SourceSchema.class);

    private static final String HEADERS_FIELD = "headers";
    private static final String BODY_FIELD = "body";
    private static final String EXPIRATION_SCHEMA = "expirationSchema";

    @ArrayField(
            configFieldName = HEADERS_FIELD,
            description = "Describes what headers will be parsed, and where the data will be saved.",
            items = SourceDestinationDefinition.class
    )
    @JsonProperty(HEADERS_FIELD)
    private List<SourceDestinationDefinition> headers;


    @ArrayField(
            configFieldName = BODY_FIELD,
            description = "Describes what parts of the body to parse, and where the data will be saved.",
            items = SourceDestinationDefinition.class
    )
    @JsonProperty(BODY_FIELD)
    private List<SourceDestinationDefinition> body;

    @ObjectField(
            configFieldName = EXPIRATION_SCHEMA,
            description = "Describes where the expiration field will go."
    )
    @JsonProperty(EXPIRATION_SCHEMA)
    private ExpirationSchema expirationSchema;

    public List<SourceDestinationDefinition> getHeaders() {
        return headers;
    }

    public List<SourceDestinationDefinition> getBody() {
        return body;
    }

    public ExpirationSchema getExpirationSchema() {
        return expirationSchema;
    }

    public void setHeaders(List<SourceDestinationDefinition> headers) {
        this.headers = headers;
    }

    public void setBody(List<SourceDestinationDefinition> body) {
        this.body = body;
    }

    /**
     * Writes values from HTTP response to shared variables based on source-destination mappings.
     */
    public void writeResponseToSharedVariables(final SharedVariableSchema sharedVariables, final HttpResponse<?> response) {
        // Write header values
        if (this.headers != null && !this.headers.isEmpty()) {
            writeHeadersToSharedVariables(sharedVariables, response);
        }

        // Write body values
        if (this.body != null && !this.body.isEmpty()) {
            writeBodyToSharedVariables(sharedVariables, response.body().toString());
        }

        // Write expiration if configured
        if (this.expirationSchema != null) {
            writeExpirationToSharedVariables(sharedVariables, response);
        }
    }

    private void writeHeadersToSharedVariables(final SharedVariableSchema sharedVariables, final HttpResponse<?> response) {
        final var headerMap = new HashMap<String, Object>();

        for (var header : response.headers().map().entrySet()) {
            if (!header.getValue().isEmpty()) {
                final var value = String.join(",", header.getValue());
                if (!value.isEmpty()) {
                    headerMap.put(header.getKey(), value);
                }
            }
        }

        writeToSharedVariables(sharedVariables, headerMap, this.headers);
    }

    private void writeBodyToSharedVariables(final SharedVariableSchema sharedVariables, final String jsonBody) {
        final var bodyMap = JsonMapper.string2Map(jsonBody);
        writeToSharedVariables(sharedVariables, bodyMap, this.body);
    }

    private void writeExpirationToSharedVariables(final SharedVariableSchema sharedVariables, final HttpResponse<?> response) {
        switch (this.expirationSchema.location) {
            case HEADER:
                final var headerValue = response.headers().map().get(this.expirationSchema.field);
                if (headerValue != null && !headerValue.isEmpty()) {
                    final var expiration = Long.parseLong(headerValue.getFirst());
                    sharedVariables.setExpiration(this.expirationSchema.ttlUnit.unitToMillis(expiration));
                } else {
                    LOG.error("Could not find '{}' in response headers", this.expirationSchema.field);
                }
                break;

            case BODY:
                final var bodyMap = JsonMapper.string2Map(response.body().toString());
                if (bodyMap.containsKey(this.expirationSchema.field)) {
                    final var expiration = Long.parseLong(String.valueOf(bodyMap.get(this.expirationSchema.field)));
                    sharedVariables.setExpiration(this.expirationSchema.ttlUnit.unitToMillis(expiration));
                } else {
                    LOG.error("Could not find '{}' in response body", this.expirationSchema.field);
                }
                break;

            default:
                throw new IllegalStateException("Invalid expiration location: " + this.expirationSchema.location);
        }
    }

    /**
     * Writes source values to shared variables based on source-destination mappings.
     */
    private static void writeToSharedVariables(
            final SharedVariableSchema sharedVariables,
            final Map<String, Object> sourceData,
            final List<SourceDestinationDefinition> mappings
    ) {
        if (mappings == null || sourceData == null) {
            return;
        }

        for (final var mapping : mappings) {
            final var sourceValue = sourceData.get(mapping.getSource());
            if (sourceValue != null) {
                final var variableName = VariableResolver.extractDestinationVariable(mapping.getDestination());
                sharedVariables.set(variableName, sourceValue);
                LOG.trace("Set '{}' = '{}'", variableName, sourceValue);
            }
        }
    }

    public static class ExpirationSchema {

        public enum ExpireLocation {

            @JsonProperty("HEADER")
            @JsonAlias({"header", "Header"})
            HEADER,

            @JsonProperty("BODY")
            @JsonAlias({"body"})
            BODY
        }

        @JsonProperty("location")
        private ExpireLocation location;

        @JsonProperty("field")
        private String field;

        @JsonProperty("ttlUnit")
        @JsonSetter(nulls = Nulls.SKIP)
        private TtlUnit ttlUnit = TtlUnit.SECOND;

        public ExpireLocation getLocation() {
            return location;
        }

        public String getField() {
            return field;
        }

        public TtlUnit getTtlUnit() {
            return ttlUnit;
        }
    }

    public static class SourceDestinationDefinition {

        @JsonProperty("source")
        private String source;

        @JsonProperty("destination")
        private String destination;

        public String getSource() {
            return source;
        }

        public String getDestination() {
            return destination;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }
    }
}
