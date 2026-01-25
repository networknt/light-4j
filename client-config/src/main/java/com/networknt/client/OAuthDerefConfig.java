package com.networknt.client;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.StringField;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthDerefConfig {

    @StringField(
            configFieldName = ClientConfig.SERVER_URL,
            externalizedKeyName = "derefServerUrl",
            description = "Token service server url, this might be different than the above token server url.\n" +
                    "The static url will be used if it is configured."
    )
    @JsonProperty(ClientConfig.SERVER_URL)
    private String serverUrl = null;

    @StringField(
            configFieldName = ClientConfig.PROXY_HOST,
            externalizedKeyName = "derefProxyHost",
            description = "For users who leverage SaaS OAuth 2.0 provider in the public cloud and has an internal\n" +
                    "proxy server to access code, token and key services of OAuth 2.0, set up the proxyHost\n" +
                    "here for the HTTPS traffic. This option is only working with server_url and serviceId\n" +
                    "below should be commented out. OAuth 2.0 services cannot be discovered if a proxy is used."
    )
    @JsonProperty(ClientConfig.PROXY_HOST)
    private String proxyHost = null;

    @IntegerField(
            configFieldName = ClientConfig.PROXY_PORT,
            externalizedKeyName = "derefProxyPort",
            description = "We only support HTTPS traffic for the proxy and the default port is 443. " +
                    "If your proxy server has\n" +
                    "a different port, please specify it here. " +
                    "If proxyHost is available and proxyPort is missing, then\n" +
                    "the default value 443 is going to be used for the HTTP connection."
    )
    @JsonProperty(ClientConfig.PROXY_PORT)
    private Integer proxyPort = null;

    @StringField(
            configFieldName = ClientConfig.SERVICE_ID,
            externalizedKeyName = "derefServiceId",
            defaultValue = "com.networknt.oauth2-token-1.0.0",
            description = "token service unique id for OAuth 2.0 provider. " +
                    "Need for service lookup/discovery. It will be used if above server_url is not configured."
    )
    @JsonProperty(ClientConfig.SERVICE_ID)
    private String serviceId = "com.networknt.oauth2-token-1.0.0";

    @BooleanField(
            configFieldName = ClientConfig.ENABLE_HTTP2,
            externalizedKeyName = "derefEnableHttp2",
            defaultValue = "true",
            description = "set to true if the oauth2 provider supports HTTP/2"
    )
    @JsonProperty(ClientConfig.ENABLE_HTTP2)
    private Boolean enableHttp2 = true;

    @StringField(
            configFieldName = ClientConfig.URI,
            externalizedKeyName = "derefUri",
            defaultValue = "/oauth2/deref",
            description = "the path for the key distribution endpoint"
    )
    @JsonProperty(ClientConfig.URI)
    private String uri = "/oauth2/deref";

    @StringField(
            configFieldName = ClientConfig.CLIENT_ID,
            externalizedKeyName = "derefClientId",
            defaultValue = "f7d42348-c647-4efb-a52d-4c5787421e72",
            description = "client_id used to access key distribution service. " +
                    "It can be the same client_id with token service or not."
    )
    @JsonProperty(ClientConfig.CLIENT_ID)
    private char[] clientId = "f7d42348-c647-4efb-a52d-4c5787421e72".toCharArray();

    @StringField(
            configFieldName = ClientConfig.CLIENT_SECRET,
            externalizedKeyName = "derefClientSecret",
            defaultValue = "f6h1FTI8Q3-7UScPZDzfXA",
            description = "client_secret for deref"
    )
    @JsonProperty(ClientConfig.CLIENT_SECRET)
    private char[] clientSecret = "f6h1FTI8Q3-7UScPZDzfXA".toCharArray();

    public String getServerUrl() {
        return serverUrl;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public String getServiceId() {
        return serviceId;
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
}
