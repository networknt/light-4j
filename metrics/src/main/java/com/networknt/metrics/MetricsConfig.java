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

package com.networknt.metrics;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Metrics middleware handler configuration that is mapped to all
 * properties in metrics.yml config file.
 *
 * @author Steve Hu
 */
public class MetricsConfig {
    boolean enabled;

    String influxdbProtocol;
    String influxdbHost;
    int influxdbPort;
    String influxdbName;
    String influxdbUser;
    String influxdbPass;
    int reportInMinutes;

    @JsonIgnore
    String description;

    public MetricsConfig() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getInfluxdbHost() {
        return influxdbHost;
    }

    public String getInfluxdbProtocol() {
        return influxdbProtocol;
    }

    public void setInfluxdbProtocol(String influxdbProtocol) {
        this.influxdbProtocol = influxdbProtocol;
    }

    public void setInfluxdbHost(String influxdbHost) {
        this.influxdbHost = influxdbHost;
    }

    public int getInfluxdbPort() {
        return influxdbPort;
    }

    public void setInfluxdbPort(int influxdbPort) {
        this.influxdbPort = influxdbPort;
    }

    public int getReportInMinutes() {
        return reportInMinutes;
    }

    public void setReportInMinutes(int reportInMinutes) {
        this.reportInMinutes = reportInMinutes;
    }

    public String getInfluxdbName() {
        return influxdbName;
    }

    public void setInfluxdbName(String influxdbName) {
        this.influxdbName = influxdbName;
    }

    public String getInfluxdbUser() {
        return influxdbUser;
    }

    public void setInfluxdbUser(String influxdbUser) {
        this.influxdbUser = influxdbUser;
    }

    public String getInfluxdbPass() {
        return influxdbPass;
    }

    public void setInfluxdbPass(String influxdbPass) {
        this.influxdbPass = influxdbPass;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
