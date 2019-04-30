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
    public static String OAUTH = "oauth";
    public static String KEY = "key";
    public static String SERVER_URL = "server_url";
    public static String SERVICE_ID = "serviceId";
    public static String URI = "uri";
    public static String CLIENT_ID = "client_id";
    public static String CLIENT_SECRET = "client_secret";
    public static String ENABLE_HTTP2 = "enableHttp2";


    String serverUrl;
    String serviceId;
    String uri;
    String clientId;
    String clientSecret;
    boolean enableHttp2;
    String kid;

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
}
