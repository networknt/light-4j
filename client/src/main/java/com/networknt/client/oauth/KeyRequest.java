package com.networknt.client.oauth;

import com.networknt.client.Http2Client;
import com.networknt.common.SecretConfig;
import com.networknt.config.Config;

import java.util.Map;

public class KeyRequest {
    public static String OAUTH = "oauth";
    public static String KEY = "key";
    public static String SERVER_URL = "server_url";
    public static String URI = "uri";
    public static String CLIENT_ID = "client_id";

    String serverUrl;
    String uri;
    String clientId;
    String clientSecret;

    public KeyRequest(String kid) {
        Map<String, Object> clientConfig = Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_NAME);
        // client_secret is in secret.yml instead of client.yml
        SecretConfig secretConfig = (SecretConfig)Config.getInstance().getJsonObjectConfig(Http2Client.CONFIG_SECRET, SecretConfig.class);
        if(clientConfig != null) {
            Map<String, Object> oauthConfig = (Map<String, Object>)clientConfig.get(OAUTH);
            if(oauthConfig != null) {
                Map<String, Object> keyConfig = (Map<String, Object>)oauthConfig.get(KEY);
                if(keyConfig != null) {
                    setServerUrl((String)keyConfig.get(SERVER_URL));
                    setUri(keyConfig.get(URI) + "/" + kid);
                    setClientId((String)keyConfig.get(CLIENT_ID));
                    setClientSecret(secretConfig.getKeyClientSecret());
                }
            }
        }
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
