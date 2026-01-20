package com.networknt.client;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.MapField;
import com.networknt.config.schema.StringField;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthTokenKeyConfig {

    @StringField(
            configFieldName = ClientConfig.SERVER_URL,
            externalizedKeyName = "tokenKeyServerUrl",
            description = "key distribution server url for token verification. It will be used if it is configured.\n" +
                    "If it is not set, a service lookup will be taken with serviceId to find an instance."
    )
    @JsonProperty(ClientConfig.SERVER_URL)
    private String serverUrl = null;

    @StringField(
            configFieldName = ClientConfig.SERVICE_ID,
            externalizedKeyName = "tokenKeyServiceId",
            defaultValue = "com.networknt.oauth2-key-1.0.0",
            description = "key serviceId for key distribution service, " +
                    "it will be used if above server_url is not configured."
    )
    @JsonProperty(ClientConfig.SERVICE_ID)
    private String serviceId = "com.networknt.oauth2-key-1.0.0";

    @StringField(
            configFieldName = ClientConfig.URI,
            externalizedKeyName = "tokenKeyUri",
            defaultValue = "/oauth2/key",
            description = "the path for the key distribution endpoint"
    )
    @JsonProperty(ClientConfig.URI)
    private String uri = "/oauth2/key";

    @StringField(
            configFieldName = ClientConfig.CLIENT_ID,
            externalizedKeyName = "tokenKeyClientId",
            defaultValue = "f7d42348-c647-4efb-a52d-4c5787421e72",
            description = "client_id used to access key distribution service. " +
                    "It can be the same client_id with token service or not."
    )
    @JsonProperty(ClientConfig.CLIENT_ID)
    private char[] clientId = "f7d42348-c647-4efb-a52d-4c5787421e72".toCharArray();

    @StringField(
            configFieldName = ClientConfig.CLIENT_SECRET,
            externalizedKeyName = "tokenKeyClientSecret",
            defaultValue = "f6h1FTI8Q3-7UScPZDzfXA",
            description = "client secret used to access the key distribution service."
    )
    @JsonProperty(ClientConfig.CLIENT_SECRET)
    private char[] clientSecret = "f6h1FTI8Q3-7UScPZDzfXA".toCharArray();

    @BooleanField(
            configFieldName = ClientConfig.ENABLE_HTTP_2,
            externalizedKeyName = "tokenKeyEnableHttp2",
            defaultValue = "true",
            description = "set to true if the oauth2 provider supports HTTP/2"
    )
    @JsonProperty(ClientConfig.ENABLE_HTTP2)
    private Boolean enableHttp2 = true;

    @MapField(
            configFieldName = ClientConfig.SERVICE_ID_AUTH_SERVERS,
            externalizedKeyName = "tokenKeyServiceIdAuthServers",
            valueType = AuthServerConfig.class,
            description = "The serviceId to the service specific OAuth 2.0 configuration. " +
                    "Used only when multipleOAuthServer is\n" +
                    "set as true. For detailed config options, please see the values.yml in the client module test."
    )
    @JsonProperty(ClientConfig.SERVICE_ID_AUTH_SERVERS)
    private Map<String, AuthServerConfig> serviceIdAuthServers = null;

    @StringField(
            configFieldName = ClientConfig.AUDIENCE,
            externalizedKeyName = "tokenKeyAudience",
            description = "audience for the token validation. " +
                    "It is optional and if it is not configured, no audience validation will be executed."
    )
    @JsonProperty(ClientConfig.AUDIENCE)
    private String audience = null;

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

    public char[] getClientSecret() {
        return clientSecret;
    }

    public Boolean isEnableHttp2() {
        return enableHttp2;
    }

    public Map<String, AuthServerConfig> getServiceIdAuthServers() {
        return serviceIdAuthServers;
    }

    public String getAudience() {
        return audience;
    }
}
