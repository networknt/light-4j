package com.networknt.client;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.StringField;

public class OAuthDerefConfig {

    public static final String SERVER_URL = "server_url";
    public static final String PROXY_HOST = "proxyHost";
    public static final String PROXY_PORT = "proxyPort";
    public static final String SERVICE_ID = "serviceId";
    public static final String ENABLE_HTTP2 = "enableHttp2";
    public static final String URI = "uri";
    public static final String CLIENT_ID = "clientId";
    public static final String CLIENT_SECRET = "clientSecret";

    @StringField(
            configFieldName = SERVER_URL,
            externalizedKeyName = "derefServerUrl",
            externalized = true,
            description = "Token service server url, this might be different than the above token server url.\n" +
                    "The static url will be used if it is configured."
    )
    private String server_url;

    @StringField(
            configFieldName = PROXY_HOST,
            externalizedKeyName = "derefProxyHost",
            externalized = true,
            description = "For users who leverage SaaS OAuth 2.0 provider in the public cloud and has an internal\n" +
                    "proxy server to access code, token and key services of OAuth 2.0, set up the proxyHost\n" +
                    "here for the HTTPS traffic. This option is only working with server_url and serviceId\n" +
                    "below should be commented out. OAuth 2.0 services cannot be discovered if a proxy is used."
    )
    private String proxyHost;

    @IntegerField(
            configFieldName = PROXY_PORT,
            externalizedKeyName = "derefProxyPort",
            min = 0,
            max = 65535,
            externalized = true,
            description = "We only support HTTPS traffic for the proxy and the default port is 443. " +
                    "If your proxy server has\n" +
                    "a different port, please specify it here. " +
                    "If proxyHost is available and proxyPort is missing, then\n" +
                    "the default value 443 is going to be used for the HTTP connection."
    )
    private int proxyPort;

    @StringField(
            configFieldName = SERVICE_ID,
            externalizedKeyName = "derefServiceId",
            externalized = true,
            defaultValue = "com.networknt.oauth2-token-1.0.0",
            description = "token service unique id for OAuth 2.0 provider. " +
                    "Need for service lookup/discovery. It will be used if above server_url is not configured."
    )
    private String serviceId;

    @BooleanField(
            configFieldName = ENABLE_HTTP2,
            externalizedKeyName = "derefEnableHttp2",
            defaultValue = true,
            externalized = true,
            description = "set to true if the oauth2 provider supports HTTP/2"
    )
    private boolean enableHttp2;

    @StringField(
            configFieldName = URI,
            externalizedKeyName = "derefUri",
            externalized = true,
            defaultValue = "/oauth2/deref",
            description = "the path for the key distribution endpoint"
    )
    private String uri;

    @StringField(
            configFieldName = CLIENT_ID,
            externalizedKeyName = "derefClientId",
            externalized = true,
            defaultValue = "f7d42348-c647-4efb-a52d-4c5787421e72",
            description = "client_id used to access key distribution service. " +
                    "It can be the same client_id with token service or not."
    )
    private char[] client_id;

    @StringField(
            configFieldName = CLIENT_SECRET,
            externalizedKeyName = "derefClientSecret",
            externalized = true,
            defaultValue = "f6h1FTI8Q3-7UScPZDzfXA",
            description = "client_secret for deref"
    )
    private char[] client_secret;

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

    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public String getUri() {
        return uri;
    }

    public char[] getClient_id() {
        return client_id;
    }

    public char[] getClient_secret() {
        return client_secret;
    }
}
