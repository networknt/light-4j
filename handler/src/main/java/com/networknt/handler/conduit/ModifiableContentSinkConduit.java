package com.networknt.handler.conduit;

import com.networknt.handler.BuffersUtils;
import com.networknt.handler.ResponseInterceptor;
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
import org.xnio.conduits.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * A conduit that allows modification of the response content before it is written to the next conduit.
 */
public class ModifiableContentSinkConduit extends AbstractStreamSinkConduit<StreamSinkConduit> {
    /** Maximum number of buffers allowed for modifiable content */
    public static int MAX_BUFFERS = 1024;

    static final Logger LOG = LoggerFactory.getLogger(ModifiableContentSinkConduit.class);

    private final HttpServerExchange exchange;

    private final ResponseInterceptor[] interceptors;

    private volatile boolean writingResponse = false;

    private final Object lock = new Object();

    private PooledByteBuffer[] http2ResponseBuffers;

    private int http2ResponseBufferIndex;

    private boolean http2WriteReadyHandlerInstalled;

    private boolean http2Terminating;

    private boolean http2Complete;

    /**
     * Construct a new instance.
     *
     * @param next     the delegate conduit to set
     * @param exchange HttpServerExchange
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
        if (oldBuffers != null)
            for (var oldBuffer : oldBuffers)
                if (oldBuffer != null)
                    oldBuffer.close();

        exchange.putAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY, new PooledByteBuffer[MAX_BUFFERS]);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return BuffersUtils.append(src, exchange.getAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY), exchange);
    }

    @Override
    public long write(ByteBuffer[] dsts, int offs, int len) throws IOException {

        for (int i = offs; i < len; ++i) {
            var srcBuffer = dsts[offs + i];

            if (srcBuffer.hasRemaining())
                return write(srcBuffer);
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

        if (this.interceptors == null || this.interceptors.length == 0)
            next.terminateWrites();

        else if (!isWritingResponse()) {

            synchronized (lock) {
                this.writingResponse = true;
            }

            if (LOG.isTraceEnabled())
                LOG.trace("terminating writes with interceptors length = " + (this.interceptors.length));

            try {

                for (var interceptor : this.interceptors) {

                    if (LOG.isDebugEnabled())
                        LOG.debug("Executing interceptor " + interceptor.getClass());

                    interceptor.handleRequest(exchange);
                }

            } catch (Exception e) {

                if (LOG.isErrorEnabled())
                    LOG.error("Error executing interceptors: " + e.getMessage(), e);

                throw new RuntimeException(e);
            }

            var dests = this.exchange.getAttachment(AttachmentConstants.BUFFERED_RESPONSE_DATA_KEY);

            if (LOG.isTraceEnabled())
                LOG.trace("Next conduit is: {}", next.getClass().getName());

            /* only update content-length header if it exists. Response might have transfer encoding */
            if (this.exchange.getResponseHeaders().get(Headers.CONTENT_LENGTH) != null)
                this.updateContentLength(this.exchange, dests);

            this.writeToNextConduit(dests);
        }
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
        if (!(this.next instanceof StreamSinkChannelWrappingConduit))
            this.http1Write(responseDataPooledBuffers);

        else this.http2Write(responseDataPooledBuffers);
    }

    private void http1Write(final PooledByteBuffer[] buffers) throws IOException {

        for (var buffer : buffers) {

            if (buffer == null)
                break;

            if (LOG.isTraceEnabled())
                LOG.trace("buffer position {} and buffer limit {}", buffer.getBuffer().position(), buffer.getBuffer().limit());

            while (buffer.getBuffer().position() < buffer.getBuffer().limit()) {

                if (LOG.isTraceEnabled())
                    LOG.trace("Before write buffer position: {}", buffer.getBuffer().position());

                next.write(buffer.getBuffer());

                if (LOG.isTraceEnabled())
                    LOG.trace("After write buffer position: {}", buffer.getBuffer().position());

            }

        }

        next.terminateWrites();


    }

    private void http2Write(final PooledByteBuffer[] buffers) {
        this.http2ResponseBuffers = buffers;
        this.http2ResponseBufferIndex = 0;
        this.http2Terminating = false;
        this.http2Complete = false;
        this.installHttp2WriteReadyHandler();
        this.runOnWriteThread(this::drainHttp2Response);
    }

    private void installHttp2WriteReadyHandler() {
        if (this.http2WriteReadyHandlerInstalled)
            return;

        this.next.setWriteReadyHandler(new WriteReadyHandler() {
            @Override
            public void writeReady() {
                ModifiableContentSinkConduit.this.runOnWriteThread(ModifiableContentSinkConduit.this::drainHttp2Response);
            }

            @Override
            public void forceTermination() {
                ModifiableContentSinkConduit.this.failHttp2Response(new IOException("HTTP/2 downstream write was forcibly terminated."));
            }

            @Override
            public void terminated() {
                ModifiableContentSinkConduit.this.closeRemainingBuffers();
            }
        });

        this.http2WriteReadyHandlerInstalled = true;
    }

    private void runOnWriteThread(Runnable task) {
        if (this.next.getWriteThread() == Thread.currentThread())
            task.run();
        else
            this.next.getWriteThread().execute(task);
    }

    private void drainHttp2Response() {
        if (this.http2Complete)
            return;

        try {
            if (this.http2Terminating) {
                this.completeHttp2Response();
                return;
            }

            while (this.http2ResponseBuffers != null && this.http2ResponseBufferIndex < this.http2ResponseBuffers.length) {
                PooledByteBuffer pooled = this.http2ResponseBuffers[this.http2ResponseBufferIndex];

                if (pooled == null || pooled.getBuffer() == null) {
                    this.completeHttp2Response();
                    return;
                }

                ByteBuffer buffer = pooled.getBuffer();

                while (buffer.hasRemaining()) {
                    int written = this.next.write(buffer);

                    if (written == 0) {
                        this.next.resumeWrites();
                        return;
                    }
                }

                pooled.close();
                this.http2ResponseBuffers[this.http2ResponseBufferIndex] = null;
                this.http2ResponseBufferIndex++;
            }

            this.completeHttp2Response();
        } catch (IOException | RuntimeException e) {
            this.failHttp2Response(e);
        }
    }

    private void completeHttp2Response() throws IOException {
        if (this.http2Complete)
            return;

        if (!this.http2Terminating) {
            this.http2Terminating = true;

            if (LOG.isTraceEnabled())
                LOG.trace("Terminating HTTP/2 response writes...");

            this.next.terminateWrites();
        }

        if (!this.next.flush()) {
            this.next.resumeWrites();
            return;
        }

        this.next.suspendWrites();
        this.http2Complete = true;
        this.closeRemainingBuffers();
    }

    private void failHttp2Response(Exception e) {
        if (LOG.isErrorEnabled())
            LOG.error("Failed to write intercepted HTTP/2 response.", e);

        this.http2Complete = true;
        this.closeRemainingBuffers();

        try {
            this.next.truncateWrites();
        } catch (IOException ex) {
            if (LOG.isDebugEnabled())
                LOG.debug("Failed to truncate HTTP/2 response writes after write failure.", ex);
        }
    }

    private void closeRemainingBuffers() {
        if (this.http2ResponseBuffers == null)
            return;

        for (int i = this.http2ResponseBufferIndex; i < this.http2ResponseBuffers.length; i++) {
            PooledByteBuffer pooled = this.http2ResponseBuffers[i];

            if (pooled == null)
                continue;

            try {
                pooled.close();
            } catch (RuntimeException e) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Failed to close HTTP/2 response buffer.", e);
            } finally {
                this.http2ResponseBuffers[i] = null;
            }
        }

        this.http2ResponseBuffers = null;
    }

    /**
     * Returns true if the conduit is currently writing a response.
     *
     * @return true if writing response, false otherwise.
     */
    private boolean isWritingResponse() {
        return writingResponse;
    }

    /**
     * Calculates the length of the buffered data and updates the content-length header.
     * Do not call this method when content-length is not already set in the response. This is to preserve transfer-encoding restrictions.
     *
     * @param exchange - current http exchange.
     * @param dests    - the updated buffered response data.
     */
    private void updateContentLength(HttpServerExchange exchange, PooledByteBuffer[] dests) {
        long length = 0;

        for (var dest : dests)
            if (dest != null)
                length += dest.getBuffer().limit();

        if (LOG.isTraceEnabled())
            LOG.trace("PooledByteBuffer array added up length = " + length);

        exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, length);

        // need also to update length of ServerFixedLengthStreamSinkConduit.
        // Should we do this for anything that extends AbstractFixedLengthStreamSinkConduit?
        if (this.next instanceof ServerFixedLengthStreamSinkConduit) {
            Method m;

            if (LOG.isTraceEnabled())
                LOG.trace("The next conduit is ServerFixedLengthStreamSinkConduit and reset the length.");

            try {
                m = ServerFixedLengthStreamSinkConduit.class.getDeclaredMethod("reset", long.class, HttpServerExchange.class);
                m.setAccessible(true);

            } catch (NoSuchMethodException | SecurityException ex) {

                if (LOG.isErrorEnabled())
                    LOG.error("could not find ServerFixedLengthStreamSinkConduit.reset method", ex);

                throw new RuntimeException("could not find ServerFixedLengthStreamSinkConduit.reset method", ex);
            }

            try {
                m.invoke(next, length, exchange);

                if (LOG.isTraceEnabled())
                    LOG.trace("reset ServerFixedLengthStreamSinkConduit length = " + length);

            } catch (Throwable ex) {

                if (LOG.isErrorEnabled())
                    LOG.error("could not access BUFFERED_REQUEST_DATA field", ex);

                throw new RuntimeException("could not access BUFFERED_REQUEST_DATA field", ex);
            }

        } else if (LOG.isWarnEnabled())
            LOG.warn("updateContentLength() next is {}", this.next.getClass().getSimpleName());

    }

}
