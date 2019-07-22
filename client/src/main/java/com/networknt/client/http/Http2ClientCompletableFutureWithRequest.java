package com.networknt.client.http;

import io.undertow.client.ClientCallback;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientResponse;
import io.undertow.util.StringReadChannelListener;
import io.undertow.util.StringWriteChannelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class Http2ClientCompletableFutureWithRequest extends CompletableFuture<ClientResponse> implements ClientCallback<ClientExchange> {

    private Logger logger = LoggerFactory.getLogger(Http2ClientCompletableFutureWithRequest.class);

    private String requestBody;
    public Http2ClientCompletableFutureWithRequest(String requestBody) {
        this.requestBody = requestBody;
    }

    @Override
    public void completed(ClientExchange result) {
        new StringWriteChannelListener(requestBody).setup(result.getRequestChannel());
        result.setResponseListener(new ClientCallback<ClientExchange>() {
            @Override
            public void completed(ClientExchange result) {
                new StringReadChannelListener(com.networknt.client.Http2Client.BUFFER_POOL) {
                    @Override
                    protected void stringDone(String string) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Service call response = {}", string);
                        }
                        result.getResponse().putAttachment(com.networknt.client.Http2Client.RESPONSE_BODY, string);
                        complete(result.getResponse());
                    }

                    @Override
                    protected void error(IOException e) {
                        logger.error("IOException:", e);
                        completeExceptionally(e);
                    }
                }.setup(result.getResponseChannel());
            }

            @Override
            public void failed(IOException e) {
                logger.error("IOException:", e);
                completeExceptionally(e);
            }
        });
    }

    @Override
    public void failed(IOException e) {
        logger.error("IOException:", e);
        completeExceptionally(e);
    }

}