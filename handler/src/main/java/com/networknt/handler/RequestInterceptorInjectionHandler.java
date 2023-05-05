package com.networknt.handler;

import com.networknt.httpstring.AttachmentConstants;
import com.networknt.httpstring.ContentType;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
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

    private static final Logger LOG = LoggerFactory.getLogger(RequestInterceptorInjectionHandler.class);
    public static final int MAX_BUFFERS = 1024;

    private volatile HttpHandler next;
    private static RequestInjectionConfig config;
    private RequestInterceptor[] interceptors = null;

    public RequestInterceptorInjectionHandler() {
        config = RequestInjectionConfig.load();
        LOG.info("RequestInterceptorInjectionHandler is loaded!");
        interceptors = SingletonServiceFactory.getBeans(RequestInterceptor.class);
    }

    public RequestInterceptorInjectionHandler(RequestInjectionConfig cfg) {
        config = cfg;
        LOG.info("RequestInterceptorInjectionHandler is loaded!");
        interceptors = SingletonServiceFactory.getBeans(RequestInterceptor.class);
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
        return this.config.isEnabled();
    }

    @Override
    public void reload() {
        config.reload();

        if (LOG.isTraceEnabled())
            LOG.trace("request-injection.yml is reloaded");

        ModuleRegistry.registerModule(RequestInjectionConfig.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(RequestInjectionConfig.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

        // Make sure content is needed by request interceptors before grabbing the data. The process has a lot of overhead.
        this.next = Handler.getNext(httpServerExchange);

        if (this.shouldReadBody(httpServerExchange)) {
            final var channel = httpServerExchange.getRequestChannel();
            final var bufferedData = new PooledByteBuffer[MAX_BUFFERS];

            int readBuffers = 0;
            var buffer = httpServerExchange.getConnection().getByteBufferPool().allocate();

            try {
                for (; ; ) {
                    int r;
                    var b = buffer.getBuffer();
                    r = channel.read(b);

                    if (r == -1) {
                        handleEndOfStream(b, bufferedData, readBuffers, buffer);
                        break;

                    } else if (r == 0) {
                        this.setChannelRead(channel, buffer, readBuffers, bufferedData, httpServerExchange);
                        channel.resumeReads();
                        return;

                    } else if (!b.hasRemaining()) {
                        b.flip();
                        bufferedData[readBuffers++] = buffer;

                        if (readBuffers == MAX_BUFFERS)
                            break;

                        buffer = httpServerExchange.getConnection().getByteBufferPool().allocate();
                    }
                }

                this.saveBufferAndResetUndertowConnector(httpServerExchange, bufferedData);

            } catch (Exception | Error e) {
                safeCloseBuffers(bufferedData, buffer);
                httpServerExchange.endExchange();
            }

        } else this.invokeInterceptors(httpServerExchange);


        Handler.next(httpServerExchange, next);
    }

    private boolean shouldReadBody(final HttpServerExchange ex) {
        var headers = ex.getRequestHeaders();
        var contentType = ex.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
        var requestPath = ex.getRequestPath();

        return this.injectorContentRequired()
                && this.isAppliedBodyInjectionPathPrefix(requestPath)
                && this.hasContent(contentType)
                && !ex.isRequestComplete()
                && !HttpContinue.requiresContinueResponse(headers);
    }

    private boolean hasContent(String contentType) {
        if (contentType == null)
            return false;
        return contentType.startsWith(ContentType.TEXT_PLAIN_VALUE.value())
                || contentType.startsWith(ContentType.XML.value())
                || contentType.startsWith(ContentType.APPLICATION_XML_VALUE.value())
                || contentType.startsWith(ContentType.MULTIPART_FORM_DATA_VALUE.value())
                || contentType.startsWith(ContentType.APPLICATION_FORM_URLENCODED_VALUE.value())
                || contentType.startsWith(ContentType.APPLICATION_JSON.value());
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

    /**
     * Create a new read channel listener for the request channel. This is needed for 'chunked' requests larger than our server buffer set.
     *
     * @param c            - the request channel.
     * @param cPooledBuffer  - pool buffer.
     * @param cRead  - our read.
     * @param bufferedData       - total buffered data.
     * @param ex - current exchange.
     */
    private void setChannelRead(final StreamSourceChannel c, final PooledByteBuffer cPooledBuffer, final int cRead, final PooledByteBuffer[] bufferedData, final HttpServerExchange ex) {
        c.getReadSetter().set(new ChannelListener<StreamSourceChannel>() {
            PooledByteBuffer buffer = cPooledBuffer;
            int readBuffers = cRead;

            @Override
            public void handleEvent(StreamSourceChannel channel) {
                try {

                    for (; ; ) {
                        int r;
                        var b = buffer.getBuffer();
                        r = channel.read(b);

                        if (r == -1) {
                            handleEndOfStream(b, bufferedData, readBuffers, buffer);
                            suspendReads(ex, bufferedData, channel, next);
                            return;

                        } else if (r == 0)
                            return;

                        else if (!b.hasRemaining()) {
                            b.flip();
                            bufferedData[readBuffers++] = buffer;

                            if (readBuffers == MAX_BUFFERS) {
                                suspendReads(ex, bufferedData, channel, next);
                                return;
                            }

                            buffer = ex.getConnection().getByteBufferPool().allocate();
                        }
                    }
                } catch (Throwable e) {
                    safeCloseBuffers(bufferedData, buffer);
                    ex.endExchange();
                }
            }
        });
    }

    /**
     * Close our buffers when issue occurs
     *
     * @param buffers - the total buffer
     * @param buf    - the current data buffer
     */
    private static void safeCloseBuffers(final PooledByteBuffer[] buffers, PooledByteBuffer buf) {
        for (var b : buffers)
            IoUtils.safeClose(b);

        if (buf != null && buf.isOpen())
            IoUtils.safeClose(buf);
    }

    /**
     * Suspend our reads and remove the channel listener we created.
     *
     * @param ex - current exchange
     * @param bufferedData       - total buffered data.
     * @param c            - request channel
     * @param next               - next http handler
     */
    private void suspendReads(final HttpServerExchange ex, final PooledByteBuffer[] bufferedData, StreamSourceChannel c, HttpHandler next) {
        saveBufferAndResetUndertowConnector(ex, bufferedData);

        c.getReadSetter().set(null);
        c.suspendReads();

        if (LOG.isTraceEnabled())
            LOG.info("Next is: {}", next.getClass());

        Connectors.executeRootHandler(next, ex);
    }


    /**
     * Save the total buffer as an attachment. Update content length just in case
     *
     * @param ex - current exchange
     * @param bufferedData       - total buffered data
     */
    private void saveBufferAndResetUndertowConnector(final HttpServerExchange ex, final PooledByteBuffer[] bufferedData) {
        ex.putAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY, bufferedData);
        this.updateContentLength(ex, bufferedData);
        Connectors.ungetRequestBytes(ex, bufferedData);
        Connectors.resetRequestChannel(ex);
        this.invokeInterceptors(ex);
    }

    private void updateContentLength(final HttpServerExchange ex, final PooledByteBuffer[] bufferedData) {
        if (ex.getRequestHeaders().getFirst(Headers.CONTENT_LENGTH) != null) {
            long length = 0;

            for (var dest : bufferedData)
                if (dest != null)
                    length += dest.getBuffer().limit();

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

    private boolean isAppliedBodyInjectionPathPrefix(String requestPath) {
        return config.getAppliedBodyInjectionPathPrefixes() != null && config.getAppliedBodyInjectionPathPrefixes().stream().anyMatch(requestPath::startsWith);
    }
}
