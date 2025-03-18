package com.networknt.client;

import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.StringField;

import java.util.List;

public class OAuthTokenRefreshTokenConfig {

    public static final String URI = "uri";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String SCOPE = "scope";

    @StringField(
            configFieldName = URI,
            externalizedKeyName = "tokenRtUri",
            externalized = true,
            defaultValue = "/oauth2/token",
            description = "token endpoint for refresh token grant"
    )
    private String uri;

    @StringField(
            configFieldName = CLIENT_ID,
            externalizedKeyName = "tokenRtClientId",
            externalized = true,
            defaultValue = "f7d42348-c647-4efb-a52d-4c5787421e72",
            description = "client_id for refresh token grant flow."
    )
    private char[] client_id;

    @StringField(
            configFieldName = CLIENT_SECRET,
            externalizedKeyName = "tokenRtClientSecret",
            externalized = true,
            defaultValue = "f6h1FTI8Q3-7UScPZDzfXA",
            description = "client_secret for refresh token grant flow"
    )
    private char[] client_secret;

    @ArrayField(
            configFieldName = SCOPE,
            externalizedKeyName = "tokenRtScope",
            externalized = true,
            items = String.class,
            description = "optional scope, default scope in the client registration will be used if not defined.\n" +
                    "If there are scopes specified here, they will be verified against the registered scopes.\n" +
                    "In values.yml, you define a list of strings for the scope(s)."
    )
    private List<String> scope;

    public String getUri() {
        return uri;
    }

    public char[] getClient_id() {
        return client_id;
    }

    public char[] getClient_secret() {
        return client_secret;
    }

    public List<String> getScope() {
        return scope;
    }

}
