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

import com.networknt.client.ClientConfig;
import com.networknt.client.Http2Client;
import com.networknt.common.SecretConstants;
import com.networknt.config.Config;

import java.util.Map;

public class DerefRequest {

    /**
     * @deprecated will be move to {@link ClientConfig#OAUTH}
     */
    @Deprecated
    public static String OAUTH = "oauth";

    /**
     * @deprecated will be move to {@link ClientConfig#DEREF}
     */
    @Deprecated
    public static String DEREF = "deref";

    /**
     * @deprecated will be move to {@link ClientConfig#SERVER_URL}
     */
    @Deprecated
    public static String SERVER_URL = "server_url";

    /**
     * @deprecated will be move to {@link ClientConfig#SERVICE_ID}
     */
    @Deprecated
    public static String SERVICE_ID = "serviceId";

    /**
     * @deprecated will be move to {@link ClientConfig#URI}
     */
    @Deprecated
    public static String URI = "uri";

    /**
     * @deprecated will be move to {@link ClientConfig#CLIENT_ID}
     */
    @Deprecated
    public static String CLIENT_ID = "client_id";

    /**
     * @deprecated will be move to {@link ClientConfig#ENABLE_HTTP2}
     */
    @Deprecated
    public static String ENABLE_HTTP2 = "enableHttp2";

    private String serverUrl;
    private String serviceId;
    private String uri;
    private String clientId;
    private String clientSecret;
    private boolean enableHttp2;

    public DerefRequest(String token) {
        Map<String, Object> derefConfig = ClientConfig.get().getDerefConfig();
        if(derefConfig != null) {
            setServerUrl((String)derefConfig.get(ClientConfig.SERVER_URL));
            setServiceId((String)derefConfig.get(ClientConfig.SERVICE_ID));
            Object object = derefConfig.get(ClientConfig.ENABLE_HTTP2);
            setEnableHttp2(object != null && (Boolean) object);
            setUri(derefConfig.get(ClientConfig.URI) + "/" + token);
            setClientId((String)derefConfig.get(ClientConfig.CLIENT_ID));
            // load client secret from client.yml and fallback to secret.yml
            if(derefConfig.get(ClientConfig.CLIENT_SECRET) != null) {
                setClientSecret((String)derefConfig.get(ClientConfig.CLIENT_SECRET));
            } else {
                Map<String, Object> secret = Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_SECRET);
                setClientSecret((String)secret.get(SecretConstants.DEREF_CLIENT_SECRET));
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
