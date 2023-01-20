package com.networknt.handler.conduit;

import com.networknt.handler.BuffersUtils;
import com.networknt.handler.ProxyHandler;
import com.networknt.handler.ResponseInterceptor;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.client.ClientConnection;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.proxy.ProxyConnection;
import io.undertow.server.protocol.http.ServerFixedLengthStreamSinkConduit;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.XnioIoThread;
import org.xnio.XnioWorker;
import org.xnio.channels.StreamSourceChannel;
import org.xnio.conduits.StreamSinkChannelWrappingConduit;
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

    private final ResponseInterceptor[] interceptors;

    /**
     * Construct a new instance.
     *
     * @param next     the delegate conduit to set
     * @param exchange
     */
    public ModifiableContentSinkConduit(StreamSinkConduit next, HttpServerExchange exchange) {
        super(next);
        this.exchange = exchange;
        // load the interceptors from the service.yml
        this.interceptors = SingletonServiceFactory.getBeans(ResponseInterceptor.class);
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
            for (var oldBuffer : oldBuffers) {
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
            ByteBuffer srcBuffer = dsts[offs + i];
            if (srcBuffer.hasRemaining()) {
                return write(srcBuffer);
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
        if (this.interceptors == null || this.interceptors.length == 0) {
            next.terminateWrites();
            return;
        }

        if(logger.isTraceEnabled())
            logger.trace("terminating writes with interceptors length = " + (this.interceptors.length));

        try {
            for (var interceptor : this.interceptors) {
                if (logger.isDebugEnabled()) logger.debug("Executing interceptor " + interceptor.getClass());
                interceptor.handleRequest(exchange);
            }
        } catch (Exception e) {
            logger.error("Error executing interceptors: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }

        var dests = this.exchange.getAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY);

        if (logger.isTraceEnabled())
            logger.trace("Next conduit is: {}", next.getClass().getName());

        /* only update content-length header if it exists. Response might have transfer encoding */
        if (this.exchange.getResponseHeaders().get(Headers.CONTENT_LENGTH) != null) {
            this.updateContentLength(this.exchange, dests);
        }

        this.writeToNextConduit(dests);
    }

    /**
     * Writes to the next conduit.
     * We track the position of the buffer after writing because of cases where the conduit does not consume everything.
     * e.g. When transfer is chunked.
     *
     * @param responseDataPooledBuffers - pooled response buffers (after modification)
     * @throws IOException - throws IO exception when writing to next conduits buffers.
     */
    private void writeToNextConduit(final PooledByteBuffer[] responseDataPooledBuffers) throws IOException {

        /* http2 uses StreamSinkChannelWrappingConduit. */
        if (!(next instanceof StreamSinkChannelWrappingConduit)) {
            this.standardHttp11Write(responseDataPooledBuffers);
        } else {
            this.standardHttp2Write(responseDataPooledBuffers);
        }
    }

    private void standardHttp11Write(final PooledByteBuffer[] buffers) throws IOException {
        for (var buffer : buffers) {

            if (buffer == null) {
                break;
            }

            if (logger.isTraceEnabled())
                logger.trace("buffer position {} and buffer limit {}",
                        buffer.getBuffer().position(),
                        buffer.getBuffer().limit());

            while (buffer.getBuffer().position() < buffer.getBuffer().limit()) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Before write buffer position: {}", buffer.getBuffer().position());
                }

                next.write(buffer.getBuffer());

                if (logger.isTraceEnabled()) {
                    logger.trace("After write buffer position: {}", buffer.getBuffer().position());
                }
            }
        }

        next.terminateWrites();
    }

    private void standardHttp2Write(final PooledByteBuffer[] buffers) throws IOException {

        final XnioIoThread ioThread = next.getWriteThread();

        final XnioWorker workerThread = next.getWriteThread().getWorker();

        if (ioThread == Thread.currentThread()) {
            workerThread.execute(() -> {
                try {
                    int index = 0;
                    long totalWritten = 0;
                    for (var buffer : buffers) {
                        if (buffer == null || buffer.getBuffer() == null) {
                            break;
                        }
                        boolean lastWrite = false;
                        long written = 0;
                        if (logger.isTraceEnabled()) {
                            logger.trace("---BEFORE-WRITE---");
                            logger.trace("Bytes written in pass: '{}' bytes", written);
                            logger.trace("Bytes written in total: '{}' bytes", totalWritten);
                            logger.trace("Current Buffer Size: {} bytes", buffer.getBuffer().limit());
                            logger.trace("------------------");
                        }

                        while (buffer.getBuffer().position() < buffer.getBuffer().limit()) {
                            long res;
                            if (buffers[index + 1] == null || buffers[index + 1].getBuffer() == null) {
                                res = next.writeFinal(buffer.getBuffer());
                                lastWrite = true;
                            } else {
                                res = next.write(buffer.getBuffer());
                            }
                            written += res;
                            if (!(res == 0L) && (buffer.getBuffer().position() >= buffer.getBuffer().limit()))
                                break;
                            next.awaitWritable();

                        }
                        totalWritten += written;

                        if (logger.isTraceEnabled()) {
                            logger.trace("----AFTER-WRITE----");
                            logger.trace("Bytes written in pass: '{}' bytes", written);
                            logger.trace("Bytes written in total: '{}' bytes", totalWritten);
                            logger.trace("Current Buffer Index: '{}'", index + 1);
                            logger.trace("------------------");
                        }

                        index++;

                        if (lastWrite) {
                            if (logger.isTraceEnabled())
                                logger.trace("Final write occurred. Terminating writes.");
                            break;
                        }

                    }
                    next.terminateWrites();
                    this.cancelProxyConnectionCallback();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

        } else {
            for (var buffer : buffers) {
                if (buffer != null && buffer.getBuffer() != null) {
                    while (next.write(buffer.getBuffer()) == 0L) {
                        next.awaitWritable();
                    }
                }
            }
            next.terminateWrites();
            this.cancelProxyConnectionCallback();
        }
    }

    /**
     * Cancels the callback attachments from the ProxyHandler.
     */
    private void cancelProxyConnectionCallback() {
        final ProxyConnection connectionAttachment = exchange.getAttachment(ProxyHandler.CONNECTION);

        if (connectionAttachment != null) {

            if (logger.isTraceEnabled())
                logger.trace("Proxy connection found. Removing cancel callback.");

            ClientConnection clientConnection = connectionAttachment.getConnection();
            IoUtils.safeClose(clientConnection);
            var timeout = this.exchange.getAttachment(ProxyHandler.TIMEOUT_KEY);
            timeout.remove();
        }
    }

    /**
     * Calculates the length of the buffered data and updates the content-length header.
     * Do not call this method when content-length is not already set in the response. This is to preserve transfer-encoding restrictions.
     *
     * @param exchange - current http exchange.
     * @param dests - the updated buffered response data.
     */
    private void updateContentLength(HttpServerExchange exchange, PooledByteBuffer[] dests) {
        long length = 0;

        for (PooledByteBuffer dest : dests) {
            if (dest != null) {
                length += dest.getBuffer().limit();
            }
        }

        if(logger.isTraceEnabled())
            logger.trace("PooledByteBuffer array added up length = " + length);

        // only when content length is already in the response headers and the value is not null, we update the value. We don't want to
        // introduce a new header that doesn't exist. We update the length just in case that response body transformer updated the body.
        exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, length);

        // need also to update length of ServerFixedLengthStreamSinkConduit.
        // Should we do this for anything that extends AbstractFixedLengthStreamSinkConduit?
        if (this.next instanceof ServerFixedLengthStreamSinkConduit) {
            Method m;
            if(logger.isTraceEnabled()) logger.trace("The next conduit is ServerFixedLengthStreamSinkConduit and reset the length.");
            try {
                m = ServerFixedLengthStreamSinkConduit.class.getDeclaredMethod("reset", long.class, HttpServerExchange.class);
                m.setAccessible(true);
            } catch (NoSuchMethodException | SecurityException ex) {
                logger.error("could not find ServerFixedLengthStreamSinkConduit.reset method", ex);
                throw new RuntimeException("could not find ServerFixedLengthStreamSinkConduit.reset method", ex);
            }

            try {
                m.invoke(next, length, exchange);
                if(logger.isTraceEnabled()) logger.trace("reset ServerFixedLengthStreamSinkConduit length = " + length);
            } catch (Throwable ex) {
                logger.error("could not access BUFFERED_REQUEST_DATA field", ex);
                throw new RuntimeException("could not access BUFFERED_REQUEST_DATA field", ex);
            }
        } else {
            logger.warn("updateContentLength() next is {}", this.next.getClass().getSimpleName());
        }
    }

}
