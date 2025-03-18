package com.networknt.client;

import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.StringField;

public class OAuthSignKeyConfig  {

    public static final String SERVER_URL = "server_url";
    public static final String SERVICE_ID = "serviceId";
    public static final String URI = "uri";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String ENABLE_HTTP_2 = "enableHttp2";
    public static final String AUDIENCE = "audience";
    @StringField(
            configFieldName = SERVER_URL,
            externalizedKeyName = "signKeyServerUrl",
            externalized = true,
            description = "key distribution server url. It will be used to establish connection if it exists.\n" +
                    "if it is not set, then a service lookup against serviceId will be taken to discover an instance."
    )
    private String server_url;

    @StringField(
            configFieldName = SERVICE_ID,
            externalizedKeyName = "signKeyServiceId",
            externalized = true,
            defaultValue = "com.networknt.oauth2-key-1.0.0",
            description = "the unique service id for key distribution service, " +
                    "it will be used to lookup key service if above url doesn't exist."
    )
    private String serviceId;

    @StringField(
            configFieldName = URI,
            externalizedKeyName = "signKeyUri",
            externalized = true,
            defaultValue = "/oauth2/key",
            description = "the path for the key distribution endpoint"
    )
    private String uri;

    @StringField(
            configFieldName = CLIENT_ID,
            externalizedKeyName = "signKeyClientId",
            externalized = true,
            defaultValue = "f7d42348-c647-4efb-a52d-4c5787421e72",
            description = "client_id used to access key distribution service. " +
                    "It can be the same client_id with token service or not."
    )
    private char[] client_id;

    @StringField(
            configFieldName = CLIENT_SECRET,
            externalizedKeyName = "signKeyClientSecret",
            externalized = true,
            defaultValue = "f6h1FTI8Q3-7UScPZDzfXA",
            description = "client secret used to access the key distribution service."
    )
    private char[] client_secret;

    @BooleanField(
            configFieldName = ENABLE_HTTP_2,
            externalizedKeyName = "signKeyEnableHttp2",
            defaultValue = true,
            externalized = true,
            description = "set to true if the oauth2 provider supports HTTP/2"
    )
    private boolean enableHttp2;

    @StringField(
            configFieldName = AUDIENCE,
            externalizedKeyName = "signKeyAudience",
            externalized = true,
            description = "audience for the token validation. " +
                    "It is optional and if it is not configured, no audience validation will be executed."
    )
    private String audience;

    public String getServer_url() {
        return server_url;
    }

    public String getServiceId() {
        return serviceId;
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

    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public String getAudience() {
        return audience;
    }
}
