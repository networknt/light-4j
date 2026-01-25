package com.networknt.handler;

import com.networknt.handler.conduit.ContentStreamSinkConduit;
import com.networknt.handler.conduit.ModifiableContentSinkConduit;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.ConduitFactory;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.conduits.StreamSinkConduit;

import java.util.Arrays;

/**
 * This is a middleware handle that is responsible for injecting the SinkConduit in order to update
 * the response content for interceptor handlers to update the response before returning to client.
 */
public class ResponseInterceptorInjectionHandler implements MiddlewareHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseInterceptorInjectionHandler.class);

    public static final AttachmentKey<HeaderMap> ORIGINAL_ACCEPT_ENCODINGS_KEY = AttachmentKey.create(HeaderMap.class);

    private ResponseInterceptor[] interceptors = null;
    private volatile HttpHandler next;
    private String configName = ResponseInjectionConfig.CONFIG_NAME;

    public ResponseInterceptorInjectionHandler() throws Exception {
        interceptors = SingletonServiceFactory.getBeans(ResponseInterceptor.class);
        LOG.info("SinkConduitInjectorHandler is loaded!");
    }

    public ResponseInterceptorInjectionHandler(String configName) throws Exception {
        this.configName = configName;
        interceptors = SingletonServiceFactory.getBeans(ResponseInterceptor.class);
        LOG.info("SinkConduitInjectorHandler is loaded with {}!", configName);
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
        return ResponseInjectionConfig.load(configName).isEnabled();
    }

    @Override
    public void register() {
    }


    /**
     * if the ModifiableContentSinkConduit is set, set the Accept-Encoding
     * header to identity this is required to avoid response interceptors
     * dealing with compressed data
     *
     * @param exchange the exchange
     */
    private void forceIdentityEncodingForInterceptors(HttpServerExchange exchange) {

        if (this.interceptorsRequireContent()) {
            var before = new HeaderMap();

            if (exchange.getRequestHeaders().contains(Headers.ACCEPT_ENCODING))
                exchange.getRequestHeaders().get(Headers.ACCEPT_ENCODING).forEach((value) -> {
                    before.add(Headers.ACCEPT_ENCODING, value);
                });

            exchange.putAttachment(ORIGINAL_ACCEPT_ENCODINGS_KEY, before);

            if (LOG.isDebugEnabled())
                LOG.debug("{} setting encoding to identity because request involves response interceptors.", before);

            exchange.getRequestHeaders().put(Headers.ACCEPT_ENCODING, "identity");
        }
    }

    /**
     * @param exchange HttpServerExchange
     * @throws Exception if any exception happens
     */
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        ResponseInjectionConfig config = ResponseInjectionConfig.load(configName);
        // of the response buffering it if any interceptor resolvers the request
        // and requires the content from the backend
        exchange.addResponseWrapper((ConduitFactory<StreamSinkConduit> factory, HttpServerExchange currentExchange) -> {
            if (this.requiresContentSinkConduit(exchange, config)) {
                var mcsc = new ModifiableContentSinkConduit(factory.create(), currentExchange);

                if (LOG.isTraceEnabled())
                    LOG.trace("created a ModifiableContentSinkConduit instance " + mcsc);

                return mcsc;

            } else return new ContentStreamSinkConduit(factory.create(), currentExchange);
        });

        Handler.next(exchange, next);
    }

    private boolean isCompressed(HttpServerExchange exchange) {

        // check if the request has a header accept encoding with gzip and deflate.
        var contentEncodings = exchange.getResponseHeaders().get(Headers.CONTENT_ENCODING_STRING);

        if (contentEncodings != null)
            for (var values : contentEncodings)
                if (this.hasCompressionFormat(values))
                    return true;

        return false;
    }

    private boolean requiresContentSinkConduit(final HttpServerExchange exchange, ResponseInjectionConfig config) {
        if(logger.isTraceEnabled()) {
            logger.trace("requiresContentSinkConduit: requiredContent {}, pathPrefix {} and isNotCompressed {}", this.interceptorsRequireContent()
                    , isAppliedBodyInjectionPathPrefix(exchange.getRequestPath(), config)
                    , !isCompressed(exchange));
        }
        return this.interceptorsRequireContent()
                && isAppliedBodyInjectionPathPrefix(exchange.getRequestPath(), config)
                && !isCompressed(exchange);
    }

    private boolean isAppliedBodyInjectionPathPrefix(String requestPath, ResponseInjectionConfig config) {
        return config.getAppliedBodyInjectionPathPrefixes() != null
                && config.getAppliedBodyInjectionPathPrefixes().stream().anyMatch(requestPath::startsWith);
    }

    private boolean hasCompressionFormat(String values) {
        return Arrays.stream(values.split(",")).anyMatch(
                (v) -> Headers.GZIP.toString().equals(v)
                        || Headers.COMPRESS.toString().equals(v)
                        || Headers.DEFLATE.toString().equals(v)
        );
    }

    private boolean interceptorsRequireContent() {
        return interceptors != null && Arrays.stream(interceptors).anyMatch(ri -> ri.isRequiredContent());
    }
}
