package com.networknt.client.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import io.undertow.client.ClientResponse;

import java.util.List;

public class Http2ServiceResponse {

    ClientResponse clientResponse;
    ObjectMapper objectMapper = Config.getInstance().getMapper();

    public Http2ServiceResponse(ClientResponse clientResponse) {
        this.clientResponse = clientResponse;
    }

    public String getClientResponseBody() {
        return clientResponse.getAttachment(Http2Client.RESPONSE_BODY);
    }

    public int getClientResponseStatusCode() {
        return clientResponse.getResponseCode();
    }

    public boolean isClientResponseStatusOK() {
        int statusCode = getClientResponseStatusCode();
        return statusCode >= 200 && statusCode < 300;
    }

    public <ResponseType> ResponseType getTypedClientResponse(Class<? extends ResponseType> clazz) throws Exception {
            return this.objectMapper.readValue(this.getClientResponseBody(), clazz);
    }

    public <ResponseType> List<ResponseType> getTypedListClientResponse(Class<? extends ResponseType> clazz) throws Exception {
        return this.objectMapper.readValue(this.getClientResponseBody(), objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
    }
}
