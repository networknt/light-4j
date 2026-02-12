package com.networknt.token.exchange.schema;

import com.fasterxml.jackson.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared variable object used to store and read different variables used in the request, source and update schemas.
 * Uses a flexible Map-based approach to support any variable name without requiring field definitions.
 * All variables are stored in a single map for uniform access.
 */
public class SharedVariableSchema {

    private static final Logger LOG = LoggerFactory.getLogger(SharedVariableSchema.class);

    public static final String ACCESS_TOKEN = "accessToken";
    public static final String CONSTRUCTED_JWT = "constructedJwt";
    public static final String TOKEN_TTL = "tokenTtl";
    public static final String TOKEN_TTL_UNIT = "tokenTtlUnit";
    public static final String WAIT_LENGTH = "waitLength";
    public static final String EXPIRATION = "expiration";

    // All variables stored in a flexible map
    @JsonIgnore
    private final Map<String, Object> variables = new HashMap<>();

    public SharedVariableSchema() {
        // Initialize defaults
        variables.put(TOKEN_TTL, 0L);
        variables.put(TOKEN_TTL_UNIT, TtlUnit.SECOND);
        variables.put(WAIT_LENGTH, 0L);
        variables.put(EXPIRATION, 0L);
    }

    @JsonAnySetter
    public void setVariable(String name, Object value) {
        // Handle special type conversions for known fields
        if (TOKEN_TTL_UNIT.equals(name) && value != null) {
            if (value instanceof String) {
                variables.put(name, TtlUnit.valueOf((String) value));
            } else {
                variables.put(name, value);
            }
        } else if ((TOKEN_TTL.equals(name) || WAIT_LENGTH.equals(name) || EXPIRATION.equals(name)) && value != null) {
            if (value instanceof Number) {
                variables.put(name, ((Number) value).longValue());
            } else {
                variables.put(name, Long.parseLong(String.valueOf(value)));
            }
        } else {
            variables.put(name, value);
        }
    }

    @JsonAnyGetter
    public Map<String, Object> getVariables() {
        return variables;
    }

    /**
     * Gets a variable value by name.
     */
    public Object get(String name) {
        return variables.get(name);
    }

    /**
     * Sets a variable value by name.
     */
    public void set(String name, Object value) {
        setVariable(name, value);
    }

    /**
     * Returns all variables as a Map for use with VariableResolver.
     */
    public Map<String, Object> asMap() {
        return new HashMap<>(variables);
    }

//    // Convenience accessors for commonly used fields
//    public String getAccessToken() {
//        return (String) variables.get(ACCESS_TOKEN);
//    }
//
//    public void setAccessToken(String accessToken) {
//        variables.put(ACCESS_TOKEN, accessToken);
//    }
//
//    public String getConstructedJwt() {
//        return (String) variables.get(CONSTRUCTED_JWT);
//    }
//
//    public void setConstructedJwt(String constructedJwt) {
//        variables.put(CONSTRUCTED_JWT, constructedJwt);
//    }

    public long getTokenTtl() {
        Object value = variables.get(TOKEN_TTL);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }
    public TtlUnit getTokenTtlUnit() {
        Object value = variables.get(TOKEN_TTL_UNIT);
        if (value instanceof TtlUnit) {
            return (TtlUnit) value;
        } else if (value instanceof String) {
            return TtlUnit.valueOf((String) value);
        }
        return TtlUnit.SECOND;
    }

    public long getWaitLength() {
        Object value = variables.get(WAIT_LENGTH);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    public long getExpiration() {
        Object value = variables.get(EXPIRATION);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    public void setExpiration(long expiration) {
        variables.put(EXPIRATION, expiration);
    }

    /**
     * Update the expiration of the token based on the configured ttl.
     * Converts ttl to milliseconds first before storing.
     */
    public void updateExpiration() {
        if (this.getTokenTtl() == 0) {
            LOG.warn("Token ttl is either not defined or is set to 0, a new token will be requested every time!");
        }
        final var ttlInMillis = this.getTokenTtlUnit().unitToMillis(this.getTokenTtl());
        final var newExpiration = System.currentTimeMillis() + ttlInMillis;
        this.setExpiration(newExpiration);
    }
}
