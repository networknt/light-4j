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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.status.Status;

/**
 * TokenResponse is extended from Status so that error response can be handled gracefully. You
 * should check if statusCode is not empty to ensure that normal response is back from OAuth.
 *
 * @author Steve Hu
 *
 */
public class TokenResponse extends Status {
    @JsonProperty(value="access_token")
    private String accessToken;

    @JsonProperty(value="token_type")
    private String tokenType;

    @JsonProperty(value="expires_in")
    private long expiresIn;

    @JsonProperty(value="scope")
    private String scope;

    @JsonProperty(value="state")
    private String state;

    @JsonProperty(value="refresh_token")
    private String refreshToken;

    @JsonProperty(value="example_parameter")
    private String exampleParameter;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getExampleParameter() {
        return exampleParameter;
    }

    public void setExampleParameter(String exampleParameter) {
        this.exampleParameter = exampleParameter;
    }

    @Override
    public String toString() {
        return "TokenResponse{" +
                "accessToken='" + accessToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", scope='" + scope + '\'' +
                ", state='" + state + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", exampleParameter='" + exampleParameter + '\'' +
                '}';
    }

    public String superString() {
        return super.toString();
    }
}
