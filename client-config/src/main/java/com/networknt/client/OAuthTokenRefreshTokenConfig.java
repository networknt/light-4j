package com.networknt.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.StringField;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthTokenRefreshTokenConfig {

    @StringField(
            configFieldName = ClientConfig.URI,
            externalizedKeyName = "tokenRtUri",
            defaultValue = "/oauth2/token",
            description = "token endpoint for refresh token grant"
    )
    @JsonProperty(ClientConfig.URI)
    private String uri = "/oauth2/token";

    @StringField(
            configFieldName = ClientConfig.CLIENT_ID,
            externalizedKeyName = "tokenRtClientId",
            defaultValue = "f7d42348-c647-4efb-a52d-4c5787421e72",
            description = "client_id for refresh token grant flow."
    )
    @JsonProperty(ClientConfig.CLIENT_ID)
    private char[] clientId = "f7d42348-c647-4efb-a52d-4c5787421e72".toCharArray();

    @StringField(
            configFieldName = ClientConfig.CLIENT_SECRET,
            externalizedKeyName = "tokenRtClientSecret",
            defaultValue = "f6h1FTI8Q3-7UScPZDzfXA",
            description = "client_secret for refresh token grant flow"
    )
    @JsonProperty(ClientConfig.CLIENT_SECRET)
    private char[] clientSecret = "f6h1FTI8Q3-7UScPZDzfXA".toCharArray();

    @ArrayField(
            configFieldName = ClientConfig.SCOPE,
            externalizedKeyName = "tokenRtScope",
            items = String.class,
            description = "optional scope, default scope in the client registration will be used if not defined.\n" +
                    "If there are scopes specified here, they will be verified against the registered scopes.\n" +
                    "In values.yml, you define a list of strings for the scope(s).\n" +
                    "- petstore.r\n" +
                    "- petstore.w\n"

    )
    @JsonProperty(ClientConfig.SCOPE)
    private List<String> scope = null;

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

}
