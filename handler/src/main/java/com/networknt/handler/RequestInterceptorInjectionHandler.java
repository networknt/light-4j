package com.networknt.handler;

import com.networknt.httpstring.AttachmentConstants;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.Handlers;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.Connectors;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RequestTooBigException;
import io.undertow.server.protocol.http.HttpContinue;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSourceChannel;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * This is the middleware used in the request/response chain to inject the implementations of RequestInterceptorHandler interface
 * to modify the request metadata and body. You can have multiple interceptors per application; however, we do provide a generic
 * implementation in request-transform module to transform the request based on the rule engine rules.
 *
 * @author Kalev Gonvick
 */
public class RequestInterceptorInjectionHandler implements MiddlewareHandler {
    /** Generic exception error code */
    public static final String GENERIC_EXCEPTION = "ERR10014";
    /** Payload too large error code */
    public static final String PAYLOAD_TOO_LARGE = "ERR10068";
    private static final Logger LOG = LoggerFactory.getLogger(RequestInterceptorInjectionHandler.class);
    private volatile HttpHandler next;
    private String configName = RequestInjectionConfig.CONFIG_NAME;
    private RequestInterceptor[] interceptors = null;

    /**
     * Default constructor for RequestInterceptorInjectionHandler.
     */
    public RequestInterceptorInjectionHandler() {
        RequestInjectionConfig.load(configName);
        interceptors = SingletonServiceFactory.getBeans(RequestInterceptor.class);
        LOG.info("RequestInterceptorInjectionHandler is loaded!");
    }

    /**
     * Constructor for RequestInterceptorInjectionHandler with a custom config name.
     * @param configName name of the configuration file
     */
    public RequestInterceptorInjectionHandler(String configName) {
        this.configName = configName;
        RequestInjectionConfig.load(configName);
        interceptors = SingletonServiceFactory.getBeans(RequestInterceptor.class);
        LOG.info("RequestInterceptorInjectionHandler is loaded with {}!", configName);
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
        return RequestInjectionConfig.load(configName).isEnabled();
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

        var method = httpServerExchange.getRequestMethod().toString();

        HttpHandler nextHandler = Handler.getNext(httpServerExchange, this.next);

        RequestInjectionConfig config = RequestInjectionConfig.load(configName);
        if(logger.isTraceEnabled())
            logger.trace("injectionContentRequired = {} appliedBodyInjectionPathPrefix = {} method = {} requestComplete = {} requiresContinueResponse = {}", this.injectorContentRequired(), this.isAppliedBodyInjectionPathPrefix(httpServerExchange.getRequestPath(), config), method, httpServerExchange.isRequestComplete(), HttpContinue.requiresContinueResponse(httpServerExchange.getRequestHeaders()));

        if (this.shouldReadBody(httpServerExchange, config)) {
            if(logger.isTraceEnabled()) logger.trace("Trying to read body");
            if (config.getMaxBodyBytes() > 0
                    && httpServerExchange.getRequestContentLength() > config.getMaxBodyBytes()) {
                setExchangeStatus(httpServerExchange, PAYLOAD_TOO_LARGE);
                return;
            }

            RequestBodyReadState readState = null;

            try {
                readState = new RequestBodyReadState(httpServerExchange, config, nextHandler);
                ReadOutcome outcome = readState.readAvailable();
                if (outcome != ReadOutcome.COMPLETE)
                    return;

                this.saveBufferAndResetUndertowConnector(httpServerExchange, readState.bufferedData);
            } catch (RequestTooBigException e) {
                logger.error(e.getMessage(), e);
                if (readState != null) readState.closeBuffers();
                setExchangeStatus(httpServerExchange, PAYLOAD_TOO_LARGE);
                return;
            } catch (Exception | Error e) {
                logger.error(e.getMessage(), e);
                if (readState != null) readState.closeBuffers();
                setExchangeStatus(httpServerExchange, GENERIC_EXCEPTION, e.getMessage());
                return;
            }
        } else {
            if(logger.isTraceEnabled()) logger.trace("No need to read body");
            // no need to inject the content for the body. just call the interceptors here.
            this.invokeInterceptors(httpServerExchange);
        }

        // If there are any error and one of the interceptor response the error to the caller, we don't need to call the next.
        if(logger.isTraceEnabled())
            logger.trace("Exchange response started status = {}", httpServerExchange.isResponseStarted());

        if(!httpServerExchange.isResponseStarted())
            Handler.next(httpServerExchange, nextHandler);

    }

    private boolean shouldReadBody(final HttpServerExchange ex, RequestInjectionConfig config) {
        var headers = ex.getRequestHeaders();
        var requestMethod = ex.getRequestMethod().toString();
        var requestPath = ex.getRequestPath();

        return this.injectorContentRequired()
                && this.isAppliedBodyInjectionPathPrefix(requestPath, config)
                && this.hasContent(requestMethod)
                && !ex.isRequestComplete()
                && !HttpContinue.requiresContinueResponse(headers);
    }

    private boolean hasContent(String method) {
        return method.equalsIgnoreCase("post") || method.equalsIgnoreCase("put") || method.equalsIgnoreCase("patch");
    }

    /**
     * Check if any of the interceptors require content.
     *
     * @return - true if required.
     */
    private boolean injectorContentRequired() {
        return this.interceptors != null && this.interceptors.length > 0 &&
                Arrays.stream(this.interceptors).anyMatch(RequestInterceptor::isRequiredContent);
    }

    private enum ReadOutcome {
        COMPLETE,
        PENDING,
        FAILED
    }

    /**
     * One request-body read state shared by the initial non-blocking read and any
     * resumed channel callbacks. Positive maxBodyBytes mode must observe EOF and
     * is never allowed to treat a full final buffer as a complete request.
     */
    private final class RequestBodyReadState implements ChannelListener<StreamSourceChannel> {
        private final HttpServerExchange exchange;
        private final RequestInjectionConfig config;
        private final HttpHandler continuation;
        private final StreamSourceChannel channel;
        private final PooledByteBuffer[] bufferedData;
        private final boolean exactLimit;
        private PooledByteBuffer buffer;
        private int readBuffers;
        private long bytesRead;

        private RequestBodyReadState(HttpServerExchange exchange, RequestInjectionConfig config, HttpHandler continuation) {
            this.exchange = exchange;
            this.config = config;
            this.continuation = continuation;
            this.channel = exchange.getRequestChannel();
            this.bufferedData = new PooledByteBuffer[config.getMaxBuffers()];
            this.exactLimit = config.getMaxBodyBytes() > 0;
            this.buffer = exchange.getConnection().getByteBufferPool().allocate();
        }

        private ReadOutcome readAvailable() throws Exception {
            for (; ; ) {
                if (readBuffers == bufferedData.length) {
                    if (!exactLimit)
                        return ReadOutcome.COMPLETE;
                    return probeForEndOfStream();
                }

                ByteBuffer byteBuffer = buffer.getBuffer();
                int originalLimit = byteBuffer.limit();
                if (exactLimit) {
                    long bytesThroughFirstExcess = (long) config.getMaxBodyBytes() - bytesRead + 1;
                    if (bytesThroughFirstExcess < byteBuffer.remaining())
                        byteBuffer.limit(byteBuffer.position() + (int) bytesThroughFirstExcess);
                }

                int read;
                try {
                    read = channel.read(byteBuffer);
                } finally {
                    byteBuffer.limit(originalLimit);
                }

                if (read == -1) {
                    handleEndOfStream(byteBuffer, bufferedData, readBuffers, buffer);
                    buffer = null;
                    return ReadOutcome.COMPLETE;
                }
                if (read == 0)
                    return awaitMoreData();

                bytesRead += read;
                if (exactLimit && bytesRead > config.getMaxBodyBytes())
                    return rejectTooLarge();

                if (!byteBuffer.hasRemaining()) {
                    byteBuffer.flip();
                    bufferedData[readBuffers++] = buffer;
                    buffer = null;
                    if (readBuffers < bufferedData.length)
                        buffer = exchange.getConnection().getByteBufferPool().allocate();
                }
            }
        }

        private ReadOutcome probeForEndOfStream() throws Exception {
            ByteBuffer probe = ByteBuffer.allocate(1);
            int read = channel.read(probe);
            if (read == -1)
                return ReadOutcome.COMPLETE;
            if (read == 0)
                return awaitMoreData();
            return rejectTooLarge();
        }

        private ReadOutcome awaitMoreData() {
            channel.getReadSetter().set(this);
            channel.resumeReads();
            return ReadOutcome.PENDING;
        }

        private ReadOutcome rejectTooLarge() {
            stopReading();
            closeBuffers();
            setExchangeStatus(exchange, PAYLOAD_TOO_LARGE);
            return ReadOutcome.FAILED;
        }

        private void stopReading() {
            channel.getReadSetter().set(null);
            channel.suspendReads();
        }

        private void closeBuffers() {
            safeCloseBuffers(bufferedData, buffer);
            buffer = null;
        }

        @Override
        public void handleEvent(StreamSourceChannel ignored) {
            try {
                ReadOutcome outcome = readAvailable();
                if (outcome == ReadOutcome.PENDING)
                    return;

                stopReading();
                if (outcome == ReadOutcome.COMPLETE) {
                    saveBufferAndResetUndertowConnector(exchange, bufferedData);
                    if (!exchange.isResponseStarted())
                        Connectors.executeRootHandler(ex -> Handler.next(ex, continuation), exchange);
                }
            } catch (Throwable e) {
                LOG.error(e.getMessage(), e);
                stopReading();
                closeBuffers();
                setExchangeStatus(exchange, GENERIC_EXCEPTION, e.getMessage());
            }
        }
    }

    /**
     * Close our buffers when issue occurs
     *
     * @param buffers the array of pooled byte buffers to close.
     * @param buf     the current pooled byte buffer to close.
     */
    private static void safeCloseBuffers(final PooledByteBuffer[] buffers, PooledByteBuffer buf) {
        for (var b : buffers)
            IoUtils.safeClose(b);

        if (buf != null && buf.isOpen())
            IoUtils.safeClose(buf);
    }

    /**
     * Save the total buffer as an attachment. Update content length just in case
     *
     * @param ex           current httpServerExchange.
     * @param bufferedData total buffered data array.
     */
    private void saveBufferAndResetUndertowConnector(final HttpServerExchange ex, final PooledByteBuffer[] bufferedData) {
        if(logger.isTraceEnabled()) logger.trace("saveBufferAndResetUndertowConnector is called.");
        ex.putAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY, bufferedData);
        this.updateContentLength(ex, bufferedData);
        Connectors.ungetRequestBytes(ex, bufferedData);
        Connectors.resetRequestChannel(ex);
        this.invokeInterceptors(ex);
    }

    private void updateContentLength(final HttpServerExchange ex, final PooledByteBuffer[] bufferedData) {
        if (ex.getRequestHeaders().getFirst(Headers.CONTENT_LENGTH) != null) {
            if(logger.isTraceEnabled()) logger.trace("original content length in request headers = {}", ex.getRequestHeaders().getFirst(Headers.CONTENT_LENGTH));
            long length = 0;

            for (var dest : bufferedData)
                if (dest != null)
                    length += dest.getBuffer().limit();
            if(logger.isTraceEnabled()) logger.trace("update content length in request headers = {}", length);
            ex.getRequestHeaders().put(Headers.CONTENT_LENGTH, length);
        }
    }

    private static void handleEndOfStream(ByteBuffer b, PooledByteBuffer[] bufferedData, int readBuffers, PooledByteBuffer buffer) {

        if (b.position() == 0)
            buffer.close();

        else {
            b.flip();
            bufferedData[readBuffers] = buffer;
        }
    }

    /**
     * Invokes the interceptors that use request body.
     *
     * @param httpServerExchange - current server exchange.
     */
    private void invokeInterceptors(HttpServerExchange httpServerExchange) {

        if (this.interceptors != null && this.interceptors.length > 0) {

            for (var ri : this.interceptors) {

                try {
                    ri.handleRequest(httpServerExchange);

                    if (httpServerExchange.isResponseStarted())
                        return;

                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                    return;
                }
            }
        }
    }

    private boolean isAppliedBodyInjectionPathPrefix(String requestPath, RequestInjectionConfig config) {
        return config.getAppliedBodyInjectionPathPrefixes() != null && config.getAppliedBodyInjectionPathPrefixes().stream().anyMatch(requestPath::startsWith);
    }
}
