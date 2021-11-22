package com.networknt.rule;

public class RuleLoaderConfig {
    public static final String CONFIG_NAME = "rule-loader";
    String portalHost;

    private RuleLoaderConfig() {
    }

    public String getPortalHost() {
        return portalHost;
    }

    public void setPortalHost(String portalHost) {
        this.portalHost = portalHost;
    }
}
