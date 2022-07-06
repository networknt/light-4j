package com.networknt.handler.conduit;

import com.networknt.handler.RequestInterceptorHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;
import org.xnio.conduits.AbstractStreamSourceConduit;
import org.xnio.conduits.StreamSourceConduit;

public class ModifiableContentSourceConduit extends AbstractStreamSourceConduit<StreamSourceConduit> {
    public static int MAX_BUFFERS = 1024;
    private final HttpServerExchange exchange;
    private final RequestInterceptorHandler[] interceptors;

    protected ModifiableContentSourceConduit(StreamSourceConduit next, final HttpServerExchange exchange) {
        super(next);
        this.exchange = exchange;
        interceptors = SingletonServiceFactory.getBeans(RequestInterceptorHandler.class);
        resetBufferPool(exchange);
    }

    private void resetBufferPool(HttpServerExchange exchange) {
        var oldBuffers = exchange.getAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY);
        // close the current buffer pool
        if (oldBuffers != null) {
            for (var oldBuffer: oldBuffers) {
                if (oldBuffer != null) {
                    oldBuffer.close();
                }
            }
        }
        exchange.putAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY, new PooledByteBuffer[MAX_BUFFERS]);
    }



}
