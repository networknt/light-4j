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
import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This is the generic key request with an id as parameter. The static serverUrl will be used if
 * available. Otherwise, the serviceId will be used to lookup the key service. There are two sub
 * classes for signature verification and access token verification with different configurations
 * in the client.yml file.
 *
 * @author Steve Hu
 *
 */
public class KeyRequest {

    /**
     * @deprecated will be move to {@link ClientConfig#OAUTH}
     */
    @Deprecated
    public static String OAUTH = "oauth";

    /**
     * @deprecated will be move to {@link ClientConfig#KEY}
     */
    @Deprecated
    public static String KEY = "key";

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
     * @deprecated will be move to {@link ClientConfig#CLIENT_SECRET}
     */
    @Deprecated
    public static String CLIENT_SECRET = "client_secret";

    /**
     * @deprecated will be move to {@link ClientConfig#ENABLE_HTTP2}
     */
    @Deprecated
    public static String ENABLE_HTTP2 = "enableHttp2";

    protected String serverUrl;
    protected String serviceId;
    protected String uri;
    protected String clientId;
    protected String clientSecret;
    protected boolean enableHttp2;
    protected String kid;

    protected Long connectionTimeout;
    public static String CONNECTION_TIMEOUT = "connectionTimeout";
    public static long defaultConnectionTimeout = 2000l;

    private static final Logger logger = LoggerFactory.getLogger(TokenRequest.class);

    private static final Map<String, Object> clientConfig = Config.getInstance().getJsonMapConfig(ClientConfig.CONFIG_NAME);


    public KeyRequest(String kid) {
        this.kid = kid;
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

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }


    public Long getConnectionTokenTimeout() {
        if (this.connectionTimeout == null) {
            this.connectionTimeout = getTimeout(CONNECTION_TIMEOUT, defaultConnectionTimeout);
        }
        return this.connectionTimeout;
    }

    public void setConnectionTokenTimeout(Long connectionTokenTimeout) {
        this.connectionTimeout = connectionTokenTimeout;
    }

    private long getTimeout(String timeoutKey, long defaultValue) {
        try {
            Map<String, Object> oauthConfig = (Map<String, Object>) clientConfig.get(ClientConfig.OAUTH);
            Map<String, Object> keyConfig = (Map<String, Object>) oauthConfig.get(ClientConfig.KEY);
            Integer timeout = (Integer) keyConfig.get(timeoutKey);
            return timeout == null ? defaultValue : timeout;
        } catch (NullPointerException e) {
            logger.error("Nullpointer in config object: " + e);
        }
        return defaultValue;
    }
}
