package com.networknt.client.oauth;

import com.networknt.client.ClientConfig;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ClientAuthenticatedUserRequest extends TokenRequest {
    private static final Logger logger = LoggerFactory.getLogger(ClientAuthenticatedUserRequest.class);
    private static final String CONFIG_PROPERTY_MISSING = "ERR10057";
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
                if(acConfig.get(ClientConfig.CLIENT_SECRET) != null) {
                    setClientSecret((String)acConfig.get(ClientConfig.CLIENT_SECRET));
                } else {
                    logger.error(new Status(CONFIG_PROPERTY_MISSING, "authorization_code client_secret", "client.yml").toString());
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
