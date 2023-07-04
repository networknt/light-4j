package com.networknt.handler.thread;

import com.networknt.httpstring.AttachmentConstants;
import io.undertow.server.HttpServerExchange;
import org.slf4j.MDC;

import java.util.concurrent.Executor;

public class LightThreadExecutor implements Executor {

    private final HttpServerExchange exchange;

    public LightThreadExecutor(final HttpServerExchange exchange) {
        this.exchange = exchange;
    }

    /**
     * Updates thread MDC based on handler context map.
     * We do not want to clear context beforehand because undertow might set context via worker thread (or io thread) beforehand.
     */
    private void updateExchangeContext() {
        var context = this.exchange.getAttachment(AttachmentConstants.MDC_CONTEXT);

        if (context != null)
            for (var entry : context.entrySet())
                MDC.put(entry.getKey(), entry.getValue());
    }

    @Override
    public void execute(Runnable command) {
        this.updateExchangeContext();
        command.run();
    }
}
