package com.networknt.client;

import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.ObjectField;
import com.networknt.config.schema.StringField;

public class OauthSignConfig {

    public static final String SERVER_URL = "server_url";
    public static final String PROXY_HOST = "proxyHost";
    public static final String PROXY_PORT = "proxyPort";
    public static final String SERVICE_ID = "serviceId";
    public static final String URI = "uri";
    public static final String TIMEOUT = "timeout";
    public static final String ENABLE_HTTP_2 = "enableHttp2";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String KEY = "key";

    @StringField(
            configFieldName = SERVER_URL,
            externalizedKeyName = "signServerUrl",
            externalized = true,
            description = "token server url. The default port number for token service is 6882. " +
                    "If this url exists, it will be used.\n" +
                    "if it is not set, then a service lookup against serviceId will be taken to discover an instance."
    )
    private String server_url;

    @StringField(
            configFieldName = PROXY_HOST,
            externalizedKeyName = "signProxyHost",
            externalized = true,
            description = "For users who leverage SaaS OAuth 2.0 provider from lightapi.net or others in the public cloud\n" +
                    "and has an internal proxy server to access code, token and key services of OAuth 2.0, set up the\n" +
                    "proxyHost here for the HTTPS traffic. This option is only working with server_url and serviceId\n" +
                    "below should be commented out. OAuth 2.0 services cannot be discovered if a proxy server is used."
    )
    private String proxyHost;

    @IntegerField(
            configFieldName = PROXY_PORT,
            externalizedKeyName = "signProxyPort",
            externalized = true,
            description = "We only support HTTPS traffic for the proxy and the default port is 443. If your proxy server has\n" +
                    "a different port, please specify it here. If proxyHost is available and proxyPort is missing, then\n" +
                    "the default value 443 is going to be used for the HTTP connection."
    )
    private int proxyPort;

    @StringField(
            configFieldName = SERVICE_ID,
            externalizedKeyName = "signServiceId",
            externalized = true,
            defaultValue = "com.networknt.oauth2-token-1.0.0",
            description = "token serviceId. If server_url doesn't exist, " +
                    "the serviceId will be used to lookup the token service."
    )
    private String serviceId;

    @StringField(
            configFieldName = URI,
            externalizedKeyName = "signUri",
            externalized = true,
            defaultValue = "/oauth2/token",
            description = "signing endpoint for the sign request"
    )
    private String uri;

    @IntegerField(
            configFieldName = TIMEOUT,
            externalizedKeyName = "signTimeout",
            externalized = true,
            defaultValue = 2000,
            description = "timeout in milliseconds"
    )
    private int timeout;

    @BooleanField(
            configFieldName = ENABLE_HTTP_2,
            externalizedKeyName = "signEnableHttp2",
            defaultValue = true,
            externalized = true,
            description = "set to true if the oauth2 provider supports HTTP/2"
    )
    private boolean enableHttp2;

    @StringField(
            configFieldName = CLIENT_ID,
            externalizedKeyName = "signClientId",
            externalized = true,
            defaultValue = "f7d42348-c647-4efb-a52d-4c5787421e72",
            description = "client_id for client authentication"
    )
    private char[] client_id;

    @StringField(
            configFieldName = CLIENT_SECRET,
            externalizedKeyName = "signClientSecret",
            externalized = true,
            defaultValue = "f6h1FTI8Q3-7UScPZDzfXA",
            description = "client secret for client authentication and it can be encrypted here."
    )
    private char[] client_secret;

    @ObjectField(
            configFieldName = KEY,
            ref = OAuthSignKeyConfig.class,
            useSubObjectDefault = true,
            description = "the key distribution sever config for sign. " +
                    "It can be different then token key distribution server."
    )
    OAuthSignKeyConfig key;

    public String getServer_url() {
        return server_url;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getUri() {
        return uri;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public char[] getClient_id() {
        return client_id;
    }

    public char[] getClient_secret() {
        return client_secret;
    }

    public OAuthSignKeyConfig getKey() {
        return key;
    }
}
