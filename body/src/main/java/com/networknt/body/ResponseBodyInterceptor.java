package com.networknt.body;

import com.networknt.handler.BuffersUtils;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.ResponseInterceptor;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.httpstring.ContentType;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import static com.networknt.body.BodyHandler.REQUEST_BODY_STRING;

public class ResponseBodyInterceptor implements ResponseInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseBodyInterceptor.class);
    private final BodyConfig config;
    private volatile HttpHandler next;

    public ResponseBodyInterceptor() {
        if (LOG.isInfoEnabled())
            LOG.info("ResponseBodyInterceptor is loaded");

        this.config = BodyConfig.load();
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
        ModuleRegistry.registerModule(ResponseBodyInterceptor.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void reload() {
        config.reload();
    }

    @Override
    public boolean isRequiredContent() {
        return true;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        if (LOG.isDebugEnabled())
            LOG.debug("ResponseBodyInterceptor.handleRequest starts.");

        if (this.shouldAttachBody(exchange.getResponseHeaders())) {

            var existing = this.getBuffer(exchange);

            if (existing != null) {

                if (LOG.isTraceEnabled())
                    LOG.trace("Attach response body requirement is met and the byte buffer pool exists.");

                var completeBody = BuffersUtils.toString(existing, StandardCharsets.UTF_8);
                var contentType = exchange.getResponseHeaders().getFirst(Headers.CONTENT_TYPE);

                if (LOG.isTraceEnabled()) {
                    if(config.isLogFullResponseBody())
                        LOG.trace("contentType = " + contentType + " response body = " + completeBody);
                    else
                        LOG.trace("contentType = " + contentType + " response body = " + (completeBody.length() > 16384 ? completeBody.substring(0, 16384) : completeBody));
                }
                boolean attached = this.handleBody(exchange, completeBody, contentType);

                if (!attached && LOG.isErrorEnabled())
                    LOG.error("Failed to attach the request body to the exchange!");
            }
        }

        if (LOG.isDebugEnabled())
            LOG.debug("ResponseBodyInterceptor.handleRequest ends.");
    }

    private boolean handleBody(final HttpServerExchange ex, String body, String contentType) {
        if (this.isJsonData(contentType))
            return this.attachJsonBody(ex, body);

        else if (this.isXmlData(contentType))
            return this.attachXmlBody(ex, body);

        else if (this.isFormData(contentType))
            return this.attachFormDataBody(ex, body);

        else
            return false;
    }

    /**
     * Method used to parse the body into a Map or a List and attach it into exchange.
     *
     * @param ex - current exchange
     * @param str - byte buffer body as a string
     * @return - true if successful
     */
    private boolean attachJsonBody(final HttpServerExchange ex, String str) {
        str = str.trim();

        if (str.charAt(0) == JSON_MAP_OBJECT_STARTING_CHAR) {
            this.cacheResponseBody(ex, str);
            return this.parseJsonMapObject(ex, AttachmentConstants.REQUEST_BODY, str);

        } else if (str.charAt(0) == JSON_ARRAY_OBJECT_STARTING_CHAR) {
            this.cacheResponseBody(ex, str);
            return this.parseJsonArrayObject(ex, AttachmentConstants.REQUEST_BODY, str);
        }

        setExchangeStatus(ex, CONTENT_TYPE_MISMATCH, ContentType.APPLICATION_JSON.value());
        return false;
    }

    public boolean attachXmlBody(HttpServerExchange ex, String str) {
        this.cacheResponseBody(ex, str);
        return true;
    }

    public boolean attachFormDataBody(HttpServerExchange ex, String str) {
        this.cacheResponseBody(ex, str);
        return true;
    }

    private void cacheResponseBody(HttpServerExchange exchange, String s) {
        if (this.config.isCacheRequestBody())
            exchange.putAttachment(AttachmentConstants.RESPONSE_BODY_STRING, s);
    }

}
