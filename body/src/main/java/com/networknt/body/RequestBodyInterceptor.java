package com.networknt.body;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.RequestInterceptor;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.networknt.body.BodyHandler.REQUEST_BODY_STRING;
import static com.networknt.body.BodyHandler.REQUEST_BODY;

/**
 * Note: With RequestInterceptorInjectionHandler implemented, this handler is changed from a
 * pure middleware handler to RequestInterceptor implementation.
 *
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
    static final Logger logger = LoggerFactory.getLogger(RequestBodyInterceptor.class);
    static final String CONTENT_TYPE_MISMATCH = "ERR10015";
    static final String PAYLOAD_TOO_LARGE = "ERR10068";
    static final String GENERIC_EXCEPTION = "ERR10014";

    public BodyConfig config;

    private volatile HttpHandler next;

    public RequestBodyInterceptor() {
        if (logger.isInfoEnabled()) logger.info("ProxyBodyHandler is loaded.");
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
        if (this.shouldAttachBody(exchange)) {
            var existing = (PooledByteBuffer[])exchange.getAttachment(AttachmentConstants.BUFFERED_REQUEST_DATA_KEY);
            StringBuilder completeBody = new StringBuilder();
            for(PooledByteBuffer buffer : existing) {
                if(buffer != null) {
                    completeBody.append(StandardCharsets.UTF_8.decode(buffer.getBuffer().duplicate()).toString());
                } else {
                    break;
                }
            }
            boolean attached = false;
            String contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
            if(contentType.startsWith("application/json")) {
                attached = this.attachJsonBody(exchange, completeBody.toString());
            } else if(contentType.startsWith("text")) { // include text/plain and text/xml etc.
                if (config.isCacheRequestBody()) {
                    exchange.putAttachment(AttachmentConstants.REQUEST_BODY_STRING, completeBody.toString());
                    attached = true;
                }
            }
            if(!attached) {
                if(logger.isInfoEnabled()) logger.info("Failed to attached the request body to the exchange");
            }
        }
        Handler.next(exchange, next);
    }

    /**
     * Check to make sure we should actually run the body parse on the current request.
     *
     * @param exchange - http exchange
     * @return - return true if we should run the body parser.
     */
    private boolean shouldAttachBody(final HttpServerExchange exchange) {
        HttpString method = exchange.getRequestMethod();
        String requestPath = exchange.getRequestPath();
        boolean hasBody = method.equals(Methods.POST) || method.equals(Methods.PUT) || method.equals(Methods.PATCH);
        boolean isPathConfigured = config.getAppliedPathPrefixes() == null ? true : config.getAppliedPathPrefixes().stream().anyMatch(s -> requestPath.startsWith(s));
        return hasBody &&
                isPathConfigured &&
                exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE) != null;
    }

    /**
     * Method used to parse the body into a Map or a List and attach it into exchange
     *
     * @param exchange exchange to be attached
     * @param string   raw request body
     */
    private boolean attachJsonBody(final HttpServerExchange exchange, String string) {
        Object body;
        string = string.trim();
        if (string.startsWith("{")) {
            try {
                body = Config.getInstance().getMapper().readValue(string, new TypeReference<Map<String, Object>>() {
                });
            } catch (JsonProcessingException e) {
                if(exchange.getConnection().getBufferSize() <= string.length()) {
                    setExchangeStatus(exchange, PAYLOAD_TOO_LARGE, "application/json");
                } else {
                    setExchangeStatus(exchange, CONTENT_TYPE_MISMATCH, "application/json");
                }
                return false;
            }
        } else if (string.startsWith("[")) {
            try {
                body = Config.getInstance().getMapper().readValue(string, new TypeReference<List<Object>>() {
                });
            } catch (JsonProcessingException e) {
                if(exchange.getConnection().getBufferSize() <= string.length()) {
                    setExchangeStatus(exchange, PAYLOAD_TOO_LARGE, "application/json");
                } else {
                    setExchangeStatus(exchange, CONTENT_TYPE_MISMATCH, "application/json");
                }
                return false;
            }
        } else {
            // error here. The content type in head doesn't match the body.
            setExchangeStatus(exchange, CONTENT_TYPE_MISMATCH, "application/json");
            return false;
        }
        if (config.isCacheRequestBody()) {
            exchange.putAttachment(AttachmentConstants.REQUEST_BODY_STRING, string);
        }
        exchange.putAttachment(AttachmentConstants.REQUEST_BODY, body);
        return true;
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
        ModuleRegistry.registerModule(RequestBodyInterceptor.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void reload() {
        config.reload();
    }

    @Override
    public boolean isRequiredContent() {
        return true;
    }
}
