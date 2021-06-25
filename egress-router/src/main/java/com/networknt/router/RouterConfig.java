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

package com.networknt.router;

/**
 * Config class for reverse router.
 *
 * @author Steve Hu
 */
public class RouterConfig {
    boolean http2Enabled;
    boolean httpsEnabled;
    int maxRequestTime;
    boolean rewriteHostHeader;
    boolean reuseXForwarded;
    int maxConnectionRetries;
    String[] hostWhitelist;

    public RouterConfig() {
    }

    public boolean isHttp2Enabled() {
        return http2Enabled;
    }

    public void setHttp2Enabled(boolean http2Enabled) {
        this.http2Enabled = http2Enabled;
    }

    public boolean isHttpsEnabled() {
        return httpsEnabled;
    }

    public void setHttpsEnabled(boolean httpsEnabled) {
        this.httpsEnabled = httpsEnabled;
    }

    public int getMaxRequestTime() {
        return maxRequestTime;
    }

    public void setMaxRequestTime(int maxRequestTime) {
        this.maxRequestTime = maxRequestTime;
    }

    public boolean isRewriteHostHeader() { return rewriteHostHeader; }

    public void setRewriteHostHeader(boolean rewriteHostHeader) { this.rewriteHostHeader = rewriteHostHeader; }

    public boolean isReuseXForwarded() { return reuseXForwarded; }

    public void setReuseXForwarded(boolean reuseXForwarded) { this.reuseXForwarded = reuseXForwarded; }

    public int getMaxConnectionRetries() { return maxConnectionRetries; }

    public void setMaxConnectionRetries(int maxConnectionRetries) { this.maxConnectionRetries = maxConnectionRetries; }

    public String[] getHostWhitelist() {
        return hostWhitelist;
    }

    public void setHostWhitelist(String[] hostWhitelist) {
        this.hostWhitelist = hostWhitelist;
    }
}
