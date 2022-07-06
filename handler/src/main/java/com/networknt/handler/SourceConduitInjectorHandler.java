package com.networknt.handler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.UndertowLogger;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.Connectors;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.protocol.http.HttpContinue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSourceChannel;

import java.io.IOException;
import java.nio.ByteBuffer;

public class SourceConduitInjectorHandler implements MiddlewareHandler {


    static final Logger logger = LoggerFactory.getLogger(SourceConduitInjectorHandler.class);
    public static final int MAX_BUFFERS = 1024;
    private volatile HttpHandler next;
    private SourceConduitConfig config;
    public SourceConduitInjectorHandler() {
        config = SourceConduitConfig.load();
        logger.info("SourceConduitInjectorHandler is loaded!");
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
        ModuleRegistry.registerModule(SinkConduitConfig.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void reload() {
        config.reload();
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

        if(!httpServerExchange.isRequestComplete() && !HttpContinue.requiresContinueResponse(httpServerExchange.getRequestHeaders())) {
            this.next = Handler.getNext(httpServerExchange);
            final StreamSourceChannel channel = httpServerExchange.getRequestChannel();
            int readBuffers = 0;
            final PooledByteBuffer[] bufferedData = new PooledByteBuffer[MAX_BUFFERS];
            PooledByteBuffer buffer = httpServerExchange
                    .getConnection()
                    .getByteBufferPool()
                    .allocate();
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
                } while (true);
                saveBufferAndResetUndertowConnector(httpServerExchange, bufferedData);
            } catch (Exception | Error e) {
                safeCloseBuffers(bufferedData, buffer);
                throw e;
            }
        }
        next.handleRequest(httpServerExchange);
        Handler.next(httpServerExchange, next);
    }

    private void setChannelRead(final StreamSourceChannel channel, final PooledByteBuffer channelPoolBuffer, final int channelReadBuffer, final PooledByteBuffer[] bufferedData, final HttpServerExchange httpServerExchange) {
        channel.getReadSetter().set(new ChannelListener<StreamSourceChannel>() {
            PooledByteBuffer listenerPoolBuffer = channelPoolBuffer;
            int listenerReadBuffer = channelReadBuffer;
            @Override
            public void handleEvent(StreamSourceChannel channel) {
                try {
                    do {
                        int r;
                        ByteBuffer b = listenerPoolBuffer.getBuffer();
                        r = channel.read(b);
                        if (r == -1) {
                            if (b.position() == 0) {
                                listenerPoolBuffer.close();
                            } else {
                                b.flip();
                                bufferedData[listenerReadBuffer] = listenerPoolBuffer;
                            }
                            suspendReads(httpServerExchange, bufferedData, channel, next);
                            return;
                        } else if (r == 0) {
                            return;
                        } else if (!b.hasRemaining()) {
                            b.flip();
                            bufferedData[listenerReadBuffer++] = listenerPoolBuffer;
                            if (listenerReadBuffer == MAX_BUFFERS) {
                                suspendReads(httpServerExchange, bufferedData, channel, next);
                                return;
                            }
                            listenerPoolBuffer = httpServerExchange.getConnection().getByteBufferPool().allocate();
                        }
                    } while (true);
                } catch (Throwable e) {
                    if (e instanceof IOException) {
                        UndertowLogger.REQUEST_IO_LOGGER.ioException((IOException) e);
                    } else {
                        UndertowLogger.REQUEST_IO_LOGGER.handleUnexpectedFailure(e);
                    }
                    safeCloseBuffers(bufferedData, listenerPoolBuffer);
                    httpServerExchange.endExchange();
                }
            }
        });
    }

    private static void safeCloseBuffers(final PooledByteBuffer[] allDataBuffer, PooledByteBuffer dataBuffer) {
        for (PooledByteBuffer bufferedDatum : allDataBuffer) {
            IoUtils.safeClose(bufferedDatum);
        }
        if (dataBuffer != null && dataBuffer.isOpen()) {
            IoUtils.safeClose(dataBuffer);
        }
    }

    private static void suspendReads(final HttpServerExchange httpServerExchange, final PooledByteBuffer[] bufferedData, StreamSourceChannel channel, HttpHandler next) {
        saveBufferAndResetUndertowConnector(httpServerExchange, bufferedData);
        channel.getReadSetter().set(null);
        channel.suspendReads();
        Connectors.executeRootHandler(next, httpServerExchange);
    }


    private static void saveBufferAndResetUndertowConnector(final HttpServerExchange httpServerExchange, final PooledByteBuffer[] bufferedData) {
        PooledByteBuffer[] existing = httpServerExchange.getAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY);
        PooledByteBuffer[] newArray;
        if (existing == null) {
            newArray = new PooledByteBuffer[bufferedData.length];
            System.arraycopy(bufferedData, 0, newArray, 0, bufferedData.length);
        } else {
            newArray = new PooledByteBuffer[existing.length + bufferedData.length];
            System.arraycopy(existing, 0, newArray, 0, existing.length);
            System.arraycopy(bufferedData, 0, newArray, existing.length, bufferedData.length);
        }

        httpServerExchange.putAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY, newArray);
        Connectors.ungetRequestBytes(httpServerExchange, bufferedData);
        Connectors.resetRequestChannel(httpServerExchange);
    }
}
