package com.networknt.body;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger("FormHandler.class");
    static final String CONTENT_TYPE_MISMATCH = "ERR10015";
    static final AttachmentKey<FormData> REQUEST_BODY = FormDataParser.FORM_DATA;
    static final String CONFIG_NAME = "form";
    public static final BodyConfig config = (BodyConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, BodyConfig.class);
    private final FormParserFactory formParserFactory;
    private volatile HttpHandler next;

    public FormHandler() {
        this.formParserFactory = FormParserFactory.builder().build();
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
        ModuleRegistry.registerModule(FormHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        // parse the body to form-data if the content type is application/
        String contentType = httpServerExchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
        if (contentType != null) {
            contentType = contentType.split(";")[0];
            if (contentType.startsWith("multipart/form-data")) {contentType = "application/x-www-form-urlencoded";}
            FormDataParser parser = formParserFactory.createParser(httpServerExchange);
            if (parser != null) {
                if (httpServerExchange.isInIoThread()) {
                    httpServerExchange.dispatch(this);
                    return;
                }
                httpServerExchange.startBlocking();
                try {
                    FormData data = parser.parseBlocking();
                    httpServerExchange.putAttachment(REQUEST_BODY, data);
                } catch (Exception e) {
                    logger.error("IOException: ", e);
                    setExchangeStatus(httpServerExchange, CONTENT_TYPE_MISMATCH, contentType);
                    return;
                }
            }
        }
        Handler.next(httpServerExchange, next);
    }
}

