package com.networknt.client.oauth;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;

import java.util.Map;

public class SignRequest {
    public static String OAUTH = "oauth";
    public static String SIGN = "sign";
    public static String SERVER_URL = "server_url";
    public static String URI = "uri";
    public static String ENABLE_HTTP2 = "enableHttp2";
    public static String TIMEOUT = "timeout";
    public static String CLIENT_ID = "client_id";
    public static String CLIENT_SECRET = "client_secret";

    String serverUrl;
    boolean enableHttp2;
    String uri;
    int timeout;
    String clientId;
    String clientSecret;
    int expires;
    Map<String, Object> payload;

    public SignRequest() {
        Map<String, Object> config = Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_NAME);
        if(config != null) {
            Map<String, Object> oauthConfig = (Map<String, Object>)config.get(OAUTH);
            if(oauthConfig != null) {
                Map<String, Object> signConfig = (Map<String, Object>)oauthConfig.get(SIGN);
                if(signConfig != null) {
                    setServerUrl((String)signConfig.get(SERVER_URL));
                    setUri((String)signConfig.get(URI));
                    timeout = (Integer) signConfig.get(TIMEOUT);
                    Object object = signConfig.get(ENABLE_HTTP2);
                    setEnableHttp2(object != null && (Boolean) object);
                    setClientId((String)signConfig.get(CLIENT_ID));
                    setClientSecret((String)signConfig.get(CLIENT_SECRET));
                }
            }
        }
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public void setEnableHttp2(boolean enableHttp2) {
        this.enableHttp2 = enableHttp2;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
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

    public int getExpires() {
        return expires;
    }

    public void setExpires(int expires) {
        this.expires = expires;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
