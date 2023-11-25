package com.networknt.handler.conduit;

import com.networknt.handler.ResponseInterceptor;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.channels.StreamSourceChannel;
import org.xnio.conduits.AbstractStreamSinkConduit;
import org.xnio.conduits.StreamSinkConduit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class ContentStreamSinkConduit extends AbstractStreamSinkConduit<StreamSinkConduit> {

    private static final Logger LOG = LoggerFactory.getLogger(ContentStreamSinkConduit.class);

    private final StreamSinkConduit _next;

    private final ResponseInterceptor[] interceptors;

    /**
     * Construct a new instance.
     *
     * @param next     the delegate conduit to set
     * @param exchange HttpServerExchange
     */
    public ContentStreamSinkConduit(StreamSinkConduit next, HttpServerExchange exchange) {
        super(next);
        this._next = next;

        // load the interceptors from the service.yml
        interceptors = SingletonServiceFactory.getBeans(ResponseInterceptor.class);

        try {

            if (interceptors != null && interceptors.length > 0)
                for (ResponseInterceptor interceptor : interceptors) {

                    if (LOG.isDebugEnabled())
                        LOG.debug("Executing interceptor " + interceptor.getClass());

                    interceptor.handleRequest(exchange);
                }

        } catch (Exception e) {

            if (LOG.isErrorEnabled())
                LOG.error("Error executing interceptors", e);

            throw new RuntimeException(e);
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return _next.write(src);
    }

    @Override
    public long write(ByteBuffer[] dsts, int offs, int len) throws IOException {
        return _next.write(dsts, offs, len);
    }

    @Override
    public long transferFrom(final FileChannel src, final long position, final long count) throws IOException {
        return _next.transferFrom(src, position, count);
    }

    @Override
    public long transferFrom(final StreamSourceChannel src, final long count, final ByteBuffer throughBuffer) throws IOException {
        return _next.transferFrom(src, count, throughBuffer);
    }

    @Override
    public int writeFinal(ByteBuffer src) throws IOException {
        return _next.writeFinal(src);
    }

    @Override
    public long writeFinal(ByteBuffer[] srcs, int offset, int length) throws IOException {
        return _next.writeFinal(srcs, offset, length);
    }

    @Override
    public void terminateWrites() throws IOException {
        _next.terminateWrites();
    }

}
