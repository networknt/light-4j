package com.networknt.metrics;


import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.networknt.server.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import io.dropwizard.metrics.broadcom.APMEPAgentSender;

import io.dropwizard.metrics.Clock;
import io.dropwizard.metrics.MetricFilter;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class APMMetricsHandler extends AbstractMetricsHandler {
    static final Logger logger = LoggerFactory.getLogger(APMMetricsHandler.class);

    // this is the indicator to start the reporter and construct the common tags. It cannot be static as
    // the currentPort and currentAddress are not available during the handler initialization.
    private final AtomicBoolean firstTime = new AtomicBoolean(true);
    private volatile HttpHandler next;

    public APMMetricsHandler() {
        logger.debug("APMMetricsHandler is constructed!");
    }

    @Override
    protected void createMetricsReporter(TimeSeriesDbSender sender, MetricsConfig config) {
        APMAgentReporter reporter = APMAgentReporter
                .forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(sender);
        reporter.start(config.getReportInMinutes(), TimeUnit.MINUTES);
        logger.info("apmmetrics is enabled and reporter is started");
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        MetricsConfig config = MetricsConfig.load();
        ServerConfig serverConfig = ServerConfig.load();
        if (this.firstTime.compareAndSet(true, false)) {
            logger.debug("First request received, initializing APMMetricsHandler.");
            AbstractMetricsHandler.addCommonTags(commonTags);
            try {
                TimeSeriesDbSender sender = new APMEPAgentSender(
                        config.getServerProtocol(),
                        config.getServerHost(),
                        config.getServerPort(),
                        config.getServerPath(),
                        serverConfig.getServiceId(),
                        config.getProductName()
                );
                this.createMetricsReporter(sender, config);
            } catch (MalformedURLException e) {
                logger.error("apmmetrics has failed to initialize APMEPAgentSender", e);
            }
        }
        long startTime = Clock.defaultClock().getTick();
        final var exchangeCompletionListener = new MetricsExchangeCompletionListener(commonTags, startTime, config);
        exchange.addExchangeCompleteListener(exchangeCompletionListener);
        Handler.next(exchange, next);

    }

    @Override
    public HttpHandler getNext() {
        return this.next;
    }

    @Override
    public MiddlewareHandler setNext(final HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

}
