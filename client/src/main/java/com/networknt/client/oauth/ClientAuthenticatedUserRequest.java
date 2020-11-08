package com.networknt.client.oauth;

import com.networknt.client.ClientConfig;
import com.networknt.client.Http2Client;
import com.networknt.common.SecretConstants;
import com.networknt.config.Config;

import java.util.List;
import java.util.Map;

public class ClientAuthenticatedUserRequest extends TokenRequest {
    private String userType;
    private String userId;
    private String roles;
    private String redirectUri;

    /**
     * load default values from client.yml for client authenticated user grant, overwrite by setters
     * in case you want to change it at runtime.
     * @param userType user type
     * @param userId user id
     * @param roles user roles
     */
    public ClientAuthenticatedUserRequest(String userType, String userId, String roles) {
        setGrantType(ClientConfig.CLIENT_AUTHENTICATED_USER);
        setUserType(userType);
        setUserId(userId);
        setRoles(roles);
        Map<String, Object> tokenConfig = ClientConfig.get().getTokenConfig();
        if(tokenConfig != null) {
            setServerUrl((String)tokenConfig.get(ClientConfig.SERVER_URL));
            setProxyHost((String)tokenConfig.get(ClientConfig.PROXY_HOST));
            int port = tokenConfig.get(ClientConfig.PROXY_PORT) == null ? 443 : (Integer)tokenConfig.get(ClientConfig.PROXY_PORT);
            setProxyPort(port);
            setServiceId((String)tokenConfig.get(ClientConfig.SERVICE_ID));
            Object object = tokenConfig.get(ClientConfig.ENABLE_HTTP2);
            setEnableHttp2(object != null && (Boolean) object);
            Map<String, Object> acConfig = (Map<String, Object>) tokenConfig.get(ClientConfig.AUTHORIZATION_CODE);
            if(acConfig != null) {
                setClientId((String)acConfig.get(ClientConfig.CLIENT_ID));
                // load client secret from client.yml and fallback to secret.yml
                if(acConfig.get(ClientConfig.CLIENT_SECRET) != null) {
                    setClientSecret((String)acConfig.get(ClientConfig.CLIENT_SECRET));
                } else {
                    Map<String, Object> secret = Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_SECRET);
                    setClientSecret((String)secret.get(SecretConstants.AUTHORIZATION_CODE_CLIENT_SECRET));
                }
                setUri((String)acConfig.get(ClientConfig.URI));
                setScope((List<String>)acConfig.get(ClientConfig.SCOPE));
                setRedirectUri((String)acConfig.get(ClientConfig.REDIRECT_URI));
            }
        }
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

}
