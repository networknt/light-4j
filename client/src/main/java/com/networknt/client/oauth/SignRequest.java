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

import java.util.Map;

/**
 * Construct a signing request for token service based on the sign configuration section in
 * client.yml file. If serverUrl is available, the static url will be used. Otherwise, the
 * serviceId will be used to do service discovery when establish a connection to the token
 * service.
 *
 * @author Steve Hu
 *
 */
public class SignRequest {
    /**
     * @deprecated will be moved to {@link ClientConfig#OAUTH}
     */
    @Deprecated
    public static String OAUTH = "oauth";

    /**
     * @deprecated will be moved to {@link ClientConfig#SIGN}
     */
    @Deprecated
    public static String SIGN = "sign";

    /**
     * @deprecated will be moved to {@link ClientConfig#SERVER_URL}
     */
    @Deprecated
    public static String SERVER_URL = "server_url";

    /**
     * @deprecated will be moved to {@link ClientConfig#SERVICE_ID}
     */
    @Deprecated
    public static String SERVICE_ID = "serviceId";

    /**
     * @deprecated will be moved to {@link ClientConfig#URI}
     */
    @Deprecated
    public static String URI = "uri";

    /**
     * @deprecated will be moved to {@link ClientConfig#ENABLE_HTTP2}
     */
    @Deprecated
    public static String ENABLE_HTTP2 = "enableHttp2";

    /**
     * @deprecated will be moved to {@link ClientConfig#TIMEOUT}
     */
    @Deprecated
    public static String TIMEOUT = "timeout";

    /**
     * @deprecated will be moved to {@link ClientConfig#CLIENT_ID}
     */
    @Deprecated
    public static String CLIENT_ID = "client_id";

    /**
     * @deprecated will be moved to {@link ClientConfig#CLIENT_SECRET}
     */
    @Deprecated
    public static String CLIENT_SECRET = "client_secret";

    private String serverUrl;
    private String serviceId;
    private boolean enableHttp2;
    private String uri;
    private int timeout;
    private String clientId;
    private String clientSecret;
    private int expires;
    private Map<String, Object> payload;

    public SignRequest() {
        Map<String, Object> signConfig = ClientConfig.get().getSignConfig();
        if(signConfig != null) {
            setServerUrl((String)signConfig.get(ClientConfig.SERVER_URL));
            setServiceId((String)signConfig.get(ClientConfig.SERVICE_ID));
            setUri((String)signConfig.get(ClientConfig.URI));
            timeout = (Integer) signConfig.get(ClientConfig.TIMEOUT);
            Object object = signConfig.get(ClientConfig.ENABLE_HTTP2);
            setEnableHttp2(object != null && (Boolean) object);
            setClientId((String)signConfig.get(ClientConfig.CLIENT_ID));
            setClientSecret((String)signConfig.get(ClientConfig.CLIENT_SECRET));
        }
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
