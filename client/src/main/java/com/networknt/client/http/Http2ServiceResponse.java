package com.networknt.client.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import io.undertow.client.ClientResponse;

import java.util.List;

/**
 * A wrapper for ClientResponse to handle HTTP/2 service responses.
 */
public class Http2ServiceResponse {

    ClientResponse clientResponse;
    ObjectMapper objectMapper = Config.getInstance().getMapper();

    /**
     * Constructor.
     * @param clientResponse the client response
     */
    public Http2ServiceResponse(ClientResponse clientResponse) {
        this.clientResponse = clientResponse;
    }

    /**
     * Get the client response body.
     * @return the response body as string
     */
    public String getClientResponseBody() {
        return clientResponse.getAttachment(Http2Client.RESPONSE_BODY);
    }

    /**
     * Get the client response status code.
     * @return the status code
     */
    public int getClientResponseStatusCode() {
        return clientResponse.getResponseCode();
    }

    /**
     * Check if the client response status is OK (2xx).
     * @return true if status is 2xx
     */
    public boolean isClientResponseStatusOK() {
        int statusCode = getClientResponseStatusCode();
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Get the typed client response.
     * @param <ResponseType> the type of response
     * @param clazz the class of response
     * @return the response object
     * @throws Exception if deserialization fails
     */
    public <ResponseType> ResponseType getTypedClientResponse(Class<? extends ResponseType> clazz) throws Exception {
            return this.objectMapper.readValue(this.getClientResponseBody(), clazz);
    }

    /**
     * Get the typed list client response.
     * @param <ResponseType> the type of response
     * @param clazz the class of response
     * @return the list of response objects
     * @throws Exception if deserialization fails
     */
    public <ResponseType> List<ResponseType> getTypedListClientResponse(Class<? extends ResponseType> clazz) throws Exception {
        return this.objectMapper.readValue(this.getClientResponseBody(), objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
    }
}
