package com.networknt.client.oauth;

import io.undertow.client.ClientRequest;

/**
 * An interface to describe that a ClientRequest can be composed by a TokenRequest.
 * TokenRequest info should be the same for different Oauth servers, but different Oauth servers may have different way to accept request.
 */
public interface IClientRequestComposable {
    /**
     * compose an actual ClientRequest based on the given TokenRequest model.
     * @param tokenRequest token request
     * @return ClientRequest
     */
    ClientRequest composeClientRequest(TokenRequest tokenRequest);

    /**
     * compose an actual request body based on the given TokenRequest model.
     * @param tokenRequest token request
     * @return String
     */
    String composeRequestBody(TokenRequest tokenRequest);
}
