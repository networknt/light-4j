package com.networknt.handler;

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
 *
 */
public class ResponseInterceptorInjectionHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(ResponseInterceptorInjectionHandler.class);

    public static final AttachmentKey<ModifiableContentSinkConduit> MCSC_KEY = AttachmentKey.create(ModifiableContentSinkConduit.class);

    public static final AttachmentKey<HeaderMap> ORIGINAL_ACCEPT_ENCODINGS_KEY = AttachmentKey.create(HeaderMap.class);

    private ResponseInterceptor[] interceptors = null;
    private volatile HttpHandler next;
    private ResponseInjectionConfig config;
    public ResponseInterceptorInjectionHandler() throws Exception{
        config = ResponseInjectionConfig.load();
        interceptors = SingletonServiceFactory.getBeans(ResponseInterceptor.class);
        logger.info("SinkConduitInjectorHandler is loaded!");
    }

    /**
     * This is a constructor for test cases only. Please don't use it.
     *
     * @param cfg limit config
     * @throws Exception thrown when config is wrong.
     *
     */
    @Deprecated
    public ResponseInterceptorInjectionHandler(ResponseInjectionConfig cfg) throws Exception {
        config = cfg;
        logger.info("SinkConduitInjectorHandler is loaded!");
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
        ModuleRegistry.registerModule(ResponseInjectionConfig.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void reload() {
        config.reload();
    }

    /**
     * if the ModificableContentSinkConduit is set, set the Accept-Encoding
     * header to identity this is required to avoid response interceptors
     * dealing with compressed data
     *
     * @param exchange
     */
    private void forceIdentityEncodingForInterceptors(HttpServerExchange exchange) {
        if (interceptors != null && Arrays.stream(interceptors).anyMatch(ri -> ri.isRequiredContent())) {
            var before = new HeaderMap();

            if (exchange.getRequestHeaders().contains(Headers.ACCEPT_ENCODING)) {
                exchange.getRequestHeaders().get(Headers.ACCEPT_ENCODING).forEach((value) -> {
                    before.add(Headers.ACCEPT_ENCODING, value);
                });
            }

            exchange.putAttachment(ORIGINAL_ACCEPT_ENCODINGS_KEY, before);

            logger.debug("{} setting encoding to identity because request involves response interceptors.", before);

            exchange.getRequestHeaders().put(Headers.ACCEPT_ENCODING, "identity");
        }
    }

    /**
     *
     * @param exchange
     * @throws Exception
     */
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        // of the response buffering it if any interceptor resolvers the request
        // and requires the content from the backend
        exchange.addResponseWrapper((ConduitFactory<StreamSinkConduit> factory, HttpServerExchange cexchange) -> {
            // restore MDC context
            // MDC context is put in the thread context
            // For proxied requests a thread switch in the request handling happens,
            // loosing the MDC context. TracingInstrumentationHandler adds it to the
            // exchange as an Attachment
            // var mdcCtx = ByteArrayProxyResponse.of(exchange).getMDCContext();
            // if (mdcCtx != null) {
            //     MDC.setContextMap(mdcCtx);
            // }

            if (interceptors != null && Arrays.stream(interceptors).anyMatch(ri -> ri.isRequiredContent())) {
                var mcsc = new ModifiableContentSinkConduit(factory.create(), cexchange);
                cexchange.putAttachment(MCSC_KEY, mcsc);
                return mcsc;
            } else {
                return new ContentStreamSinkConduit(factory.create(), cexchange);
            }
        });

        forceIdentityEncodingForInterceptors(exchange);
        // if any of the interceptors send response, don't call other middleware handlers in the chain.
        if(!exchange.isResponseStarted()) Handler.next(exchange, next);
    }

}
