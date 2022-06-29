package com.networknt.rule;

public class RuleLoaderConfig {
    public static final String CONFIG_NAME = "rule-loader";
    public static final String RULE_SOURCE_LIGHT_PORTAL = "light-portal";
    public static final String RULE_SOURCE_CONFIG_FOLDER = "config-folder";

    boolean enabled;
    String ruleSource;
    String portalHost;
    String portalToken;

    private RuleLoaderConfig() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRuleSource() {
        return ruleSource;
    }

    public void setRuleSource(String ruleSource) {
        this.ruleSource = ruleSource;
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
