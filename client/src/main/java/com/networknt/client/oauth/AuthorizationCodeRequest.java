package com.networknt.client.oauth;

import com.networknt.client.Client;
import com.networknt.config.Config;

import java.util.List;
import java.util.Map;

/**
 * Created by steve on 02/09/16.
 */
public class AuthorizationCodeRequest extends TokenRequest {

    String authCode;
    String redirectUri;

    /**
     * load default values from client.json for authorization code grant, overwrite by setters
     * in case you want to change it at runtime.
     */
    public AuthorizationCodeRequest() {
        setGrantType(AUTHORIZATION_CODE);
        Map<String, Object> clientConfig = Config.getInstance().getJsonMapConfig(Client.CONFIG_NAME);
        if(clientConfig != null) {
            Map<String, Object> oauthConfig = (Map<String, Object>)clientConfig.get(OAUTH);
            if(oauthConfig != null) {
                setServerUrl((String)oauthConfig.get(SERVER_URL));
                Map<String, Object> acConfig = (Map<String, Object>) oauthConfig.get(AUTHORIZATION_CODE);
                if(acConfig != null) {
                    setClientId((String)acConfig.get(CLIENT_ID));
                    setClientSecret((String)acConfig.get(CLIENT_SECRET));
                    setUri((String)acConfig.get(URI));
                    setScope((List<String>)acConfig.get(SCOPE));
                    setRedirectUri((String)acConfig.get(REDIRECT_URI));
                }
            }
        }
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
