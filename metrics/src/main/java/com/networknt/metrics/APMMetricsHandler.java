package com.networknt.metrics;


import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.networknt.server.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.server.Server;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.Util;
import io.dropwizard.metrics.broadcom.APMEPAgentSender;

import io.dropwizard.metrics.Clock;
import io.dropwizard.metrics.MetricFilter;
import io.dropwizard.metrics.MetricName;
import io.dropwizard.metrics.MetricRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class APMMetricsHandler extends AbstractMetricsHandler {
    static final Logger logger = LoggerFactory.getLogger(APMMetricsHandler.class);
    public static ServerConfig serverConfig;

    // this is the indicator to start the reporter and construct the common tags. It cannot be static as
    // the currentPort and currentAddress are not available during the handler initialization.
    private boolean firstTime = true;
    private volatile HttpHandler next;

    public APMMetricsHandler() {
        config = MetricsConfig.load();
        serverConfig = (ServerConfig) Config.getInstance().getJsonObjectConfig(ServerConfig.CONFIG_NAME, ServerConfig.class);
        ModuleRegistry.registerModule(MetricsConfig.class.getName(), config.getMappedConfig(), null);
        if(logger.isDebugEnabled()) logger.debug("APMMetricsHandler is constructed!");
    }

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (firstTime) {
            commonTags.put("api", Server.getServerConfig().getServiceId());
            commonTags.put("env", Server.getServerConfig().getEnvironment());
            commonTags.put("addr", Server.currentAddress);
            commonTags.put("port", "" + (Server.getServerConfig().isEnableHttps() ? Server.currentHttpsPort : Server.currentHttpPort));
            InetAddress inetAddress = Util.getInetAddress();
            commonTags.put("host", inetAddress == null ? "unknown" : inetAddress.getHostName()); // will be container id if in docker.
            if (logger.isDebugEnabled()) {
                logger.debug(commonTags.toString());
            }
            
            try {
                TimeSeriesDbSender influxDb =
                        new APMEPAgentSender(config.getServerProtocol(), config.getServerHost(), config.getServerPort(), config.getServerPath(), serverConfig.getServiceId());
                APMInfluxDbReporter reporter = APMInfluxDbReporter
                        .forRegistry(registry)
                        .convertRatesTo(TimeUnit.SECONDS)
                        .convertDurationsTo(TimeUnit.MILLISECONDS)
                        .filter(MetricFilter.ALL)
                        .build(influxDb);
                reporter.start(config.getReportInMinutes(), TimeUnit.MINUTES);

                logger.info("apmmetrics is enabled and reporter is started");
            } catch (MalformedURLException e) {
                logger.error("apmmetrics has failed to initialize APMEPAgentSender", e);
            }            

            // reset the flag so that this block will only be called once.
            firstTime = false;
        }
        
        long startTime = Clock.defaultClock().getTick();
        exchange.addExchangeCompleteListener((exchange1, nextListener) -> {
            Map<String, Object> auditInfo = exchange1.getAttachment(AttachmentConstants.AUDIT_INFO);
            if (auditInfo != null) {
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
            nextListener.proceed();
        });
        
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
        ModuleRegistry.registerModule(APMMetricsHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }

    @Override
    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(APMMetricsHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }
}
