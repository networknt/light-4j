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

package com.networknt.client;

import io.undertow.client.ClientResponse;

/**
 * This class represents an asynchronous response from a client request.
 * It contains the ClientResponse, the response body as a String, and the response time.
 */
public class AsyncResponse {
    private ClientResponse clientResponse;
    private String responseBody;
    private long responseTime;

    /**
     * Constructs an AsyncResponse with the given parameters.
     * @param clientResponse the ClientResponse object
     * @param responseBody the response body as a String
     * @param responseTime the time taken to receive the response
     */
    public AsyncResponse(ClientResponse clientResponse, String responseBody, long responseTime) {
        this.clientResponse = clientResponse;
        this.responseBody = responseBody;
        this.responseTime = responseTime;
    }

    /**
     * Returns the ClientResponse object.
     * @return the ClientResponse object
     */
    public ClientResponse getClientResponse() {
        return clientResponse;
    }

    /**
     * Sets the ClientResponse object.
     * @param clientResponse the ClientResponse object to set
     */
    public void setClientResponse(ClientResponse clientResponse) {
        this.clientResponse = clientResponse;
    }

    /**
     * Returns the response body as a String.
     * @return the response body
     */
    public String getResponseBody() {
        return responseBody;
    }

    /**
     * Sets the response body.
     * @param responseBody the response body to set
     */
    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    /**
     * Returns the response time.
     * @return the response time
     */
    public long getResponseTime() {
        return responseTime;
    }

    /**
     * Sets the response time.
     * @param responseTime the response time to set
     */
    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }
}
