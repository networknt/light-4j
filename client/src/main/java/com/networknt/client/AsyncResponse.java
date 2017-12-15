package com.networknt.client;

import io.undertow.client.ClientResponse;

public class AsyncResponse {
    ClientResponse clientResponse;
    String responseBody;
    long responseTime;

    public AsyncResponse(ClientResponse clientResponse, String responseBody, long responseTime) {
        this.clientResponse = clientResponse;
        this.responseBody = responseBody;
        this.responseTime = responseTime;
    }

    public ClientResponse getClientResponse() {
        return clientResponse;
    }

    public void setClientResponse(ClientResponse clientResponse) {
        this.clientResponse = clientResponse;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }
}
