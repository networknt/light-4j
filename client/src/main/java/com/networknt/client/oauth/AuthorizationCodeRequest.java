/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.client.oauth;

import com.networknt.client.Http2Client;
import com.networknt.common.DecryptUtil;
import com.networknt.common.SecretConstants;
import com.networknt.config.Config;

import java.util.List;
import java.util.Map;

/**
 * Created by steve on 02/09/16.
 */
public class AuthorizationCodeRequest extends TokenRequest {
    static Map<String, Object> secret = DecryptUtil.decryptMap((Map<String, Object>)Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_SECRET));

    String authCode;
    String redirectUri;

    /**
     * load default values from client.json for authorization code grant, overwrite by setters
     * in case you want to change it at runtime.
     */
    public AuthorizationCodeRequest() {
        setGrantType(AUTHORIZATION_CODE);
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
                    Map<String, Object> acConfig = (Map<String, Object>) tokenConfig.get(AUTHORIZATION_CODE);
                    if(acConfig != null) {
                        setClientId((String)acConfig.get(CLIENT_ID));
                        setClientSecret((String)secret.get(SecretConstants.AUTHORIZATION_CODE_CLIENT_SECRET));
                        setUri((String)acConfig.get(URI));
                        setScope((List<String>)acConfig.get(SCOPE));
                        setRedirectUri((String)acConfig.get(REDIRECT_URI));
                    }
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
