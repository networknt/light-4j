package com.networknt.body;

import com.networknt.common.ContentType;
import com.networknt.config.Config;
import com.networknt.handler.BuffersUtils;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.RequestInterceptor;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Note: With RequestInterceptorInjectionHandler implemented, this handler is changed from a
 * pure middleware handler to RequestInterceptor implementation.
 * <p>
 * This is the Body Parser interceptor used by the light-4j, light-proxy and http-sidecar to
 * not only parse the body into an attachment in the exchange but also to keep the stream to
 * be forwarded to the subsequent middleware handlers or backend API. If the normal BodyHandler is used, once the stream is consumed, it is gone and
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
public class RequestBodyInterceptor implements RequestInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(RequestBodyInterceptor.class);

    public BodyConfig config;

    private volatile HttpHandler next;

    public RequestBodyInterceptor() {

        if (LOG.isInfoEnabled())
            LOG.info("RequestBodyInterceptor is loaded.");

        config = BodyConfig.load();
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
        if (LOG.isDebugEnabled())
            LOG.debug("RequestBodyInterceptor.handleRequest starts.");

        if (this.shouldAttachBody(exchange.getRequestHeaders())) {

            var existing = (PooledByteBuffer[]) exchange.getAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY);

            if(LOG.isTraceEnabled())
                LOG.trace("request body exists in exchange attachment = {}", existing != null);

            if (existing != null) {

                if (LOG.isTraceEnabled())
                    LOG.trace("Attach request body requirement is met and the byte buffer pool exists.");

                var completeBody = BuffersUtils.toString(existing, StandardCharsets.UTF_8);
                var contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);

                if (LOG.isTraceEnabled()) {
                    // this config flag should only be enabled on non-production environment for troubleshooting purpose.
                    if(config.isLogFullRequestBody())
                        LOG.trace("contentType = " + contentType + " request body = " + completeBody);
                    else
                        LOG.trace("contentType = " + contentType + " request body = " + (completeBody.length() > 16384 ? completeBody.substring(0, 16384) : completeBody));
                }
                boolean attached = this.handleBody(exchange, completeBody, contentType);

                if (!attached && LOG.isErrorEnabled())
                    LOG.error("Failed to attach the request body to the exchange!");

                else if (LOG.isTraceEnabled())
                    LOG.trace("Request body was attached to exchange");

            } else if (LOG.isTraceEnabled())
                LOG.trace("Request body interceptor is skipped due to the request path is not in request-injection.appliedBodyInjectionPathPrefixes configuration");
        }

        if (LOG.isDebugEnabled())
            LOG.debug("RequestBodyInterceptor.handleRequest ends.");

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
    public boolean attachJsonBody(final HttpServerExchange ex, String str) {
        str = str.trim();

        if (str.charAt(0) == JSON_MAP_OBJECT_STARTING_CHAR) {
            this.cacheRequestBody(ex, str);
            return this.parseJsonMapObject(ex, AttachmentConstants.REQUEST_BODY, str);

        } else if (str.charAt(0) == JSON_ARRAY_OBJECT_STARTING_CHAR) {
            this.cacheRequestBody(ex, str);
            return this.parseJsonArrayObject(ex, AttachmentConstants.REQUEST_BODY, str);
        }

        setExchangeStatus(ex, CONTENT_TYPE_MISMATCH, ContentType.APPLICATION_JSON.value());
        return false;
    }

    public boolean attachXmlBody(HttpServerExchange exchange, String s) {
        // TODO
        this.cacheRequestBody(exchange, s);
        return true;
    }

    /**
     * Method used to parse the body into FormData and attach it into exchange
     *
     * @param exchange exchange to be attached
     * @param s        the string of the  request body
     * @return boolean to indicate if attached.
     */
    public boolean attachFormDataBody(final HttpServerExchange exchange, String s) {
        // TODO
        this.cacheRequestBody(exchange, s);
        return true;
    }

    private void cacheRequestBody(HttpServerExchange exchange, String s) {
        if (this.config.isCacheRequestBody())
            exchange.putAttachment(AttachmentConstants.REQUEST_BODY_STRING, s);
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
        ModuleRegistry.registerModule(BodyConfig.CONFIG_NAME, RequestBodyInterceptor.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(BodyConfig.CONFIG_NAME), null);
    }

    @Override
    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(BodyConfig.CONFIG_NAME, RequestBodyInterceptor.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(BodyConfig.CONFIG_NAME), null);
        if (LOG.isInfoEnabled())
            LOG.info("RequestBodyInterceptor is reloaded.");
    }

    @Override
    public boolean isRequiredContent() {
        return true;
    }
}
