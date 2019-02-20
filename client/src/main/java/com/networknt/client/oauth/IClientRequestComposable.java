package com.networknt.client.oauth;

import io.undertow.client.ClientRequest;

public interface IClientRequestComposable {
    ClientRequest ComposeClientRequest(TokenRequest tokenRequest);
    String ComposeRequestBody(TokenRequest tokenRequest);
}
