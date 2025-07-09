package com.networknt.metrics;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;
import io.dropwizard.metrics.Clock;
import io.dropwizard.metrics.MetricFilter;
import io.dropwizard.metrics.influxdb.InfluxDbHttpSender;
import io.dropwizard.metrics.influxdb.InfluxDbReporter;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class MetricsHandler extends AbstractMetricsHandler {
    static final Logger logger = LoggerFactory.getLogger(MetricsHandler.class);

    // this is the indicator to start the reporter and construct the common tags. It cannot be static as
    // the currentPort and currentAddress are not available during the handler initialization.
    private final AtomicBoolean firstTime = new AtomicBoolean(true);
    private static final String MASK_KEY_SERVER_PASS = "serverPass";
    private volatile HttpHandler next;

    public MetricsHandler() {
        config = MetricsConfig.load();
        if (config.getIssuerRegex() != null) {
            pattern = Pattern.compile(config.getIssuerRegex());
        }
        ModuleRegistry.registerModule(MetricsConfig.CONFIG_NAME, MetricsHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(MetricsConfig.CONFIG_NAME), List.of(MASK_KEY_SERVER_PASS));
        logger.debug("MetricsHandler is constructed!");
    }

    @Override
    protected void createMetricsReporter(TimeSeriesDbSender sender) {
        try (InfluxDbReporter reporter = InfluxDbReporter
                .forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(sender)) {
            reporter.start(config.getReportInMinutes(), TimeUnit.MINUTES);
        }

        if (config.enableJVMMonitor) {
            createJVMMetricsReporter(sender);
        }

        logger.info("metrics is enabled and reporter is started");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        logger.debug("MetricsHandler.handleRequest starts.");
        if (firstTime.compareAndSet(true, false)) {
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
                this.createMetricsReporter(sender);
            } catch (Exception e) {
                // if there are any exception, chances are influxdb is not available.
                logger.error("metrics is failed to connect to the influxdb", e);
            }
        }
        long startTime = Clock.defaultClock().getTick();
        final var exchangeCompletionListener = new MetricsExchangeCompletionListener(commonTags, startTime);
        exchange.addExchangeCompleteListener(exchangeCompletionListener);

        if (logger.isDebugEnabled()) logger.debug("MetricsHandler.handleRequest ends.");
        Handler.next(exchange, next);
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(MetricsConfig.CONFIG_NAME, MetricsHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(MetricsConfig.CONFIG_NAME), List.of(MASK_KEY_SERVER_PASS));
    }

    @Override
    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(MetricsConfig.CONFIG_NAME, MetricsHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(MetricsConfig.CONFIG_NAME), List.of(MASK_KEY_SERVER_PASS));
        if (logger.isInfoEnabled()) logger.info("MetricsHandler is reloaded.");
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
