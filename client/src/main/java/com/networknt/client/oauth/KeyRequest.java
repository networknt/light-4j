package com.networknt.client.oauth;

public class KeyRequest {
    public static String SERVER_URL = "server_url";
    public static String URI = "uri";
    public static String CLIENT_ID = "client_id";
    public static String CLIENT_SECRET = "client_secret";

    String serverUrl;
    String uri;
    String clientId;
    String clientSecret;

    public KeyRequest() {
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

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

}
