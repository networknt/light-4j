package com.networknt.client.oauth;

import java.util.List;

/**
 * Created by steve on 02/09/16.
 */
public class TokenRequest {
    public static String OAUTH = "oauth";
    public static String SERVER_URL = "server_url";
    public static String AUTHORIZATION_CODE = "authorization_code";
    public static String CLIENT_CREDENTIALS = "client_credentials";
    public static String URI = "uri";
    public static String CLIENT_ID = "client_id";
    public static String CLIENT_SECRET = "client_secret";
    public static String REDIRECT_URI = "redirect_uri";
    public static String SCOPE = "scope";

    String grantType;
    String serverUrl;
    String uri;
    String clientId;
    String clientSecret;
    List<String> scope;

    public TokenRequest() {
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
}
