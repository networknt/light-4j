package com.networknt.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.StringField;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthTokenAuthorizationCodeConfig {

    @StringField(
            configFieldName = ClientConfig.URI,
            externalizedKeyName = "tokenAcUri",
            defaultValue = "/oauth2/token",
            description = "token endpoint for authorization code grant"
    )
    @JsonProperty(ClientConfig.URI)
    private String uri = "/oauth2/token";

    @StringField(
            configFieldName = ClientConfig.CLIENT_ID,
            externalizedKeyName = "tokenAcClientId",
            defaultValue = "f7d42348-c647-4efb-a52d-4c5787421e72",
            description = "client_id for authorization code grant flow."
    )
    @JsonProperty(ClientConfig.CLIENT_ID)
    private char[] clientId = "f7d42348-c647-4efb-a52d-4c5787421e72".toCharArray();

    @StringField(
            configFieldName = ClientConfig.CLIENT_SECRET,
            externalizedKeyName = "tokenAcClientSecret",
            defaultValue = "f6h1FTI8Q3-7UScPZDzfXA",
            description = "client_secret for authorization code grant flow."
    )
    @JsonProperty(ClientConfig.CLIENT_SECRET)
    private char[] clientSecret = "f6h1FTI8Q3-7UScPZDzfXA".toCharArray();

    @StringField(
            configFieldName = ClientConfig.REDIRECT_URI,
            externalizedKeyName = "tokenAcRedirectUri",
            defaultValue = "https://localhost:3000/authorization",
            description = "the web server uri that will receive the redirected authorization code"
    )
    @JsonProperty(ClientConfig.REDIRECT_URI)
    private String redirectUri = "https://localhost:3000/authorization";

    @ArrayField(
            configFieldName = ClientConfig.SCOPE,
            externalizedKeyName = "tokenAcScope",
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

    public String getRedirectUri() {
        return redirectUri;
    }

    public List<String> getScope() {
        return scope;
    }
}
