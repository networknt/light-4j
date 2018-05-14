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

package com.networknt.auth;

/**
 * Created by steve on 29/09/16.
 */
public class StatelessAuthConfig {
    boolean enabled;
    String redirectUri;
    boolean enableHttp2;
    String requestPath;
    String cookieDomain;
    String cookiePath;
    int cookieMaxAge;
    boolean cookieSecure;

    public StatelessAuthConfig() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public void setEnableHttp2(boolean enableHttp2) {
        this.enableHttp2 = enableHttp2;
    }

    public String getRequestPath() { return requestPath; }

    public void setRequestPath(String requestPath) { this.requestPath = requestPath; }

    public String getCookieDomain() { return cookieDomain; }

    public void setCookieDomain(String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }

    public String getCookiePath() {
        return cookiePath;
    }

    public void setCookiePath(String cookiePath) {
        this.cookiePath = cookiePath;
    }

    public int getCookieMaxAge() {
        return cookieMaxAge;
    }

    public void setCookieMaxAge(int cookieMaxAge) {
        this.cookieMaxAge = cookieMaxAge;
    }

    public boolean isCookieSecure() { return cookieSecure; }

    public void setCookieSecure(boolean cookieSecure) { this.cookieSecure = cookieSecure; }
}
