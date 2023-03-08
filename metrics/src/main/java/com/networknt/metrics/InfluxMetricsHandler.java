package com.networknt.metrics;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.server.Server;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.Util;
import io.dropwizard.metrics.Clock;
import io.dropwizard.metrics.MetricFilter;
import io.dropwizard.metrics.MetricName;
import io.dropwizard.metrics.MetricRegistry;
import io.dropwizard.metrics.influxdb.InfluxDbHttpSender;
import io.dropwizard.metrics.influxdb.InfluxDbReporter;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class InfluxMetricsHandler extends MetricsHandler {
    static final Logger logger = LoggerFactory.getLogger(InfluxMetricsHandler.class);
    public static MetricsConfig config;

    // this is the indicator to start the reporter and construct the common tags. It cannot be static as
    // the currentPort and currentAddress are not available during the handler initialization.
    private boolean firstTime = true;
    Map<String, String> commonTags = new HashMap<>();
    static String MASK_KEY_SERVER_PASS= "serverPass";
    private volatile HttpHandler next;

    public InfluxMetricsHandler() {
        config = MetricsConfig.load();
        ModuleRegistry.registerModule(InfluxMetricsHandler.class.getName(), config.getMappedConfig(), List.of(MASK_KEY_SERVER_PASS));
        if(logger.isDebugEnabled()) logger.debug("InfluxMetricsHandler is constructed!");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("InfluxMetricsHandler.handleRequest starts.");
        if(firstTime) {
            commonTags.put("api", Server.getServerConfig().getServiceId());
            commonTags.put("env", Server.getServerConfig().getEnvironment());
            commonTags.put("addr", Server.currentAddress);
            commonTags.put("port", "" + (Server.getServerConfig().isEnableHttps() ? Server.currentHttpsPort : Server.currentHttpPort));
            InetAddress inetAddress = Util.getInetAddress();
            commonTags.put("host", inetAddress == null ? "unknown" : inetAddress.getHostName()); // will be container id if in docker.
            if(logger.isDebugEnabled()) {
                logger.debug(commonTags.toString());
            }
            try {
                TimeSeriesDbSender influxDb =
                        new InfluxDbHttpSender(config.getServerProtocol(), config.getServerHost(), config.getServerPort(),
                                config.getServerName(), config.getServerUser(), config.getServerPass());
                InfluxDbReporter reporter = InfluxDbReporter
                        .forRegistry(registry)
                        .convertRatesTo(TimeUnit.SECONDS)
                        .convertDurationsTo(TimeUnit.MILLISECONDS)
                        .filter(MetricFilter.ALL)
                        .build(influxDb);
                reporter.start(config.getReportInMinutes(), TimeUnit.MINUTES);
                if (config.enableJVMMonitor) {
                    createJVMMetricsReporter(influxDb);
                }

                logger.info("metrics is enabled and reporter is started");
            } catch (Exception e) {
                // if there are any exception, chances are influxdb is not available.
                logger.error("metrics is failed to connect to the influxdb", e);
            }
            // reset the flag so that this block will only be called once.
            firstTime = false;
        }

        long startTime = Clock.defaultClock().getTick();
        exchange.addExchangeCompleteListener((exchange1, nextListener) -> {
            try {
                Map<String, Object> auditInfo = exchange1.getAttachment(AttachmentConstants.AUDIT_INFO);
                if(auditInfo != null) {
                    Map<String, String> tags = new HashMap<>();
                    tags.put("endpoint", (String)auditInfo.get(Constants.ENDPOINT_STRING));
                    tags.put("clientId", auditInfo.get(Constants.CLIENT_ID_STRING) != null ? (String)auditInfo.get(Constants.CLIENT_ID_STRING) : "unknown");
                    tags.put("scopeClientId", auditInfo.get(Constants.SCOPE_CLIENT_ID_STRING) != null ? (String)auditInfo.get(Constants.SCOPE_CLIENT_ID_STRING) : "unknown");
                    tags.put("callerId", auditInfo.get(Constants.CALLER_ID_STRING) != null ? (String)auditInfo.get(Constants.CALLER_ID_STRING) : "unknown");
                    long time = Clock.defaultClock().getTick() - startTime;
                    MetricName metricName = new MetricName("response_time");
                    metricName = metricName.tagged(commonTags);
                    metricName = metricName.tagged(tags);
                    registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.TIMERS).update(time, TimeUnit.NANOSECONDS);
                    incCounterForStatusCode(exchange1.getStatusCode(), commonTags, tags);
                }
            } catch (Throwable e) {
                logger.error("ExchangeListener throwable",  e);
            } finally {
                nextListener.proceed();
            }
        });
        if(logger.isDebugEnabled()) logger.debug("InfluxMetricsHandler.handleRequest ends.");
        Handler.next(exchange, next);
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(InfluxMetricsHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), List.of(MASK_KEY_SERVER_PASS));
    }

    @Override
    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(InfluxMetricsHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), List.of(MASK_KEY_SERVER_PASS));
        if(logger.isTraceEnabled()) logger.trace("InfluxMetricsHandler is reloaded.");
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
