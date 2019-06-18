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
import com.networknt.client.oauth.constant.OauthConfigConstants;
import com.networknt.common.SecretConstants;
import com.networknt.config.Config;

import java.util.Map;

public class DerefRequest {

    /**
     * @deprecated will be move to {@link OauthConfigConstants#OAUTH}
     */
    @Deprecated
    public static String OAUTH = "oauth";

    /**
     * @deprecated will be move to {@link OauthConfigConstants#DEREF}
     */
    @Deprecated
    public static String DEREF = "deref";

    /**
     * @deprecated will be move to {@link OauthConfigConstants#SERVER_URL}
     */
    @Deprecated
    public static String SERVER_URL = "server_url";

    /**
     * @deprecated will be move to {@link OauthConfigConstants#SERVICE_ID}
     */
    @Deprecated
    public static String SERVICE_ID = "serviceId";

    /**
     * @deprecated will be move to {@link OauthConfigConstants#URI}
     */
    @Deprecated
    public static String URI = "uri";

    /**
     * @deprecated will be move to {@link OauthConfigConstants#CLIENT_ID}
     */
    @Deprecated
    public static String CLIENT_ID = "client_id";

    /**
     * @deprecated will be move to {@link OauthConfigConstants#ENABLE_HTTP2}
     */
    @Deprecated
    public static String ENABLE_HTTP2 = "enableHttp2";

    private static Map<String, Object> secret = Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_SECRET);

    private String serverUrl;
    private String serviceId;
    private String uri;
    private String clientId;
    private String clientSecret;
    private boolean enableHttp2;

    public DerefRequest(String token) {
        Map<String, Object> clientConfig = Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_NAME);
        // client_secret is in secret.yml instead of client.yml
        if(clientConfig != null) {
            Map<String, Object> oauthConfig = (Map<String, Object>)clientConfig.get(OauthConfigConstants.OAUTH);
            if(oauthConfig != null) {
                Map<String, Object> derefConfig = (Map<String, Object>)oauthConfig.get(OauthConfigConstants.DEREF);
                if(derefConfig != null) {
                    setServerUrl((String)derefConfig.get(OauthConfigConstants.SERVER_URL));
                    setServiceId((String)derefConfig.get(OauthConfigConstants.SERVICE_ID));
                    Object object = derefConfig.get(OauthConfigConstants.ENABLE_HTTP2);
                    setEnableHttp2(object != null && (Boolean) object);
                    setUri(derefConfig.get(OauthConfigConstants.URI) + "/" + token);
                    setClientId((String)derefConfig.get(OauthConfigConstants.CLIENT_ID));
                    setClientSecret((String)secret.get(SecretConstants.DEREF_CLIENT_SECRET));
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

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public boolean isEnableHttp2() { return enableHttp2; }

    public void setEnableHttp2(boolean enableHttp2) { this.enableHttp2 = enableHttp2; }
}
