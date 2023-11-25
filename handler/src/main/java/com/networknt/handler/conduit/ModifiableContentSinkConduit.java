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
import org.xnio.XnioWorker;
import org.xnio.channels.StreamSourceChannel;
import org.xnio.conduits.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class ModifiableContentSinkConduit extends AbstractStreamSinkConduit<StreamSinkConduit> {
    public static int MAX_BUFFERS = 1024;

    static final Logger LOG = LoggerFactory.getLogger(ModifiableContentSinkConduit.class);

    private final HttpServerExchange exchange;

    private final ResponseInterceptor[] interceptors;

    private volatile boolean writingResponse = false;

    private final Object lock = new Object();

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
        final var ioThread = next.getWriteThread();
        final var workerThread = next.getWorker();

        if (ioThread == Thread.currentThread())
            this.executeHttp2WriteThread(workerThread, buffers);

        else throw new IllegalStateException("Conduit should not be called in a non IO-thread...");
    }

    private void executeHttp2WriteThread(XnioWorker workerThread, final PooledByteBuffer[] buffers) {
        workerThread.execute(() -> {

            try {
                int index = 0;
                long totalWritten = 0;

                for (var buffer : buffers) {

                    if (buffer == null || buffer.getBuffer() == null)
                        break;

                    var written = 0;

                    if (LOG.isTraceEnabled())
                        LOG.trace("[{}] Before-Write: current pass: '{}' bytes, total: '{}' bytes, buffer size: '{}' bytes", index, written, totalWritten, buffer.getBuffer().limit());

                    var lastWrite = this.doWrite(buffers, buffer, written, index);

                    totalWritten += written;

                    if (LOG.isTraceEnabled())
                        LOG.trace("[{}] After-Write: current pass: '{}' bytes, total: '{}' bytes, buffer size: '{}' bytes", index, written, totalWritten, buffer.getBuffer().limit());

                    buffer.close();
                    index++;

                    if (lastWrite)
                        break;
                }

                if (LOG.isTraceEnabled())
                    LOG.trace("Terminating writes...");

                next.terminateWrites();
            } catch (IOException e) {
                throw new RuntimeException("Failed to execute conduit writes on Worker Thread. " + e.getMessage(), e);
            }
        });
    }

    private boolean doWrite(PooledByteBuffer[] buffers, PooledByteBuffer buffer, int written, int index) throws IOException {
        boolean lastWrite = false;
        while (buffer.getBuffer().position() < buffer.getBuffer().limit()) {
            long res;

            if (this.isLastWrite(buffers, index)) {

                if (LOG.isTraceEnabled())
                    LOG.trace("Final write occurred...");

                res = next.write(buffer.getBuffer());
                lastWrite = true;

            } else res = next.write(buffer.getBuffer());

            written += res;

            if (this.isBufferConsumed(buffer, res, index))
                break;

            next.awaitWritable();
        }
        return lastWrite;
    }

    private boolean isBufferConsumed(PooledByteBuffer buffer, long res, int index) {

        if (LOG.isTraceEnabled())
            LOG.trace("[{}] Checking if the buffer was fully consumed...", index);

        return !(res == 0L) && (buffer.getBuffer().position() >= buffer.getBuffer().limit());
    }

    private boolean isLastWrite(PooledByteBuffer[] buffers, int index) {

        if (LOG.isTraceEnabled())
            LOG.trace("[{}] Checking if this is the last write....", index);

        return buffers[index + 1] == null || buffers[index + 1].getBuffer() == null;
    }

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
