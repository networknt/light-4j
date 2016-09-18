package com.networknt.validator;

import com.networknt.config.Config;
import com.networknt.utility.Constants;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a swagger validator handler that validate request and response based on the spec. In
 * production only request validator should be turned on and response validator should only be
 * used during development.
 *
 * Created by steve on 17/09/16.
 */
public class ValidatorHandler implements HttpHandler {
    public static final String CONFIG_NAME = "validator";
    public static final String ENABLE_VALIDATOR = "enableValidator";
    static final String ENABLE_REQUEST_VALIDATOR = "enableRequestValidator";
    static final String ENABLE_RESPONSE_VALIDATOR = "enableResponseValidator";

    static final Logger logger = LoggerFactory.getLogger(ValidatorHandler.class);

    private final HttpHandler next;

    public ValidatorHandler(final HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        ValidatorConfig config = (ValidatorConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ValidatorConfig.class);

        if(config.enableResponseValidator) {
            exchange.addExchangeCompleteListener(new ExchangeCompletionListener() {
                @Override
                public void exchangeEvent(final HttpServerExchange exchange, final NextListener nextListener) {



                    nextListener.proceed();
                }
            });
        }

        next.handleRequest(exchange);
    }
}
