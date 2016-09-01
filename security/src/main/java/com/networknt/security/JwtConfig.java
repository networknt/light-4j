package com.networknt.security;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by steve on 01/09/16.
 */
public class JwtConfig {
    String issuer;
    String audience;
    String version;
    int expiredInMinutes;

    @JsonIgnore
    String description;

    public JwtConfig() {
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getExpiredInMinutes() {
        return expiredInMinutes;
    }

    public void setExpiredInMinutes(int expiredInMinutes) {
        this.expiredInMinutes = expiredInMinutes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
