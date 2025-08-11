package com.networknt.metrics;


import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import com.networknt.server.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;
import io.dropwizard.metrics.broadcom.APMEPAgentSender;

import io.dropwizard.metrics.Clock;
import io.dropwizard.metrics.MetricFilter;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class APMMetricsHandler extends AbstractMetricsHandler {
    static final Logger logger = LoggerFactory.getLogger(APMMetricsHandler.class);
    public static ServerConfig serverConfig;

    // this is the indicator to start the reporter and construct the common tags. It cannot be static as
    // the currentPort and currentAddress are not available during the handler initialization.
    private final AtomicBoolean firstTime = new AtomicBoolean(true);
    private volatile HttpHandler next;

    public APMMetricsHandler() {
        config = MetricsConfig.load();
        if (config.getIssuerRegex() != null) {
            pattern = Pattern.compile(config.getIssuerRegex());
        }
        serverConfig = ServerConfig.getInstance();
        ModuleRegistry.registerModule(MetricsConfig.CONFIG_NAME, APMMetricsHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(MetricsConfig.CONFIG_NAME), null);
        logger.debug("APMMetricsHandler is constructed!");
    }

    @Override
    protected void createMetricsReporter(TimeSeriesDbSender sender) {
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
                this.createMetricsReporter(sender);
            } catch (MalformedURLException e) {
                logger.error("apmmetrics has failed to initialize APMEPAgentSender", e);
            }
        }
        long startTime = Clock.defaultClock().getTick();
        final var exchangeCompletionListener = new MetricsExchangeCompletionListener(commonTags, startTime);
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

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(MetricsConfig.CONFIG_NAME, APMMetricsHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(MetricsConfig.CONFIG_NAME), null);
    }

    @Override
    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(MetricsConfig.CONFIG_NAME, APMMetricsHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(MetricsConfig.CONFIG_NAME), null);
        logger.info("APMMetricsHandler is reloaded.");
    }
}
