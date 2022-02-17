package com.networknt.rule;

public class RuleLoaderConfig {
    public static final String CONFIG_NAME = "rule-loader";
    String portalHost;
    String portalToken;

    private RuleLoaderConfig() {
    }

    public String getPortalHost() {
        return portalHost;
    }

    public void setPortalHost(String portalHost) {
        this.portalHost = portalHost;
    }

    public String getPortalToken() {
        return portalToken;
    }

    public void setPortalToken(String portalToken) {
        this.portalToken = portalToken;
    }
}
