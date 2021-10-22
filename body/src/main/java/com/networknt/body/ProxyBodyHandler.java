package com.networknt.body;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.UndertowLogger;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.Connectors;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSourceChannel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.networknt.body.BodyHandler.REQUEST_BODY_STRING;
import static com.networknt.body.BodyHandler.REQUEST_BODY;

/**
 * This is the Body Parser handler used by the light-proxy and http-sidecar to not only parse
 * the body into an attachment in the exchange but also keep the stream to be forwarded to the
 * backend API. If the normal BodyHandler is used, once the stream is consumed, it is gone and
 * cannot be transfer to the backend with socket to socket transfer.
 *
 * The body validation will only support smaller size JSON request, so we check the content type
 * first and check the request size before applying the logic. For other type of data, we just
 * call the next handler to bypass the body parser so that other type of request will be forwarded
 * to the backend directly.
 *
 * If you are using this handler in a chain, the last handler must be the proxy. If the stream is
 * not forward to the backend API, the event listener won't be triggered and the buffer won't be
 * closed immediately. For one request, this is fine, but with multiple requests in a row, an
 * error will be thrown.
 *
 * @author Steve Hu
 */
public class ProxyBodyHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(ProxyBodyHandler.class);
    static final String CONTENT_TYPE_MISMATCH = "ERR10015";

    public static final String CONFIG_NAME = "body";

    public static final BodyConfig config = (BodyConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, BodyConfig.class);

    private volatile HttpHandler next;

    public ProxyBodyHandler() {
        if (logger.isInfoEnabled()) logger.info("ProxyBodyHandler is loaded.");
    }

    /**
     * Check the header starts with application/json and parse it into map or list
     * based on the first character "{" or "[". Otherwise, check the header starts
     * with application/x-www-form-urlencoded or multipart/form-data and parse it
     * into formdata
     *
     * @param exchange HttpServerExchange
     * @throws Exception Exception
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        String contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
        long requestContentLength = exchange.getRequestContentLength();
        HttpString method = exchange.getRequestMethod();
        boolean hasBody = method.equals(Methods.POST) || method.equals(Methods.PUT) || method.equals(Methods.PATCH);
        // bypass the body parser if body doesn't exist.
        if(hasBody && contentType != null && contentType.startsWith("application/json") && requestContentLength < config.getMaxBuffers()) {
            final StreamSourceChannel channel = exchange.getRequestChannel();
            int readBuffers = 0;
            final PooledByteBuffer[] bufferedData = new PooledByteBuffer[config.getMaxBuffers()];
            PooledByteBuffer buffer = exchange.getConnection().getByteBufferPool().allocate();
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
                        final PooledByteBuffer finalBuffer = buffer;
                        final int finalReadBuffers = readBuffers;
                        channel.getReadSetter().set(new ChannelListener<StreamSourceChannel>() {

                            PooledByteBuffer buffer = finalBuffer;
                            int readBuffers = finalReadBuffers;

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
                                            Connectors.ungetRequestBytes(exchange, bufferedData);
                                            Connectors.resetRequestChannel(exchange);
                                            channel.getReadSetter().set(null);
                                            channel.suspendReads();
                                            Connectors.executeRootHandler(next, exchange);
                                            return;
                                        } else if (r == 0) {
                                            return;
                                        } else if (!b.hasRemaining()) {
                                            b.flip();
                                            bufferedData[readBuffers++] = buffer;
                                            if (readBuffers == config.getMaxBuffers()) {
                                                Connectors.ungetRequestBytes(exchange, bufferedData);
                                                Connectors.resetRequestChannel(exchange);
                                                channel.getReadSetter().set(null);
                                                channel.suspendReads();
                                                Connectors.executeRootHandler(next, exchange);
                                                return;
                                            }
                                            buffer = exchange.getConnection().getByteBufferPool().allocate();
                                        }
                                    } while (true);
                                } catch (Throwable t) {
                                    if (t instanceof IOException) {
                                        UndertowLogger.REQUEST_IO_LOGGER.ioException((IOException) t);
                                    } else {
                                        UndertowLogger.REQUEST_IO_LOGGER.handleUnexpectedFailure(t);
                                    }
                                    for (int i = 0; i < bufferedData.length; ++i) {
                                        IoUtils.safeClose(bufferedData[i]);
                                    }
                                    if (buffer != null && buffer.isOpen()) {
                                        IoUtils.safeClose(buffer);
                                    }
                                    exchange.endExchange();
                                }
                            }
                        });
                        channel.resumeReads();
                        return;
                    } else if (!b.hasRemaining()) {
                        b.flip();
                        bufferedData[readBuffers++] = buffer;
                        if (readBuffers == config.getMaxBuffers()) {
                            break;
                        }
                        buffer = exchange.getConnection().getByteBufferPool().allocate();
                    }
                } while (true);
                Connectors.ungetRequestBytes(exchange, bufferedData);
                Connectors.resetRequestChannel(exchange);
            } catch (Exception | Error e) {
                for (int i = 0; i < bufferedData.length; ++i) {
                    IoUtils.safeClose(bufferedData[i]);
                }
                if (buffer != null && buffer.isOpen()) {
                    IoUtils.safeClose(buffer);
                }
                throw e;
            }
            ByteBuffer bb = buffer.getBuffer().duplicate();
            String requestBody = StandardCharsets.UTF_8.decode(bb).toString();
            logger.debug("request body = " + requestBody);

            // parse the body to map or list if content type is application/json
            if (contentType != null) {
                try {
                    if (contentType.startsWith("application/json")) {
                        if (config.isCacheRequestBody()) {
                            exchange.putAttachment(REQUEST_BODY_STRING, requestBody);
                        }
                        // attach the parsed request body into exchange if the body parser is enabled
                        attachJsonBody(exchange, requestBody);
                    } else {
                        exchange.putAttachment(REQUEST_BODY, requestBody);
                    }
                } catch (IOException e) {
                    logger.error("IOException: ", e);
                    setExchangeStatus(exchange, CONTENT_TYPE_MISMATCH, contentType);
                    return;
                }
            }
        }
        Handler.next(exchange, next);
    }

    /**
     * Method used to parse the body into FormData and attach it into exchange
     *
     * @param exchange exchange to be attached
     * @throws IOException
     */
    private void attachFormDataBody(final HttpServerExchange exchange) throws IOException {
        Object data;
        FormParserFactory formParserFactory = FormParserFactory.builder().build();
        FormDataParser parser = formParserFactory.createParser(exchange);
        if (parser != null) {
            FormData formData = parser.parseBlocking();
            data = BodyConverter.convert(formData);
            exchange.putAttachment(REQUEST_BODY, data);
        } else {
            InputStream inputStream = exchange.getInputStream();
            exchange.putAttachment(REQUEST_BODY, inputStream);
        }
    }

    /**
     * Method used to parse the body into a Map or a List and attach it into exchange
     *
     * @param exchange exchange to be attached
     * @param string   unparsed request body
     * @throws IOException
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
        ModuleRegistry.registerModule(ProxyBodyHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }

}
