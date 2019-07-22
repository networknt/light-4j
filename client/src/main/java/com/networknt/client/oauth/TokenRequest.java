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

import java.util.List;

/**
 * Created by steve on 02/09/16.
 */
public class TokenRequest {

    /**
     * @deprecated will be move to {@link ClientConfig#OAUTH}
     */
    @Deprecated
    public static String OAUTH = "oauth";

    /**
     * @deprecated will be move to {@link ClientConfig#TOKEN}
     */
    @Deprecated
    public static String TOKEN = "token";

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
     * @deprecated will be move to {@link ClientConfig#ENABLE_HTTP2}
     */
    @Deprecated
    public static String ENABLE_HTTP2 = "enableHttp2";

    /**
     * @deprecated will be move to {@link ClientConfig#AUTHORIZATION_CODE}
     */
    @Deprecated
    public static String AUTHORIZATION_CODE = "authorization_code";

    /**
     * @deprecated will be move to {@link ClientConfig#CLIENT_CREDENTIALS}
     */
    @Deprecated
    public static String CLIENT_CREDENTIALS = "client_credentials";

    /**
     * @deprecated will be move to {@link ClientConfig#SAML_BEARER}
     */
    @Deprecated
    public static String SAML_BEARER = "saml_bearer";

    /**
     * @deprecated will be move to {@link ClientConfig#REFRESH_TOKEN}
     */
    @Deprecated
    public static String REFRESH_TOKEN = "refresh_token";

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
     * @deprecated will be move to {@link ClientConfig#REDIRECT_URI}
     */
    @Deprecated
    public static String REDIRECT_URI = "redirect_uri";

    /**
     * @deprecated will be move to {@link ClientConfig#SCOPE}
     */
    @Deprecated
    public static String SCOPE = "scope";

    /**
     * @deprecated will be move to {@link ClientConfig#CSRF}
     */
    @Deprecated
    public static String CSRF = "csrf";

    private String grantType;
    private String serverUrl;
    private String serviceId;
    private boolean enableHttp2;
    private String uri;
    private String clientId;
    private String clientSecret;
    private List<String> scope;
    /**
     * put csrf here as both authorization code and refresh token need it.
     */
    private String csrf;

    public TokenRequest() {
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getUri() {
        return uri;
    }

    public boolean isEnableHttp2() { return enableHttp2; }

    public void setEnableHttp2(boolean enableHttp2) { this.enableHttp2 = enableHttp2; }

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

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
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

    public String getCsrf() { return csrf; }

    public void setCsrf(String csrf) { this.csrf = csrf; }


}
