package com.networknt.body;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.UndertowLogger;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.ConduitWrapper;
import io.undertow.server.Connectors;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.ConduitFactory;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSourceChannel;
import org.xnio.conduits.AbstractStreamSourceConduit;
import org.xnio.conduits.StreamSourceConduit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.networknt.body.BodyHandler.REQUEST_BODY_STRING;
import static com.networknt.body.BodyHandler.REQUEST_BODY;

/**
 * This is the Body Parser handler used by the light-proxy and http-sidecar to not only parse
 * the body into an attachment in the exchange but also keep the stream to be forwarded to the
 * backend API. If the normal BodyHandler is used, once the stream is consumed, it is gone and
 * cannot be transfer/forward to the backend with socket to socket transfer.
 * <p>
 * The body validation will only support smaller size JSON request body, so we check the method
 * and content type before applying the logic. For other type of data, we just call the next
 * handler to bypass the body parser so that other type of request will be forwarded to the
 * backend directly.
 * <p>
 * If you are using this handler in a chain, the last handler must be the proxy. If the stream is
 * not forward to the backend API, the event listener won't be triggered and the buffer won't be
 * closed immediately. For one request, this is fine, but with multiple requests in a row, an
 * error will be thrown.
 * <p>
 * In the body.yml config file, we have added skipProxyBodyHandler to allow users to skip this
 * handler if the JSON body is too big to be validated on the proxy side. In general, the handler
 * should be skipped if the body size is bigger than 16K or 64K depending on your company policy.
 *
 * @author Steve Hu
 */
public class ProxyBodyHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(ProxyBodyHandler.class);
    static final String CONTENT_TYPE_MISMATCH = "ERR10015";
    static final String PAYLOAD_TOO_LARGE = "ERR10068";

    public static final BodyConfig config = (BodyConfig) Config.getInstance().getJsonObjectConfig(BodyConfig.CONFIG_NAME, BodyConfig.class);

    private volatile HttpHandler next;

    public ProxyBodyHandler() {
        if (logger.isInfoEnabled()) logger.info("ProxyBodyHandler is loaded.");
    }

    /**
     * Check the header starts with application/json and parse it into map or list
     * based on the first character "{" or "[". Otherwise, check the header starts
     * with application/x-www-form-urlencoded or multipart/form-data and parse it
     * into form-data.
     *
     * @param exchange HttpServerExchange
     * @throws Exception Exception
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        String contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
        exchange.addRequestWrapper(new ConduitWrapper<>() {
            @Override
            public StreamSourceConduit wrap(ConduitFactory<StreamSourceConduit> conduitFactory, HttpServerExchange httpServerExchange) {
                StreamSourceConduit source = conduitFactory.create();
                return new AbstractStreamSourceConduit<>(source) {
                    final ByteArrayOutputStream bufferOut = new ByteArrayOutputStream(config.getMaxBuffers());
                    @Override
                    public int read(ByteBuffer dst) throws IOException {
                        int x = super.read(dst);
                        if(x >= 0) {
                            ByteBuffer duplicateDat = dst.duplicate();
                            duplicateDat.flip();
                            byte[] data = new byte[x];
                            duplicateDat.get(data);
                            bufferOut.write(data);
                        } else {
                            String requestBody = bufferOut.toString(StandardCharsets.UTF_8);
                            logger.debug("request body = " + requestBody);

                            // parse the body to map or list if content type is application/json
                            try {
                                prepParsedBody(exchange, contentType, requestBody);
                            } catch (IOException e) {
                                logger.error("IOException: ", e);
                                setExchangeStatus(exchange, CONTENT_TYPE_MISMATCH, contentType);
                            }
                        }
                        return x;
                    }
                };
            }

            private void prepParsedBody(HttpServerExchange exchange, String contentType, String requestBody) throws IOException {
                if (contentType.startsWith("application/json")) {
                    if (config.isCacheRequestBody()) {
                        exchange.putAttachment(REQUEST_BODY_STRING, requestBody);
                    }
                    // attach the parsed request body into exchange if the body parser is enabled
                    attachJsonBody(exchange, requestBody);
                } else {
                    exchange.putAttachment(REQUEST_BODY, requestBody);
                }
            }

        });
//        if (this.shouldParseBody(exchange)) {
//
//            logger.info("maxBuffer: {}", config.getMaxBuffers());
//            final StreamSourceChannel streamSourceChannel = exchange.getRequestChannel();
//            int bufferIndex = 0;
//            final PooledByteBuffer[] bufferedData = new PooledByteBuffer[config.getMaxBuffers()];
//            PooledByteBuffer pooledByteBuffer = exchange.getConnection().getByteBufferPool().allocate();
//            logger.info("request len: {}", pooledByteBuffer.getBuffer().position());
//            try {
//                for (; ; ) {
//                    ByteBuffer byteBuffer = pooledByteBuffer.getBuffer();
//                    int read = streamSourceChannel.read(byteBuffer);
//
//                    if (read == -1) {
//                        transferBufferToBuffer(bufferedData, byteBuffer, pooledByteBuffer, bufferIndex);
//                        break;
//                    } else if (read == 0) {
//                        final AtomicReference<PooledByteBuffer> pooledByteBufferReference = new AtomicReference<>();
//                        pooledByteBufferReference.set(pooledByteBuffer);
//
//                        final AtomicReference<Integer> bufferIndexReference = new AtomicReference<>();
//                        bufferIndexReference.set(bufferIndex);
//                        streamSourceChannel.getReadSetter().set(new ChannelListener<StreamSourceChannel>() {
//
//                            PooledByteBuffer channelPoolBuffer = pooledByteBufferReference.get();
//                            final int channelBufferIndex = bufferIndexReference.get();
//
//                            @Override
//                            public void handleEvent(StreamSourceChannel channel) {
//                                try {
//                                    for (; ; ) {
//                                        ByteBuffer byteBuffer = channelPoolBuffer.getBuffer();
//                                        int read = channel.read(byteBuffer);
//                                        if (read == -1) {
//                                            transferBufferToBuffer(bufferedData, byteBuffer, channelPoolBuffer, channelBufferIndex);
//                                            this.resetExchangeStream();
//                                            return;
//
//                                        } else if (read == 0) {
//                                            return;
//
//                                        } else if (!byteBuffer.hasRemaining()) {
//                                            byteBuffer.flip();
//                                            bufferedData[channelBufferIndex] = channelPoolBuffer;
//                                            if (channelBufferIndex == config.getMaxBuffers()) {
//                                                this.resetExchangeStream();
//                                                setExchangeStatus(exchange, PAYLOAD_TOO_LARGE, "application/json");
//                                                return;
//                                            }
//                                            channelPoolBuffer = exchange.getConnection().getByteBufferPool().allocate();
//
//                                        }
//                                    }
//                                } catch (Throwable t) {
//                                    if (t instanceof IOException) {
//                                        UndertowLogger.REQUEST_IO_LOGGER.ioException((IOException) t);
//                                    } else {
//                                        UndertowLogger.REQUEST_IO_LOGGER.handleUnexpectedFailure(t);
//                                    }
//                                    safeBufferClose(bufferedData, channelPoolBuffer);
//                                    exchange.endExchange();
//                                }
//                            }
//
//                            private void resetExchangeStream() {
//                                resetRequestChannel(bufferedData, exchange);
//                                streamSourceChannel.getReadSetter().set(null);
//                                streamSourceChannel.suspendReads();
//                                Connectors.executeRootHandler(next, exchange);
//                            }
//                        });
//
//                        streamSourceChannel.resumeReads();
//                        break;
//
//                    } else if (!byteBuffer.hasRemaining()) {
//                        byteBuffer.flip();
//                        bufferedData[bufferIndex++] = pooledByteBuffer;
//                        if (hasExceededMaxBuffer(bufferIndex)) {
//                            resetRequestChannel(bufferedData, exchange);
//                            safeBufferClose(bufferedData, pooledByteBuffer);
//                            setExchangeStatus(exchange, PAYLOAD_TOO_LARGE, "application/json");
//                            return;
//                        }
//                        pooledByteBuffer = exchange.getConnection().getByteBufferPool().allocate();
//                    }
//                }
//
//                resetRequestChannel(bufferedData, exchange);
//            } catch (Exception | Error e) {
//                safeBufferClose(bufferedData, pooledByteBuffer);
//                throw e;
//            }
//
//            String requestBody = StandardCharsets.UTF_8.decode(pooledByteBuffer.getBuffer().duplicate()).toString();
//            logger.debug("request body = " + requestBody);
//
//            // parse the body to map or list if content type is application/json
//            try {
//                this.prepParsedBody(exchange, contentType, requestBody);
//            } catch (IOException e) {
//                logger.error("IOException: ", e);
//                setExchangeStatus(exchange, CONTENT_TYPE_MISMATCH, contentType);
//                return;
//            }
//        }
        next.handleRequest(exchange);
        Handler.next(exchange, next);
    }

    private static boolean hasExceededMaxBuffer(int read) {
        return read == config.getMaxBuffers();
    }


    /**
     * Reset the buffered data and the exchange.
     *
     * @param bufferedData - the buffered exchange data.
     * @param exchange     - http exchange.
     */
    protected static void resetRequestChannel(final PooledByteBuffer[] bufferedData, final HttpServerExchange exchange) {
        Connectors.ungetRequestBytes(exchange, bufferedData);
        Connectors.resetRequestChannel(exchange);
    }

    /**
     * Safely close the fixed length pool buffer and current pooled buffer.
     *
     * @param bufferedData - pooled buffer (fixed len)
     * @param buffer       - pooled buffer from exchange
     */
    protected static void safeBufferClose(final PooledByteBuffer[] bufferedData, PooledByteBuffer buffer) {
        for (PooledByteBuffer bufferedDatum : bufferedData) {
            IoUtils.safeClose(bufferedDatum);
        }
        if (buffer != null && buffer.isOpen()) {
            IoUtils.safeClose(buffer);
        }
    }

    /**
     * Move data from exchange buffer to pooled buffer.
     *
     * @param pooledBuffer   - fixed len. pooled buffer.
     * @param buffer         - our current byte buffer
     * @param exchangeBuffer - buffer from exchange
     * @param i              - index
     */
    protected static void transferBufferToBuffer(final PooledByteBuffer[] pooledBuffer, ByteBuffer buffer, PooledByteBuffer exchangeBuffer, int i) {
        if (buffer.position() == 0) {
            exchangeBuffer.close();
        } else {
            buffer.flip();
            pooledBuffer[i] = exchangeBuffer;
        }
    }


    private void prepParsedBody(HttpServerExchange exchange, String contentType, String requestBody) throws IOException {
        if (contentType.startsWith("application/json")) {
            if (config.isCacheRequestBody()) {
                exchange.putAttachment(REQUEST_BODY_STRING, requestBody);
            }
            // attach the parsed request body into exchange if the body parser is enabled
            attachJsonBody(exchange, requestBody);
        } else {
            exchange.putAttachment(REQUEST_BODY, requestBody);
        }
    }

    /**
     * Check to make sure we should actually run the body parse on the current request.
     *
     * @param exchange - http exchange
     * @return - return true if we should run the body parser.
     */
    private boolean shouldParseBody(final HttpServerExchange exchange) {
        HttpString method = exchange.getRequestMethod();
        boolean hasBody = method.equals(Methods.POST) || method.equals(Methods.PUT) || method.equals(Methods.PATCH);
        return !config.isSkipProxyBodyHandler() &&
                hasBody &&
                exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE) != null &&
                exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE).startsWith("application/json");
    }

    /**
     * Method used to parse the body into a Map or a List and attach it into exchange
     *
     * @param exchange exchange to be attached
     * @param string   raw request body
     * @throws IOException - throws exception if can't read string value.
     */
    private void attachJsonBody(final HttpServerExchange exchange, String string) throws IOException {
        Object body;
        if (string != null) {
            string = string.trim();
            if (string.startsWith("{")) {
                body = Config.getInstance().getMapper().readValue(string, new TypeReference<Map<String, Object>>() {
                });
            } else if (string.startsWith("[")) {
                body = Config.getInstance().getMapper().readValue(string, new TypeReference<List<Object>>() {
                });
            } else {
                // error here. The content type in head doesn't match the body.
                setExchangeStatus(exchange, CONTENT_TYPE_MISMATCH, "application/json");
                return;
            }
            exchange.putAttachment(REQUEST_BODY, body);
        }
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(final HttpHandler next) {
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
        ModuleRegistry.registerModule(ProxyBodyHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(BodyConfig.CONFIG_NAME), null);
    }

}
