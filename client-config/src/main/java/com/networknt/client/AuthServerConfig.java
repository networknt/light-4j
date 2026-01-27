package com.networknt.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.StringField;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthServerConfig {

    @StringField(
            configFieldName = "server_url"
    )
    @JsonProperty("server_url")
    private String serverUrl = null;

    @StringField(
            configFieldName = "proxyHost"
    )
    private String proxyHost = null;

    @IntegerField(
            configFieldName = "proxyPort",
            min = 0,
            max = 65535
    )
    private Integer proxyPort = null;

    @BooleanField(
            configFieldName = "enableHttp2"
    )
    private Boolean enableHttp2 = null;

    @StringField(
            configFieldName = "uri"
    )
    private String uri = null;

    @StringField(
            configFieldName = "client_id"
    )
    @JsonProperty("client_id")
    private char[] clientId = null;

    @StringField(
            configFieldName = "client_secret"
    )
    @JsonProperty("client_secret")
    private char[] clientSecret = null;

    @ArrayField(
            configFieldName = "scope",
            items = String.class
    )
    private List<String> scope = null;

    @StringField(
            configFieldName = "audience"
    )
    private String audience = null;

    private Integer tokenRenewBeforeExpired = 60000;

    private Integer expiredRefreshRetryDelay = 2000;

    private Integer earlyRefreshRetryDelay = 4000;

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }

    public void setEnableHttp2(Boolean enableHttp2) {
        this.enableHttp2 = enableHttp2;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setClientId(char[] clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(char[] clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public Boolean isEnableHttp2() {
        return enableHttp2;
    }

    public String getUri() {
        return uri;
    }

    public char[] getClientId() {
        return clientId;
    }

    public char[] getClientSecret() {
        return clientSecret;
    }

    public List<String> getScope() {
        return scope;
    }

    public String getAudience() {
        return audience;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public Integer getTokenRenewBeforeExpired() {
        return tokenRenewBeforeExpired;
    }

    public void setTokenRenewBeforeExpired(Integer tokenRenewBeforeExpired) {
        this.tokenRenewBeforeExpired = tokenRenewBeforeExpired;
    }

    public Integer getExpiredRefreshRetryDelay() {
        return expiredRefreshRetryDelay;
    }

    public void setExpiredRefreshRetryDelay(Integer expiredRefreshRetryDelay) {
        this.expiredRefreshRetryDelay = expiredRefreshRetryDelay;
    }

    public Integer getEarlyRefreshRetryDelay() {
        return earlyRefreshRetryDelay;
    }

    public void setEarlyRefreshRetryDelay(Integer earlyRefreshRetryDelay) {
        this.earlyRefreshRetryDelay = earlyRefreshRetryDelay;
    }
}
