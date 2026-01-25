package com.networknt.metrics;

import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import io.dropwizard.metrics.Clock;
import io.dropwizard.metrics.MetricFilter;
import io.dropwizard.metrics.influxdb.InfluxDbHttpSender;
import io.dropwizard.metrics.influxdb.InfluxDbReporter;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MetricsHandler extends AbstractMetricsHandler {
    static final Logger logger = LoggerFactory.getLogger(MetricsHandler.class);

    // this is the indicator to start the reporter and construct the common tags. It cannot be static as
    // the currentPort and currentAddress are not available during the handler initialization.
    private final AtomicBoolean firstTime = new AtomicBoolean(true);
    private static final String MASK_KEY_SERVER_PASS = "serverPass";
    private volatile HttpHandler next;

    public MetricsHandler() {
        logger.debug("MetricsHandler is constructed!");
    }

    @Override
    protected void createMetricsReporter(TimeSeriesDbSender sender, MetricsConfig config) {
        InfluxDbReporter reporter = InfluxDbReporter
                .forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(sender);
        reporter.start(config.getReportInMinutes(), TimeUnit.MINUTES);
        if (config.isEnableJVMMonitor()) {
            createJVMMetricsReporter(sender, config);
        }

        logger.info("metrics is enabled and reporter is started");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        MetricsConfig config = MetricsConfig.load();
        if (firstTime.compareAndSet(true, false)) {
            logger.debug("First request received, initializing MetricsHandler.");
            AbstractMetricsHandler.addCommonTags(commonTags);
            try {
                TimeSeriesDbSender sender = new InfluxDbHttpSender(
                        config.getServerProtocol(),
                        config.getServerHost(),
                        config.getServerPort(),
                        config.getServerName(),
                        config.getServerUser(),
                        config.getServerPass()
                );
                this.createMetricsReporter(sender, config);
            } catch (Exception e) {
                // if there are any exception, chances are influxdb is not available.
                logger.error("metrics is failed to connect to the influxdb", e);
            }
        }
        long startTime = Clock.defaultClock().getTick();
        final var exchangeCompletionListener = new MetricsExchangeCompletionListener(commonTags, startTime, config);
        exchange.addExchangeCompleteListener(exchangeCompletionListener);
        Handler.next(exchange, next);
    }

    @Override
    public void register() {
    }

    @Override
    public void reload() {
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
