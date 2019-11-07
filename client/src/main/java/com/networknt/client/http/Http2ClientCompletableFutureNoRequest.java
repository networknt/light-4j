package com.networknt.client.http;

import io.undertow.client.ClientCallback;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientResponse;
import io.undertow.util.StringReadChannelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListeners;
import org.xnio.channels.StreamSinkChannel;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class Http2ClientCompletableFutureNoRequest extends CompletableFuture<ClientResponse> implements ClientCallback<ClientExchange> {
    private Logger logger = LoggerFactory.getLogger(Http2ClientCompletableFutureNoRequest.class);

    @Override
    public void completed(ClientExchange result) {
        result.setResponseListener(new ClientCallback<ClientExchange>() {
            @Override
            public void completed(final ClientExchange result) {
                new StringReadChannelListener(result.getConnection().getBufferPool()) {

                    @Override
                    protected void stringDone(String string) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Service call response = {}", string);
                        }
                        result.getResponse().putAttachment(com.networknt.client.Http2Client.RESPONSE_BODY, string);
                        Http2ClientCompletableFutureNoRequest.super.complete(result.getResponse());
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
        try {
            result.getRequestChannel().shutdownWrites();
            if(!result.getRequestChannel().flush()) {
                result.getRequestChannel().getWriteSetter().set(ChannelListeners.<StreamSinkChannel>flushingChannelListener(null, null));
                result.getRequestChannel().resumeWrites();
            }
        } catch (IOException e) {
            logger.error("IOException:", e);
            completeExceptionally(e);
        }
    }

    @Override
    public void failed(IOException e) {
        logger.error("IOException:", e);
        completeExceptionally(e);
    }


}