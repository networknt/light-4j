package com.networknt.token.exchange.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.MapField;
import com.networknt.token.exchange.RequestContext;
import com.networknt.token.exchange.VariableResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class UpdateSchema {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateSchema.class);

    private static final String HEADERS = "headers";
    private static final String BODY = "body";
    private static final String UPDATE_EXPIRATION_FROM_TTL = "updateExpirationFromTtl";

    @MapField(configFieldName = HEADERS, valueType = String.class)
    @JsonProperty(HEADERS)
    private Map<String, String> headers;

    @MapField(configFieldName = BODY, valueType = String.class)
    @JsonProperty(BODY)
    private Map<String, String> body;

    @BooleanField(configFieldName = UPDATE_EXPIRATION_FROM_TTL)
    @JsonProperty(UPDATE_EXPIRATION_FROM_TTL)
    @JsonSetter(nulls = Nulls.SKIP)
    private boolean updateExpirationFromTtl = true;

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getBody() {
        return body;
    }

    public void setBody(Map<String, String> body) {
        this.body = body;
    }

    public boolean isUpdateExpirationFromTtl() {
        return updateExpirationFromTtl;
    }

    public Map<String, String> getResolvedHeaders(final SharedVariableSchema sharedVariableSchema) {
        return getResolvedHeaders(sharedVariableSchema, null);
    }

    public Map<String, String> getResolvedHeaders(final SharedVariableSchema sharedVariableSchema, final RequestContext requestContext) {
        if (this.headers == null) {
            LOG.trace("No headers defined in update schema.");
            return new HashMap<>();
        }
        return VariableResolver.resolveMap(this.headers, sharedVariableSchema.asMap(), requestContext);
    }

    public Map<String, String> getResolvedBody(final SharedVariableSchema sharedVariableSchema) {
        return getResolvedBody(sharedVariableSchema, null);
    }

    public Map<String, String> getResolvedBody(final SharedVariableSchema sharedVariableSchema, final RequestContext requestContext) {
        if (this.body == null) {
            LOG.trace("No body defined in update schema.");
            return new HashMap<>();
        }
        return VariableResolver.resolveMap(this.body, sharedVariableSchema.asMap(), requestContext);
    }
}
