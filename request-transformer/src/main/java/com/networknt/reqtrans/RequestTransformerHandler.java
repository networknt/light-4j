package com.networknt.reqtrans;

import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.RequestInterceptorHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.protocol.http.HttpContinue;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.Buffers;
import java.nio.ByteBuffer;

/**
 * Transforms the request body of an active request being processed.
 * This is executed by RequestInterceptorExecutionHandler.
 *
 * @author Kalev Gonvick
 *
 */
public class RequestTransformerHandler implements RequestInterceptorHandler {
    static final Logger logger = LoggerFactory.getLogger(RequestTransformerHandler.class);

    private RequestTransformerConfig config;
    private volatile HttpHandler next;

    public RequestTransformerHandler() {
        if(logger.isInfoEnabled()) logger.info("RequestManipulatorHandler is loaded");
        config = RequestTransformerConfig.load();
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
        ModuleRegistry.registerModule(RequestTransformerHandler.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void reload() {
        config.reload();
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        if (!httpServerExchange.isRequestComplete() && !HttpContinue.requiresContinueResponse(httpServerExchange.getRequestHeaders())) {

            // This object contains the reference to the request data buffer. Any modification done to this will be reflected in the request.
            PooledByteBuffer[] requestData = this.getBuffer(httpServerExchange);

            logger.info("RequestTransformerHandler.handleRequest is called.");

            // Create a new buffer that will replace our request body.
            String s = "[{\"com.networknt.handler.RequestInterceptorHandler\":[\"com.networknt.restrans.RequestTransformerHandler\"]}]";
            ByteBuffer overwriteData = ByteBuffer.wrap(s.getBytes());

            // Do the overwrite operation by copying our overwriteData to the source buffer pool.
            int pidx = 0;
            while (overwriteData.hasRemaining() && pidx < requestData.length) {
                ByteBuffer _dest;
                if (requestData[pidx] == null) {
                    requestData[pidx] = httpServerExchange.getConnection().getByteBufferPool().allocate();
                    _dest = requestData[pidx].getBuffer();
                } else {
                    _dest = requestData[pidx].getBuffer();
                    _dest.clear();
                }
                Buffers.copy(_dest, overwriteData);
                _dest.flip();
                pidx++;
            }
            while (pidx < requestData.length) {
                requestData[pidx] = null;
                pidx++;
            }

            // We need to update the content length.
            long length = 0;
            for (PooledByteBuffer dest : requestData) {
                if (dest != null) {
                    length += dest.getBuffer().limit();
                }
            }
            httpServerExchange.getRequestHeaders().put(Headers.CONTENT_LENGTH, length);
        }
    }

    @Override
    public boolean isRequiredContent() {
        return true;
    }

    public PooledByteBuffer[] getBuffer(HttpServerExchange exchange) {
        PooledByteBuffer[] buffer = exchange.getAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY);
        if (buffer == null) {
            throw new IllegalStateException("Request content is not available in exchange attachment as there is no interceptors.");
        }
        return buffer;
    }
}
