package com.networknt.restrans;

import com.networknt.config.Config;
import com.networknt.handler.BuffersUtils;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.ResponseInterceptorHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * This is a generic middleware handler to manipulate response based on rule-engine rules so that it can be much more
 * flexible than any other handlers like the header handler to manipulate the headers. The rules will be loaded from
 * the configuration or from the light-portal if portal is implemented.
 *
 * @author Steve Hu
 */
public class ResponseTransformerHandler implements ResponseInterceptorHandler {
    static final Logger logger = LoggerFactory.getLogger(ResponseTransformerHandler.class);
    public static int MAX_BUFFERS = 1024;

    private ResponseTransformerConfig config;
    private volatile HttpHandler next;

    public ResponseTransformerHandler() {
        if(logger.isInfoEnabled()) logger.info("ResponseManipulatorHandler is loaded");
        config = ResponseTransformerConfig.load();
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(ResponseTransformerHandler.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void reload() {
        config.reload();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(logger.isTraceEnabled()) logger.trace("ResponseTransformerHandler.handleRequest is called.");
        String s = BuffersUtils.toString(getBuffer(exchange), StandardCharsets.UTF_8);
        if(logger.isTraceEnabled()) logger.trace("original response body = " + s);
        // change the buffer
        s = "[{\"com.networknt.handler.ResponseInterceptorHandler\":[\"com.networknt.restrans.ResponseTransformerHandler\"]}]";
        PooledByteBuffer[] dest = new PooledByteBuffer[MAX_BUFFERS];
        setBuffer(exchange, dest);
        BuffersUtils.transfer(ByteBuffer.wrap(s.getBytes()), dest, exchange);
    }

    @Override
    public boolean isRequiredContent() {
        return true;
    }

    public PooledByteBuffer[] getBuffer(HttpServerExchange exchange) {
        PooledByteBuffer[] buffer = exchange.getAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY);
        if (buffer == null) {
            throw new IllegalStateException("Response content is not available in exchange attachment as there is no interceptors.");
        }
        return buffer;
    }

    public void setBuffer(HttpServerExchange exchange, PooledByteBuffer[] raw) {
        // close the current buffer pool
        PooledByteBuffer[] oldBuffers = exchange.getAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY);
        if (oldBuffers != null) {
            for (var oldBuffer: oldBuffers) {
                if (oldBuffer != null) {
                    oldBuffer.close();
                }
            }
        }
        exchange.putAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY, raw);
    }

}
