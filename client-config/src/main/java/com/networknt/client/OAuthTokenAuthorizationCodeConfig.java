package com.networknt.client;

import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.StringField;

import java.util.List;

public class OAuthTokenAuthorizationCodeConfig {

    public static final String URI = "uri";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String SCOPE = "scope";
    @StringField(
            configFieldName = URI,
            externalizedKeyName = "tokenAcUri",
            externalized = true,
            defaultValue = "/oauth2/token",
            description = "token endpoint for authorization code grant"
    )
    private String uri;

    @StringField(
            configFieldName = CLIENT_ID,
            externalizedKeyName = "tokenAcClientId",
            externalized = true,
            defaultValue = "f7d42348-c647-4efb-a52d-4c5787421e72",
            description = "client_id for authorization code grant flow."
    )
    private char[] client_id;

    @StringField(
            configFieldName = CLIENT_SECRET,
            externalizedKeyName = "tokenAcClientSecret",
            externalized = true,
            defaultValue = "f6h1FTI8Q3-7UScPZDzfXA",
            description = "client_secret for authorization code grant flow."
    )
    private char[] client_secret;

    @StringField(
            configFieldName = REDIRECT_URI,
            externalizedKeyName = "tokenAcRedirectUri",
            externalized = true,
            defaultValue = "http://localhost:8080/authorization",
            description = "the web server uri that will receive the redirected authorization code"
    )
    private String redirect_uri;

    @ArrayField(
            configFieldName = SCOPE,
            externalizedKeyName = "tokenAcScope",
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

    public String getRedirect_uri() {
        return redirect_uri;
    }

    public List<String> getScope() {
        return scope;
    }
}
