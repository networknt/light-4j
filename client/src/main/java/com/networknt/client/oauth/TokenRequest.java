/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

import java.util.List;

/**
 * Created by steve on 02/09/16.
 */
public class TokenRequest {
    public static String OAUTH = "oauth";
    public static String TOKEN = "token";
    public static String SERVER_URL = "server_url";
    public static String ENABLE_HTTP2 = "enableHttp2";
    public static String AUTHORIZATION_CODE = "authorization_code";
    public static String CLIENT_CREDENTIALS = "client_credentials";
    public static String SAML_BEARER = "saml_bearer";
    public static String REFRESH_TOKEN = "refresh_token";
    public static String URI = "uri";
    public static String CLIENT_ID = "client_id";
    public static String REDIRECT_URI = "redirect_uri";
    public static String SCOPE = "scope";
    public static String CSRF = "csrf";

    String grantType;
    String serverUrl;
    boolean enableHttp2;
    String uri;
    String clientId;
    String clientSecret;
    List<String> scope;
    // put csrf here as both authorization code and refresh token need it.
    String csrf;

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

    public String getCsrf() { return csrf; }

    public void setCsrf(String csrf) { this.csrf = csrf; }


}
