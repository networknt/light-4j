package com.networknt.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.StringField;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthTokenExchangeConfig {

    @StringField(
            configFieldName = ClientConfig.URI,
            externalizedKeyName = "tokenExUri",
            externalized = true,
            defaultValue = "/oauth2/token",
            description = "token endpoint for token exchange grant type"
    )
    @JsonProperty(ClientConfig.URI)
    private String uri = "/oauth2/token";

    @StringField(
            configFieldName = ClientConfig.CLIENT_ID,
            externalizedKeyName = "tokenExClientId",
            externalized = true,
            defaultValue = "f7d42348-c647-4efb-a52d-4c5787421e72",
            description = "client_id for token exchange grant flow."
    )
    @JsonProperty(ClientConfig.CLIENT_ID)
    private char[] clientId = "f7d42348-c647-4efb-a52d-4c5787421e72".toCharArray();

    @StringField(
            configFieldName = ClientConfig.CLIENT_SECRET,
            externalizedKeyName = "tokenExClientSecret",
            externalized = true,
            defaultValue = "f6h1FTI8Q3-7UScPZDzfXA",
            description = "client_secret for token exchange grant flow"
    )
    @JsonProperty(ClientConfig.CLIENT_SECRET)
    private char[] clientSecret = "f6h1FTI8Q3-7UScPZDzfXA".toCharArray();

    @ArrayField(
            configFieldName = ClientConfig.SCOPE,
            externalizedKeyName = "tokenExScope",
            externalized = true,
            items = String.class,
            description = "optional scope, default scope in the client registration will be used if not defined.\n" +
                    "If there are scopes specified here, they will be verified against the registered scopes.\n" +
                    "In values.yml, you define a list of strings for the scope(s).\n" +
                    "- petstore.r\n" +
                    "- petstore.w\n"

    )
    @JsonProperty(ClientConfig.SCOPE)
    private List<String> scope = null;


    @StringField(
            configFieldName = ClientConfig.SUBJECT_TOKEN,
            externalizedKeyName = "subjectToken",
            externalized = true,
            description = "subject token the identity of the party on behalf of whom the request is being made"
    )
    @JsonProperty(ClientConfig.SUBJECT_TOKEN)
    private String subjectToken = null;


    @StringField(
            configFieldName = ClientConfig.SUBJECT_TOKEN_TYPE,
            externalizedKeyName = "subjectTokenType",
            externalized = true,
            description = "subject token type the type of the subject token"
    )
    @JsonProperty(ClientConfig.SUBJECT_TOKEN_TYPE)
    private String subjectTokenType = "urn:ietf:params:oauth:token-type:jwt";

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

    public String getSubjectToken() {
        return subjectToken;
    }

    public String getSubjectTokenType() {
        return subjectTokenType;
    }
}
