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

package com.networknt.server;

/**
 * Server configuration class that maps to server.yml properties.
 *
 * @author Steve Hu
 */
public class ServerConfig {
    public static final String CONFIG_NAME = "server";
    String ip;
    int httpPort;
    boolean enableHttp;
    int httpsPort;
    boolean enableHttps;
    boolean enableHttp2;
    String keystoreName;
    boolean enableTwoWayTls;
    String truststoreName;
    boolean enableRegistry;
    String serviceId;
    String environment;
    String buildNumber;
    boolean dynamicPort;
    int minPort;
    int maxPort;

    public ServerConfig() {
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getHttpPort() {
    	String port = System.getProperty("httpPort");
    	if (port != null) {
    		try {
    			int newPort = Integer.parseInt(port);
    			httpPort = newPort;
    		}
    		catch (NumberFormatException ex) {
    			ex.printStackTrace(System.err);
    		}
    	}
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public boolean isEnableHttp() {
        return enableHttp;
    }

    public void setEnableHttp(boolean enableHttp) {
        this.enableHttp = enableHttp;
    }

    public int getHttpsPort() {
    	String port = System.getProperty("httpsPort");
    	if (port != null) {
    		try {
    			int newPort = Integer.parseInt(port);
    			httpsPort = newPort;
    		}
    		catch (NumberFormatException ex) {
    			ex.printStackTrace(System.err);
    		}
    	}
        return httpsPort;
    }

    public void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    public boolean isEnableHttps() {
        return enableHttps;
    }

    public void setEnableHttps(boolean enableHttps) {
        this.enableHttps = enableHttps;
    }

    public String getKeystoreName() {
        return keystoreName;
    }

    public void setKeystoreName(String keystoreName) {
        this.keystoreName = keystoreName;
    }

    public String getTruststoreName() {
        return truststoreName;
    }

    public void setTruststoreName(String truststoreName) {
        this.truststoreName = truststoreName;
    }

    public boolean isEnableTwoWayTls() {
        return enableTwoWayTls;
    }

    public void setEnableTwoWayTls(boolean enableTwoWayTls) {
        this.enableTwoWayTls = enableTwoWayTls;
    }

    public boolean isEnableRegistry() {
        return enableRegistry;
    }

    public void setEnableRegistry(boolean enableRegistry) {
        this.enableRegistry = enableRegistry;
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

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public boolean isDynamicPort() {
        return dynamicPort;
    }

    public void setDynamicPort(boolean dynamicPort) {
        this.dynamicPort = dynamicPort;
    }

    public int getMinPort() {
        return minPort;
    }

    public void setMinPort(int minPort) {
        this.minPort = minPort;
    }

    public int getMaxPort() {
        return maxPort;
    }

    public void setMaxPort(int maxPort) {
        this.maxPort = maxPort;
    }
}
