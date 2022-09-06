package com.networknt.client.model;
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


public class ServiceDef {
    private String protocol;
    private String serviceId;
    private String environment;
    private String requestKey;

    // To pick up the environment from config
    public ServiceDef(String protocol, String serviceId, String requestKey) {
        this.protocol = protocol;
        this.serviceId = serviceId;
        this.requestKey = requestKey;
    }

    public ServiceDef(String protocol, String serviceId, String environment, String requestKey) {
        this.protocol = protocol;
        this.serviceId = serviceId;
        this.requestKey = requestKey;
        this.environment = environment;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }
}
