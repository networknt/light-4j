package com.networknt.handler.thread;

import com.networknt.httpstring.AttachmentConstants;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.concurrent.Executor;

public class LightThreadExecutor implements Executor {
    private static final Logger LOG = LoggerFactory.getLogger(LightThreadExecutor.class);
    private final HttpServerExchange exchange;

    public LightThreadExecutor(final HttpServerExchange exchange) {
        LOG.trace("Creating new LightThreadExecutor instance for exchange: {}", exchange);
        this.exchange = exchange;
    }

    /**
     * Updates thread MDC based on handler context map.
     * We do not want to clear context beforehand because undertow might set context via worker thread (or io thread) beforehand.
     */
    private void updateExchangeContext() {
        var context = this.exchange.getAttachment(AttachmentConstants.MDC_CONTEXT);
        MDC.clear();

        if (context != null) {
            for (var entry : context.entrySet()) {
                LOG.trace("Setting MDC key: {} value: {} for exchange: {}", entry.getKey(), entry.getValue(), this.exchange);
                MDC.put(entry.getKey(), entry.getValue());
            }

        } else LOG.error("No context found in exchange attachment: {}", this.exchange);
    }

    @Override
    public void execute(Runnable command) {
        LOG.trace("Executing command in LightThreadExecutor for exchange: {}", exchange);
        this.updateExchangeContext();
        command.run();
    }
}
