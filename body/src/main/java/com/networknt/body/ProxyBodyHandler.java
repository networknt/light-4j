package com.networknt.body;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.ConduitWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.ConduitFactory;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.conduits.AbstractStreamSourceConduit;
import org.xnio.conduits.StreamSourceConduit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
     * based on the first character "{" or "[".
     *
     * @param exchange HttpServerExchange
     * @throws Exception Exception
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        String contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
        if (this.shouldParseBody(exchange)) {

            exchange.addRequestWrapper(new ConduitWrapper<>() {
                @Override
                public StreamSourceConduit wrap(ConduitFactory<StreamSourceConduit> conduitFactory, HttpServerExchange httpServerExchange) {
                    StreamSourceConduit source = conduitFactory.create();
                    return new AbstractStreamSourceConduit<>(source) {
                        final ByteArrayOutputStream bufferOut = new ByteArrayOutputStream(config.getMaxBuffers());

                        @Override
                        public int read(ByteBuffer dataBuffer) throws IOException {
                            int x = super.read(dataBuffer);
                            if (x >= 0) {
                                ByteBuffer dupDataBuffer = dataBuffer.duplicate();
                                dupDataBuffer.flip();
                                byte[] data = new byte[x];
                                dupDataBuffer.get(data);
                                bufferOut.write(data);
                            } else {
                                String requestBody = bufferOut.toString(StandardCharsets.UTF_8);
                                logger.debug("request body = " + requestBody);

                                // parse the body to map or list if content type is application/json
                                try {
                                    prepParsedBody(httpServerExchange, contentType, requestBody);
                                } catch (IOException e) {
                                    logger.error("IOException: ", e);
                                    setExchangeStatus(httpServerExchange, CONTENT_TYPE_MISMATCH, contentType);
                                }
                            }
                            return x;
                        }
                    };
                }
            });
        }

        Handler.next(exchange, next);
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

    private void prepParsedBody(HttpServerExchange exchange, String contentType, String requestBody) throws IOException {
        if (config.isCacheRequestBody()) {
            exchange.putAttachment(REQUEST_BODY_STRING, requestBody);
        }
        // attach the parsed request body into exchange if the body parser is enabled
        attachJsonBody(exchange, requestBody);
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
