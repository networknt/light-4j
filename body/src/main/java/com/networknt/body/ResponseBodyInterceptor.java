package com.networknt.body;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.handler.BuffersUtils;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.ResponseInterceptor;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.networknt.body.BodyHandler.REQUEST_BODY;
import static com.networknt.body.BodyHandler.REQUEST_BODY_STRING;

public class ResponseBodyInterceptor implements ResponseInterceptor {
    static final Logger logger = LoggerFactory.getLogger(ResponseBodyInterceptor.class);
    static final String CONTENT_TYPE_MISMATCH = "ERR10015";
    static final String PAYLOAD_TOO_LARGE = "ERR10068";

    public static int MAX_BUFFERS = 1024;
    private BodyConfig config;
    private volatile HttpHandler next;

    public ResponseBodyInterceptor() {
        if(logger.isInfoEnabled()) logger.info("ResponseBodyInterceptor is loaded");
        config = BodyConfig.load();
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
        if(shouldParseBody(exchange) && getBuffer(exchange) != null) {
            String s = BuffersUtils.toString(getBuffer(exchange), StandardCharsets.UTF_8);
            if(logger.isTraceEnabled()) logger.trace("original response body = " + s);
            // put the response body in the attachment for auditing and validation.
            boolean attached = this.attachJsonBody(exchange, s);
            if(!attached) {
                if(logger.isInfoEnabled()) logger.info("Failed to attached the response body to the exchange");
            }
        }
        // as an interceptor, we don't need to call the next handler in the chain.
        // Handler.next(exchange, next);
    }

    private boolean shouldParseBody(final HttpServerExchange exchange) {
        String requestPath = exchange.getRequestPath();
        boolean isPathConfigured = config.getAppliedPathPrefixes() == null ? true : config.getAppliedPathPrefixes().stream().anyMatch(s -> requestPath.startsWith(s));
        return isPathConfigured && isAttachContentType(exchange);
    }
    private boolean isAttachContentType(final HttpServerExchange exchange) {
        String contentType = exchange.getResponseHeaders().getFirst(Headers.CONTENT_TYPE);
        // do not attach if content type is null
        if(contentType == null) return false;
        return contentType.startsWith("application/json") || contentType.startsWith("text") || contentType.startsWith("application/xml");
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
        // handle the json body on the best effort.
        if (string.startsWith("{")) {
            try {
                body = Config.getInstance().getMapper().readValue(string, new TypeReference<Map<String, Object>>() {
                });
                exchange.putAttachment(AttachmentConstants.RESPONSE_BODY, body);
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
                exchange.putAttachment(AttachmentConstants.RESPONSE_BODY, body);
            } catch (JsonProcessingException e) {
                if(exchange.getConnection().getBufferSize() <= string.length()) {
                    setExchangeStatus(exchange, PAYLOAD_TOO_LARGE, "application/json");
                } else {
                    setExchangeStatus(exchange, CONTENT_TYPE_MISMATCH, "application/json");
                }
                return false;
            }
        }
        // cache other content type of body if it is enabled for auditing purpose.
        if (config.isCacheRequestBody()) {
            exchange.putAttachment(AttachmentConstants.RESPONSE_BODY_STRING, string);
        }
        return true;
    }

}
