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

import java.util.Map;

import com.networknt.client.Http2Client;
import com.networknt.common.SecretConstants;
import com.networknt.config.Config;

public class KeyRequest {
    public static String OAUTH = "oauth";
    public static String KEY = "key";
    public static String SERVER_URL = "server_url";
    public static String URI = "uri";
    public static String CLIENT_ID = "client_id";
    public static String ENABLE_HTTP2 = "enableHttp2";

    static Map<String, Object> secret = (Map<String, Object>)Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_SECRET);

    String serverUrl;
    String uri;
    String clientId;
    String clientSecret;
    boolean enableHttp2;

    public KeyRequest(String kid) {
        Map<String, Object> clientConfig = Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_NAME);
        // client_secret is in secret.yml instead of client.yml
        if(clientConfig != null) {
            Map<String, Object> oauthConfig = (Map<String, Object>)clientConfig.get(OAUTH);
            if(oauthConfig != null) {
                Map<String, Object> keyConfig = (Map<String, Object>)oauthConfig.get(KEY);
                if(keyConfig != null) {
                    setServerUrl((String)keyConfig.get(SERVER_URL));
                    Object object = keyConfig.get(ENABLE_HTTP2);
                    setEnableHttp2(object != null && (Boolean) object);
                    setUri(keyConfig.get(URI) + "/" + kid);
                    setClientId((String)keyConfig.get(CLIENT_ID));
                    setClientSecret((String)secret.get(SecretConstants.KEY_CLIENT_SECRET));
                }
            }
        }
    }

    public KeyRequest() { }

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

    public boolean isEnableHttp2() { return enableHttp2; }

    public void setEnableHttp2(boolean enableHttp2) { this.enableHttp2 = enableHttp2; }
}
