package com.networknt.handler.conduit;

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

    static final Logger LOGGER = LoggerFactory.getLogger(ContentStreamSinkConduit.class);

    private final StreamSinkConduit _next;

    private final ResponseInterceptorsExecutor responseInterceptorsExecutor = new ResponseInterceptorsExecutor(true);

    /**
     * Construct a new instance.
     *
     * @param next
     * @param exchange
     */
    public ContentStreamSinkConduit(StreamSinkConduit next, HttpServerExchange exchange) {
        super(next);
        this._next = next;

        try {
            this.responseInterceptorsExecutor.handleRequest(exchange);
        } catch (Exception e) {
            LOGGER.error("Error executing interceptors", e);
            ByteArrayProxyRequest.of(exchange).setInError(true);
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
