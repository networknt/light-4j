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

package com.networknt.portal.registry;

public class PortalRegistryConfig {
    public static final String CONFIG_NAME = "portal-registry";

    String portalUrl;
    String portalToken;
    int maxReqPerConn;
    int deregisterAfter;
    int checkInterval;
    boolean httpCheck;
    boolean ttlCheck;
    String healthPath;

    public String getPortalUrl() {
        return portalUrl;
    }

    public void setPortalUrl(String portalUrl) {
        this.portalUrl = portalUrl;
    }

    public String getPortalToken() {
        return portalToken;
    }

    public void setPortalToken(String portalToken) {
        this.portalToken = portalToken;
    }

    public int getMaxReqPerConn() {
        return maxReqPerConn;
    }

    public void setMaxReqPerConn(int maxReqPerConn) {
        this.maxReqPerConn = maxReqPerConn;
    }

    public int getDeregisterAfter() {
        return deregisterAfter;
    }

    public void setDeregisterAfter(int deregisterAfter) {
        this.deregisterAfter = deregisterAfter;
    }

    public int getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(int checkInterval) {
        this.checkInterval = checkInterval;
    }

    public boolean isHttpCheck() {
        return httpCheck;
    }

    public void setHttpCheck(boolean httpCheck) {
        this.httpCheck = httpCheck;
    }

    public boolean isTtlCheck() {
        return ttlCheck;
    }

    public void setTtlCheck(boolean ttlCheck) {
        this.ttlCheck = ttlCheck;
    }

    public String getHealthPath() {
        return healthPath;
    }

    public void setHealthPath(String healthPath) {
        this.healthPath = healthPath;
    }
}
