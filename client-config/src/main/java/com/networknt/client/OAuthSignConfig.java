package com.networknt.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.ObjectField;
import com.networknt.config.schema.StringField;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthSignConfig {

    @StringField(
            configFieldName = ClientConfig.SERVER_URL,
            externalizedKeyName = "signServerUrl",
            externalized = true,
            description = "token server url. The default port number for token service is 6882. " +
                    "If this url exists, it will be used.\n" +
                    "if it is not set, then a service lookup against serviceId will be taken to discover an instance."
    )
    @JsonProperty(ClientConfig.SERVER_URL)
    private String serverUrl = null;

    @StringField(
            configFieldName = ClientConfig.PROXY_HOST,
            externalizedKeyName = "signProxyHost",
            externalized = true,
            description = "For users who leverage SaaS OAuth 2.0 provider from lightapi.net or others in the public cloud\n" +
                    "and has an internal proxy server to access code, token and key services of OAuth 2.0, set up the\n" +
                    "proxyHost here for the HTTPS traffic. This option is only working with server_url and serviceId\n" +
                    "below should be commented out. OAuth 2.0 services cannot be discovered if a proxy server is used."
    )
    @JsonProperty(ClientConfig.PROXY_HOST)
    private String proxyHost = null;

    @IntegerField(
            configFieldName = ClientConfig.PROXY_PORT,
            externalizedKeyName = "signProxyPort",
            min = 0,
            max = 65535,
            externalized = true,
            description = "We only support HTTPS traffic for the proxy and the default port is 443. If your proxy server has\n" +
                    "a different port, please specify it here. If proxyHost is available and proxyPort is missing, then\n" +
                    "the default value 443 is going to be used for the HTTP connection."
    )
    @JsonProperty(ClientConfig.PROXY_PORT)
    private Integer proxyPort = null;

    @StringField(
            configFieldName = ClientConfig.SERVICE_ID,
            externalizedKeyName = "signServiceId",
            externalized = true,
            defaultValue = "com.networknt.oauth2-token-1.0.0",
            description = "token serviceId. If server_url doesn't exist, " +
                    "the serviceId will be used to lookup the token service."
    )
    @JsonProperty(ClientConfig.SERVICE_ID)
    private String serviceId = "com.networknt.oauth2-token-1.0.0";

    @StringField(
            configFieldName = ClientConfig.URI,
            externalizedKeyName = "signUri",
            externalized = true,
            defaultValue = "/oauth2/token",
            description = "signing endpoint for the sign request"
    )
    @JsonProperty(ClientConfig.URI)
    private String uri = "/oauth2/sign";

    @IntegerField(
            configFieldName = ClientConfig.TIMEOUT,
            externalizedKeyName = "signTimeout",
            externalized = true,
            defaultValue = "2000",
            description = "timeout in milliseconds"
    )
    @JsonProperty(ClientConfig.TIMEOUT)
    private Integer timeout = 2000;

    @BooleanField(
            configFieldName = ClientConfig.ENABLE_HTTP_2,
            externalizedKeyName = "signEnableHttp2",
            defaultValue = "true",
            externalized = true,
            description = "set to true if the oauth2 provider supports HTTP/2"
    )
    @JsonProperty(ClientConfig.ENABLE_HTTP_2)
    private boolean enableHttp2 = true;

    @StringField(
            configFieldName = ClientConfig.CLIENT_ID,
            externalizedKeyName = "signClientId",
            externalized = true,
            defaultValue = "f7d42348-c647-4efb-a52d-4c5787421e72",
            description = "client_id for client authentication"
    )
    @JsonProperty(ClientConfig.CLIENT_ID)
    private char[] clientId =  "f7d42348-c647-4efb-a52d-4c5787421e72".toCharArray();

    @StringField(
            configFieldName = ClientConfig.CLIENT_SECRET,
            externalizedKeyName = "signClientSecret",
            externalized = true,
            defaultValue = "f6h1FTI8Q3-7UScPZDzfXA",
            description = "client secret for client authentication and it can be encrypted here."
    )
    @JsonProperty(ClientConfig.CLIENT_SECRET)
    private char[] clientSecret = "f6h1FTI8Q3-7UScPZDzfXA".toCharArray();

    @ObjectField(
            configFieldName = ClientConfig.KEY,
            ref = OAuthSignKeyConfig.class,
            useSubObjectDefault = true,
            description = "the key distribution sever config for sign. " +
                    "It can be different then token key distribution server."
    )
    @JsonProperty(ClientConfig.KEY)
    OAuthSignKeyConfig key = null;

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

    public String getUri() {
        return uri;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public char[] getClientId() {
        return clientId;
    }

    public char[] getClientSecret() {
        return clientSecret;
    }

    public OAuthSignKeyConfig getKey() {
        return key;
    }
}
