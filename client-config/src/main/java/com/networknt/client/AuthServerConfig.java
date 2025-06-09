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
}
