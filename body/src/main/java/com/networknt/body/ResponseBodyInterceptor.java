package com.networknt.body;

import com.networknt.common.ContentType;
import com.networknt.config.Config;
import com.networknt.handler.BuffersUtils;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.ResponseInterceptor;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.server.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class ResponseBodyInterceptor implements ResponseInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseBodyInterceptor.class);
    private String configName = BodyConfig.CONFIG_NAME;
    private volatile HttpHandler next;

    public ResponseBodyInterceptor() {
        BodyConfig.load(configName);
        if (LOG.isInfoEnabled())
            LOG.info("ResponseBodyInterceptor is loaded");
    }

    public ResponseBodyInterceptor(String configName) {
        this.configName = configName;
        BodyConfig.load(configName);
        if (LOG.isInfoEnabled())
            LOG.info("ResponseBodyInterceptor is loaded with {}.", configName);
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
        return BodyConfig.load(configName).isEnabled();
    }

    @Override
    public boolean isRequiredContent() {
        return true;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        if (LOG.isDebugEnabled())
            LOG.debug("ResponseBodyInterceptor.handleRequest starts.");

        BodyConfig config = BodyConfig.load(configName);

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
                boolean attached = this.handleBody(exchange, completeBody, contentType, config);

                if (!attached && LOG.isErrorEnabled())
                    LOG.error("Failed to attach the request body to the exchange!");
            }
        }

        if (LOG.isDebugEnabled())
            LOG.debug("ResponseBodyInterceptor.handleRequest ends.");
    }

    private boolean handleBody(final HttpServerExchange ex, String body, String contentType, BodyConfig config) {
        if (this.isJsonData(contentType))
            return this.attachJsonBody(ex, body, config);

        else if (this.isXmlData(contentType))
            return this.attachXmlBody(ex, body, config);

        else if (this.isFormData(contentType))
            return this.attachFormDataBody(ex, body, config);

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
    private boolean attachJsonBody(final HttpServerExchange ex, String str, BodyConfig config) {
        str = str.trim();
        if(str.isEmpty()) {
            // if an empty string is passed in, we should not try to parse it. Just cache it.
            this.cacheResponseBody(ex, str, config);
            return true;
        }
        if (str.charAt(0) == JSON_MAP_OBJECT_STARTING_CHAR) {
            this.cacheResponseBody(ex, str, config);
            return this.parseJsonMapObject(ex, AttachmentConstants.REQUEST_BODY, str);

        } else if (str.charAt(0) == JSON_ARRAY_OBJECT_STARTING_CHAR) {
            this.cacheResponseBody(ex, str, config);
            return this.parseJsonArrayObject(ex, AttachmentConstants.REQUEST_BODY, str);
        }

        setExchangeStatus(ex, CONTENT_TYPE_MISMATCH, ContentType.APPLICATION_JSON.value());
        return false;
    }

    public boolean attachXmlBody(HttpServerExchange ex, String str, BodyConfig config) {
        this.cacheResponseBody(ex, str, config);
        return true;
    }

    public boolean attachFormDataBody(HttpServerExchange ex, String str, BodyConfig config) {
        this.cacheResponseBody(ex, str, config);
        return true;
    }

    private void cacheResponseBody(HttpServerExchange exchange, String s, BodyConfig config) {
        if (config.isCacheRequestBody())
            exchange.putAttachment(AttachmentConstants.RESPONSE_BODY_STRING, s);
    }

}
