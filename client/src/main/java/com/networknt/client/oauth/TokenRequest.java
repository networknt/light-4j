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
    public static String SERVER_URL = "server_url";
    public static String AUTHORIZATION_CODE = "authorization_code";
    public static String CLIENT_CREDENTIALS = "client_credentials";
    public static String URI = "uri";
    public static String CLIENT_ID = "client_id";
    public static String AUTHORIZATION_CODE_CLIENT_SECRET = "authorizationCodeClientSecret";
    public static String CLIENT_CREDENTIALS_CLIENT_SECRET = "clientCredentialsClientSecret";
    public static String REDIRECT_URI = "redirect_uri";
    public static String SCOPE = "scope";

    String grantType;
    String serverUrl;
    String uri;
    String clientId;
    String clientSecret;
    List<String> scope;

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
}
