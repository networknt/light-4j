package com.networknt.handler;

import com.networknt.httpstring.AttachmentConstants;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.UndertowLogger;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.Connectors;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.protocol.http.HttpContinue;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSourceChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Injects the content from the request channel as an attachment. Content is only
 * read if one of the set request interceptors returns isContentRequired() as true.
 *
 * @author Kalev Gonvick
 *
 */
public class RequestContentInjectorHandler implements MiddlewareHandler {

    static final Logger logger = LoggerFactory.getLogger(RequestContentInjectorHandler.class);
    public static final int MAX_BUFFERS = 1024;
    private volatile HttpHandler next;
    private RequestContentInjectorConfig config;
    private RequestInterceptorHandler[] interceptors = null;

    public RequestContentInjectorHandler() {
        config = RequestContentInjectorConfig.load();
        logger.info("SourceConduitInjectorHandler is loaded!");
        interceptors = SingletonServiceFactory.getBeans(RequestInterceptorHandler.class);
    }

    public RequestContentInjectorHandler(RequestContentInjectorConfig cfg) {
        config = cfg;
        logger.info("SourceConduitInjectorHandler is loaded!");
        interceptors = SingletonServiceFactory.getBeans(RequestInterceptorHandler.class);
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
        ModuleRegistry.registerModule(RequestContentInjectorConfig.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void reload() {
        config.reload();
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

        // Make sure content is needed by request interceptors before grabbing the data. The process has a lot of overhead.
        if (this.injectorContentRequired()
                && !httpServerExchange.isRequestComplete()
                && !HttpContinue.requiresContinueResponse(httpServerExchange.getRequestHeaders())) {

            this.next = Handler.getNext(httpServerExchange);
            final StreamSourceChannel channel = httpServerExchange.getRequestChannel();
            int readBuffers = 0;
            final PooledByteBuffer[] bufferedData = new PooledByteBuffer[MAX_BUFFERS];
            PooledByteBuffer buffer = httpServerExchange
                    .getConnection()
                    .getByteBufferPool()
                    .allocate();
            try {
                for (; ; ) {
                    int r;
                    ByteBuffer b = buffer.getBuffer();
                    r = channel.read(b);
                    if (r == -1) {
                        if (b.position() == 0) {
                            buffer.close();
                        } else {
                            b.flip();
                            bufferedData[readBuffers] = buffer;
                        }
                        break;
                    } else if (r == 0) {
                        this.setChannelRead(channel, buffer, readBuffers, bufferedData, httpServerExchange);
                        channel.resumeReads();
                        return;
                    } else if (!b.hasRemaining()) {
                        b.flip();
                        bufferedData[readBuffers++] = buffer;
                        if (readBuffers == MAX_BUFFERS) {
                            break;
                        }
                        buffer = httpServerExchange.getConnection().getByteBufferPool().allocate();
                    }
                }

                saveBufferAndResetUndertowConnector(httpServerExchange, bufferedData);
            } catch (Exception | Error e) {
                safeCloseBuffers(bufferedData, buffer);
                throw e;
            }

        }
        Handler.next(httpServerExchange, next);
    }

    /**
     * Check if any of the interceptors require content.
     * @return - true if required.
     */
    private boolean injectorContentRequired() {
        return interceptors.length > 0 && Arrays.stream(interceptors).anyMatch(RequestInterceptorHandler::isRequiredContent);
    }

    /**
     * Create a new read channel listener for the request channel. This is needed for 'chunked' requests larger than our server buffer set.
     *
     * @param channel - the request channel.
     * @param channelPoolBuffer - pool buffer.
     * @param channelReadBuffer - our read.
     * @param bufferedData - total buffered data.
     * @param httpServerExchange - current exchange.
     */
    private void setChannelRead(final StreamSourceChannel channel, final PooledByteBuffer channelPoolBuffer, final int channelReadBuffer, final PooledByteBuffer[] bufferedData, final HttpServerExchange httpServerExchange) {
        channel.getReadSetter().set(new ChannelListener<StreamSourceChannel>() {
            PooledByteBuffer buffer = channelPoolBuffer;
            int readBuffers = channelReadBuffer;

            @Override
            public void handleEvent(StreamSourceChannel channel) {
                try {
                    do {
                        int r;
                        ByteBuffer b = buffer.getBuffer();
                        r = channel.read(b);
                        if (r == -1) {
                            if (b.position() == 0) {
                                buffer.close();
                            } else {
                                b.flip();
                                bufferedData[readBuffers] = buffer;
                            }
                            suspendReads(httpServerExchange, bufferedData, channel, next);
                            return;
                        } else if (r == 0) {
                            return;
                        } else if (!b.hasRemaining()) {
                            b.flip();
                            bufferedData[readBuffers++] = buffer;
                            if (readBuffers == MAX_BUFFERS) {
                                suspendReads(httpServerExchange, bufferedData, channel, next);
                                return;
                            }
                            buffer = httpServerExchange.getConnection().getByteBufferPool().allocate();
                        }
                    } while (true);
                } catch (Throwable e) {
                    if (e instanceof IOException) {
                        UndertowLogger.REQUEST_IO_LOGGER.ioException((IOException) e);
                    } else {
                        UndertowLogger.REQUEST_IO_LOGGER.handleUnexpectedFailure(e);
                    }
                    safeCloseBuffers(bufferedData, buffer);
                    httpServerExchange.endExchange();
                }
            }
        });
    }

    /**
     * Close our buffers when issue occurs
     *
     * @param allDataBuffer - the total buffer
     * @param dataBuffer - the current data buffer
     */
    private static void safeCloseBuffers(final PooledByteBuffer[] allDataBuffer, PooledByteBuffer dataBuffer) {
        for (PooledByteBuffer bufferedDatum : allDataBuffer) {
            IoUtils.safeClose(bufferedDatum);
        }
        if (dataBuffer != null && dataBuffer.isOpen()) {
            IoUtils.safeClose(dataBuffer);
        }
    }

    /**
     * Suspend our reads and remove the channel listener we created.
     *
     * @param httpServerExchange - current exchange
     * @param bufferedData - total buffered data.
     * @param channel - request channel
     * @param next - next http handler
     */
    private static void suspendReads(final HttpServerExchange httpServerExchange, final PooledByteBuffer[] bufferedData, StreamSourceChannel channel, HttpHandler next) {
        saveBufferAndResetUndertowConnector(httpServerExchange, bufferedData);
        channel.getReadSetter().set(null);
        channel.suspendReads();
        Connectors.executeRootHandler(next, httpServerExchange);
    }


    /**
     * Save the total buffer as an attachment. Update content length just in case
     *
     * @param httpServerExchange - current exchange
     * @param bufferedData - total buffered data
     */
    private static void saveBufferAndResetUndertowConnector(final HttpServerExchange httpServerExchange, final PooledByteBuffer[] bufferedData) {
        httpServerExchange.putAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY, bufferedData);
        long length = 0;
        for (PooledByteBuffer dest : bufferedData) {
            if (dest != null) {
                length += dest.getBuffer().limit();
            }
        }
        httpServerExchange.getRequestHeaders().put(Headers.CONTENT_LENGTH, length);

        Connectors.ungetRequestBytes(httpServerExchange, bufferedData);
        Connectors.resetRequestChannel(httpServerExchange);
    }
}
