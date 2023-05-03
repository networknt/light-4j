package com.networknt.metrics;


import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.networknt.config.JsonMapper;
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
        if(config.getIssuerRegex() != null) {
            pattern = Pattern.compile(config.getIssuerRegex());
        }
        serverConfig = (ServerConfig) Config.getInstance().getJsonObjectConfig(ServerConfig.CONFIG_NAME, ServerConfig.class);
        ModuleRegistry.registerModule(MetricsConfig.class.getName(), config.getMappedConfig(), null);
        if(logger.isDebugEnabled()) logger.debug("APMMetricsHandler is constructed!");
    }

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (firstTime) {
            commonTags.put("api", Server.getServerConfig().getServiceId());
            commonTags.put("addr", Server.currentAddress);
            commonTags.put("port", "" + (Server.getServerConfig().isEnableHttps() ? Server.currentHttpsPort : Server.currentHttpPort));
            InetAddress inetAddress = Util.getInetAddress();
            commonTags.put("host", inetAddress == null ? "unknown" : inetAddress.getHostName()); // will be container id if in docker.
            if (logger.isDebugEnabled()) {
                logger.debug(commonTags.toString());
            }
            
            try {
                TimeSeriesDbSender sender =
                        new APMEPAgentSender(config.getServerProtocol(), config.getServerHost(), config.getServerPort(), config.getServerPath(), serverConfig.getServiceId(),  config.getProductName());
                APMAgentReporter reporter = APMAgentReporter
                        .forRegistry(registry)
                        .convertRatesTo(TimeUnit.SECONDS)
                        .convertDurationsTo(TimeUnit.MILLISECONDS)
                        .filter(MetricFilter.ALL)
                        .build(sender);
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
            if(logger.isTraceEnabled()) logger.trace("auditInfo = " + auditInfo);
            if (auditInfo != null) {
                Map<String, String> tags = new HashMap<>();
                tags.put("endpoint", (String) auditInfo.get(Constants.ENDPOINT_STRING));
                String clientId = auditInfo.get(Constants.CLIENT_ID_STRING) != null ? (String) auditInfo.get(Constants.CLIENT_ID_STRING) : "unknown";
                if(logger.isTraceEnabled()) logger.trace("clientId = " + clientId);
                tags.put("clientId", clientId);
                // scope client id will only be available if two token is used. For example, authorization code flow.
                if (config.isSendScopeClientId()) {
                    tags.put("scopeClientId", auditInfo.get(Constants.SCOPE_CLIENT_ID_STRING) != null ? (String) auditInfo.get(Constants.SCOPE_CLIENT_ID_STRING) : "unknown");
                }
                // caller id is the calling serviceId that is passed from the caller. It is not always available but some organizations enforce it.
                if (config.isSendCallerId()) {
                    tags.put("callerId", auditInfo.get(Constants.CALLER_ID_STRING) != null ? (String) auditInfo.get(Constants.CALLER_ID_STRING) : "unknown");
                }
                if (config.isSendIssuer()) {
                    String issuer = (String) auditInfo.get(Constants.ISSUER_CLAIMS);
                    if (issuer != null) {
                        // we need to send issuer as a tag. Do we need to apply regex to extract only a part of the issuer?
                        if(config.getIssuerRegex() != null) {
                            Matcher matcher = pattern.matcher(issuer);
                            if (matcher.find()) {
                                String iss = matcher.group(1);
                                if(logger.isTraceEnabled()) logger.trace("Extracted issuer {} from Original issuer {] is sent.", iss, issuer);
                                tags.put("issuer", iss != null ? iss : "unknown");
                            }
                        } else {
                            if(logger.isTraceEnabled()) logger.trace("Original issuer {} is sent.", issuer);
                            tags.put("issuer", issuer);
                        }
                    }
                }

                MetricName metricName = new MetricName("response_time");
                metricName = metricName.tagged(commonTags);
                metricName = metricName.tagged(tags);
                long time = Clock.defaultClock().getTick() - startTime;
                registry.getOrAdd(metricName, MetricRegistry.MetricBuilder.TIMERS).update(time, TimeUnit.NANOSECONDS);
                if(logger.isTraceEnabled()) logger.trace("metricName = " + metricName  + " commonTags = " + JsonMapper.toJson(commonTags) + " tags = " + JsonMapper.toJson(tags));
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
