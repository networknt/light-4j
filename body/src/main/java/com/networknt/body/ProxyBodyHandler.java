package com.networknt.body;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.StringUtils;
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
 * This is the Body Parser handler used by the light-proxy and light-mesh/http-sidecar to not only parse
 * the body into an attachment but also keep the stream to be forwarded to the backend API. If the normal
 * BodyHandler is used, once the stream is consumed, it is gone and cannot be transfer to the backend.
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
        HttpString method = exchange.getRequestMethod();
        boolean hasBody = method.equals(Methods.POST) || method.equals(Methods.PUT) || method.equals(Methods.PATCH);
        // bypass the body parser if body doesn't exist.
        if(hasBody && contentType != null) {
            try {
                if (contentType.startsWith("application/json")) {
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
                    if (config.isCacheRequestBody()) {
                        exchange.putAttachment(REQUEST_BODY_STRING, requestBody);
                    }
                    // attach the parsed request body into exchange if the body parser is enabled
                    attachJsonBody(exchange, requestBody);
                } else if (contentType.startsWith("text/plain")) {
                    InputStream inputStream = exchange.getInputStream();
                    String unparsedRequestBody = StringUtils.inputStreamToString(inputStream, StandardCharsets.UTF_8);
                    exchange.putAttachment(REQUEST_BODY, unparsedRequestBody);
                } else if (contentType.startsWith("multipart/form-data") || contentType.startsWith("application/x-www-form-urlencoded")) {
                    // attach the parsed request body into exchange if the body parser is enabled
                    if (exchange.isInIoThread()) {
                        exchange.dispatch(this);
                        return;
                    }
                    exchange.startBlocking();
                    attachFormDataBody(exchange);
                } else {
                    InputStream inputStream = exchange.getInputStream();
                    exchange.putAttachment(REQUEST_BODY, inputStream);
                }
            } catch (IOException e) {
                logger.error("IOException: ", e);
                setExchangeStatus(exchange, CONTENT_TYPE_MISMATCH, contentType);
                return;
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
