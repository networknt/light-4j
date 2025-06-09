package com.networknt.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.StringField;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthSignKeyConfig  {

    @StringField(
            configFieldName = ClientConfig.SERVER_URL,
            externalizedKeyName = "signKeyServerUrl",
            externalized = true,
            description = "key distribution server url. It will be used to establish connection if it exists.\n" +
                    "if it is not set, then a service lookup against serviceId will be taken to discover an instance."
    )
    @JsonProperty(ClientConfig.SERVER_URL)
    private String serverUrl;

    @StringField(
            configFieldName = ClientConfig.SERVICE_ID,
            externalizedKeyName = "signKeyServiceId",
            externalized = true,
            defaultValue = "com.networknt.oauth2-key-1.0.0",
            description = "the unique service id for key distribution service, " +
                    "it will be used to lookup key service if above url doesn't exist."
    )
    @JsonProperty(ClientConfig.SERVICE_ID)
    private String serviceId = "com.networknt.oauth2-key-1.0.0";

    @StringField(
            configFieldName = ClientConfig.URI,
            externalizedKeyName = "signKeyUri",
            externalized = true,
            defaultValue = "/oauth2/key",
            description = "the path for the key distribution endpoint"
    )
    @JsonProperty(ClientConfig.URI)
    private String uri = "/oauth2/key";

    @StringField(
            configFieldName = ClientConfig.CLIENT_ID,
            externalizedKeyName = "signKeyClientId",
            externalized = true,
            defaultValue = "f7d42348-c647-4efb-a52d-4c5787421e72",
            description = "client_id used to access key distribution service. " +
                    "It can be the same client_id with token service or not."
    )
    @JsonProperty(ClientConfig.CLIENT_ID)
    private char[] clientId = "f7d42348-c647-4efb-a52d-4c5787421e72".toCharArray();

    @StringField(
            configFieldName = ClientConfig.CLIENT_SECRET,
            externalizedKeyName = "signKeyClientSecret",
            externalized = true,
            defaultValue = "f6h1FTI8Q3-7UScPZDzfXA",
            description = "client secret used to access the key distribution service."
    )
    @JsonProperty(ClientConfig.CLIENT_SECRET)
    private char[] client_secret;

    @BooleanField(
            configFieldName = ClientConfig.ENABLE_HTTP_2,
            externalizedKeyName = "signKeyEnableHttp2",
            defaultValue = true,
            externalized = true,
            description = "set to true if the oauth2 provider supports HTTP/2"
    )
    @JsonProperty(ClientConfig.ENABLE_HTTP_2)
    private Boolean enableHttp2 = true;

    @StringField(
            configFieldName = ClientConfig.AUDIENCE,
            externalizedKeyName = "signKeyAudience",
            externalized = true,
            description = "audience for the token validation. " +
                    "It is optional and if it is not configured, no audience validation will be executed."
    )
    @JsonProperty(ClientConfig.AUDIENCE)
    private String audience;

    public String getServerUrl() {
        return serverUrl;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getUri() {
        return uri;
    }

    public char[] getClientId() {
        return clientId;
    }

    public char[] getClient_secret() {
        return client_secret;
    }

    public Boolean isEnableHttp2() {
        return enableHttp2;
    }

    public String getAudience() {
        return audience;
    }
}
