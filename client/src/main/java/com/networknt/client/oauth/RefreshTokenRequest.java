package com.networknt.client.oauth;

import com.networknt.client.Http2Client;
import com.networknt.common.DecryptUtil;
import com.networknt.common.SecretConstants;
import com.networknt.config.Config;

import java.util.List;
import java.util.Map;

public class RefreshTokenRequest extends TokenRequest {
    static Map<String, Object> secret = DecryptUtil.decryptMap((Map<String, Object>)Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_SECRET));

    String refreshToken;

    public RefreshTokenRequest() {
        setGrantType(REFRESH_TOKEN);
        Map<String, Object> clientConfig = Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_NAME);
        // client_secret is in secret.yml instead of client.yml
        if(clientConfig != null) {
            Map<String, Object> oauthConfig = (Map<String, Object>)clientConfig.get(OAUTH);
            if(oauthConfig != null) {
                Map<String, Object> tokenConfig = (Map<String, Object>)oauthConfig.get(TOKEN);
                if(tokenConfig != null) {
                    setServerUrl((String)tokenConfig.get(SERVER_URL));
                    Object object = tokenConfig.get(ENABLE_HTTP2);
                    setEnableHttp2(object != null && (Boolean) object);
                    Map<String, Object> rtConfig = (Map<String, Object>) tokenConfig.get(REFRESH_TOKEN);
                    if(rtConfig != null) {
                        setClientId((String)rtConfig.get(CLIENT_ID));
                        setClientSecret((String)secret.get(SecretConstants.REFRESH_TOKEN_CLIENT_SECRET));
                        setUri((String)rtConfig.get(URI));
                        setScope((List<String>)rtConfig.get(SCOPE));
                    }
                }
            }
        }
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
