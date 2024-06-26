package com.networknt.handler;

import com.networknt.config.Config;
import com.networknt.handler.conduit.ContentStreamSinkConduit;
import com.networknt.handler.conduit.ModifiableContentSinkConduit;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.utility.ModuleRegistry;
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
    private static ResponseInjectionConfig config;

    public ResponseInterceptorInjectionHandler() throws Exception {
        config = ResponseInjectionConfig.load();
        interceptors = SingletonServiceFactory.getBeans(ResponseInterceptor.class);
        LOG.info("SinkConduitInjectorHandler is loaded!");
    }

    /**
     * This is a constructor for test cases only. Please don't use it.
     *
     * @param cfg limit config
     * @throws Exception thrown when config is wrong.
     */
    @Deprecated
    public ResponseInterceptorInjectionHandler(ResponseInjectionConfig cfg) throws Exception {
        config = cfg;
        LOG.info("SinkConduitInjectorHandler is loaded!");
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
        ModuleRegistry.registerModule(ResponseInjectionConfig.CONFIG_NAME, ResponseInterceptorInjectionHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(ResponseInjectionConfig.CONFIG_NAME), null);
    }

    @Override
    public void reload() {
        config.reload();
        if (LOG.isTraceEnabled())
            LOG.trace("response-injection.yml is reloaded");
        ModuleRegistry.registerModule(ResponseInjectionConfig.CONFIG_NAME, ResponseInterceptorInjectionHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(ResponseInjectionConfig.CONFIG_NAME), null);
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

        // of the response buffering it if any interceptor resolvers the request
        // and requires the content from the backend
        exchange.addResponseWrapper((ConduitFactory<StreamSinkConduit> factory, HttpServerExchange currentExchange) -> {
            if (this.requiresContentSinkConduit(exchange)) {
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

    private boolean requiresContentSinkConduit(final HttpServerExchange exchange) {
        return this.interceptorsRequireContent()
                && isAppliedBodyInjectionPathPrefix(exchange.getRequestPath())
                && !isCompressed(exchange);
    }

    private boolean isAppliedBodyInjectionPathPrefix(String requestPath) {
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
