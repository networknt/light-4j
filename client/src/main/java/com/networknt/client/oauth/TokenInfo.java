package com.networknt.client.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This is the class the returned from the token introspection. It contains all the token info returned from the
 * OAuth server token introspection endpoint to assist SwtVerifierHandler in light-rest-4j.
 *
 * @author Steve Hu
 */
public class TokenInfo {
    boolean active;
    String clientId;
    String tokenType;
    String scope;
    String sub;
    long exp;
    long iat;
    String iss;
    String error;
    String errorDescription;


    public TokenInfo() {
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @JsonProperty("client_id")
    public String getClientId() {
        return clientId;
    }

    @JsonProperty("client_id")
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @JsonProperty("token_type")
    public String getTokenType() {
        return tokenType;
    }

    @JsonProperty("token_type")
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public long getExp() {
        return exp;
    }

    public void setExp(String exp) {
        this.exp = Long.valueOf(exp);
    }

    public void setExp(long exp) {
        this.exp = exp;
    }

    public long getIat() {
        return iat;
    }

    public void setIat(String iat) {
        this.iat = Long.valueOf(iat);
    }
    public void setIat(long iat) {
        this.iat = iat;
    }

    public String getIss() {
        return iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @JsonProperty("error_description")
    public String getErrorDescription() {
        return errorDescription;
    }

    @JsonProperty("error_description")
    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }
}
