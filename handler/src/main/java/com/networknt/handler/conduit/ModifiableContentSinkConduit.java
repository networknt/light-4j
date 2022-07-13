package com.networknt.handler.conduit;

import com.networknt.handler.BuffersUtils;
import com.networknt.handler.ResponseInterceptorHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.protocol.http.ServerFixedLengthStreamSinkConduit;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSourceChannel;
import org.xnio.conduits.AbstractStreamSinkConduit;
import org.xnio.conduits.ConduitWritableByteChannel;
import org.xnio.conduits.Conduits;
import org.xnio.conduits.StreamSinkConduit;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class ModifiableContentSinkConduit extends AbstractStreamSinkConduit<StreamSinkConduit> {
    public static int MAX_BUFFERS = 1024;

    static final Logger logger = LoggerFactory.getLogger(ModifiableContentSinkConduit.class);

    private final HttpServerExchange exchange;

    private final ResponseInterceptorHandler[] interceptors;

    /**
     * Construct a new instance.
     *
     * @param next the delegate conduit to set
     * @param exchange
     */
    public ModifiableContentSinkConduit(StreamSinkConduit next, HttpServerExchange exchange) {
        super(next);
        this.exchange = exchange;
        // load the interceptors from the service.yml
        interceptors = SingletonServiceFactory.getBeans(ResponseInterceptorHandler.class);
        resetBufferPool(exchange);
    }

    /**
     * init buffers pool with a single, empty buffer.
     *
     * @param exchange
     * @return
     */
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
        exchange.putAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY, new PooledByteBuffer[MAX_BUFFERS]);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return BuffersUtils.append(src, exchange.getAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY), exchange);
    }

    @Override
    public long write(ByteBuffer[] dsts, int offs, int len) throws IOException {
        for (int i = offs; i < len; ++i) {
            if (dsts[i].hasRemaining()) {
                return write(dsts[i]);
            }
        }
        return 0;
    }

    @Override
    public long transferFrom(final FileChannel src, final long position, final long count) throws IOException {
        return src.transferTo(position, count, new ConduitWritableByteChannel(this));
    }

    @Override
    public long transferFrom(final StreamSourceChannel source, final long count, final ByteBuffer throughBuffer) throws IOException {
        return IoUtils.transfer(source, count, throughBuffer, new ConduitWritableByteChannel(this));
    }

    @Override
    public int writeFinal(ByteBuffer src) throws IOException {
        return Conduits.writeFinalBasic(this, src);
    }

    @Override
    public long writeFinal(ByteBuffer[] srcs, int offset, int length) throws IOException {
        return Conduits.writeFinalBasic(this, srcs, offset, length);
    }

    @Override
    public void terminateWrites() throws IOException {
        logger.info("terminating writes");
        try {
            if(interceptors.length > 0) {
                // iterate all interceptor handlers.
                for(ResponseInterceptorHandler interceptor : interceptors) {
                    if(logger.isDebugEnabled()) logger.debug("Executing interceptor " + interceptor.getClass());
                    interceptor.handleRequest(exchange);
                }
            }
        } catch (Exception e) {
            logger.error("Error executing interceptors", e);
            // ByteArrayProxyRequest.of(exchange).setInError(true);
            throw new RuntimeException(e);
        }

        var dests = exchange.getAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY);

        updateContentLength(exchange, dests);

        for (PooledByteBuffer dest : dests) {
            if (dest != null) {
                next.write(dest.getBuffer());
            }
        }

        next.terminateWrites();
    }

    private void updateContentLength(HttpServerExchange exchange, PooledByteBuffer[] dests) {
        long length = 0;

        for (PooledByteBuffer dest : dests) {
            if (dest != null) {
                length += dest.getBuffer().limit();
            }
        }

        exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, length);

        // need also to update lenght of ServerFixedLengthStreamSinkConduit
        if (next instanceof ServerFixedLengthStreamSinkConduit) {
            Method m;

            try {
                m = ServerFixedLengthStreamSinkConduit.class.getDeclaredMethod("reset", long.class, HttpServerExchange.class);
                m.setAccessible(true);
            } catch (NoSuchMethodException | SecurityException ex) {
                logger.error("could not find ServerFixedLengthStreamSinkConduit.reset method", ex);
                throw new RuntimeException("could not find ServerFixedLengthStreamSinkConduit.reset method", ex);
            }

            try {
                m.invoke(next, length, exchange);
            } catch (Throwable ex) {
                logger.error("could not access BUFFERED_REQUEST_DATA field", ex);
                throw new RuntimeException("could not access BUFFERED_REQUEST_DATA field", ex);
            }
        } else {
            logger.warn("updateContentLenght() next is {}", next.getClass().getSimpleName());
        }
    }

}
