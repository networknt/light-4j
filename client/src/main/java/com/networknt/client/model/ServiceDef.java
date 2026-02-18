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


/**
 * ServiceDef class.
 */
public class ServiceDef {
    private String protocol;
    private String serviceId;
    private String environment;
    private String requestKey;

    // To pick up the environment from config
    /**
     * Constructor.
     * @param protocol the protocol
     * @param serviceId the serviceId
     * @param requestKey the requestKey
     */
    public ServiceDef(String protocol, String serviceId, String requestKey) {
        this.protocol = protocol;
        this.serviceId = serviceId;
        this.requestKey = requestKey;
    }

    /**
     * Constructor.
     * @param protocol the protocol
     * @param serviceId the serviceId
     * @param environment the environment
     * @param requestKey the requestKey
     */
    public ServiceDef(String protocol, String serviceId, String environment, String requestKey) {
        this.protocol = protocol;
        this.serviceId = serviceId;
        this.requestKey = requestKey;
        this.environment = environment;
    }

    /**
     * Get protocol.
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Set protocol.
     * @param protocol the protocol
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Get serviceId.
     * @return the serviceId
     */
    public String getServiceId() {
        return serviceId;
    }

    /**
     * Set serviceId.
     * @param serviceId the serviceId
     */
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    /**
     * Get environment.
     * @return the environment
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     * Set environment.
     * @param environment the environment
     */
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    /**
     * Get requestKey.
     * @return the requestKey
     */
    public String getRequestKey() {
        return requestKey;
    }

    /**
     * Set requestKey.
     * @param requestKey the requestKey
     */
    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }
}
