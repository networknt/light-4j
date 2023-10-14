package com.networknt.router.middleware;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.router.SidecarConfig;
import com.networknt.url.HttpURL;
import com.networknt.utility.Constants;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SidecarTokenHandler extends TokenHandler{
    private static final Logger logger = LoggerFactory.getLogger(SidecarTokenHandler.class);

    public static SidecarConfig sidecarConfig;

    public SidecarTokenHandler() {
        super();
        sidecarConfig = SidecarConfig.load();
        if(logger.isDebugEnabled()) logger.debug("SidecarTokenHandler is constructed");
    }
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(logger.isTraceEnabled()) logger.trace("SidecarTokenHandler.handleRequest starts with indicator {}.", sidecarConfig.getEgressIngressIndicator());
        if (Constants.HEADER.equalsIgnoreCase(sidecarConfig.getEgressIngressIndicator())) {
            HeaderValues serviceIdHeader = exchange.getRequestHeaders().get(HttpStringConstants.SERVICE_ID);
            String serviceId = serviceIdHeader != null ? serviceIdHeader.peekFirst() : null;
            String serviceUrl = exchange.getRequestHeaders().getFirst(HttpStringConstants.SERVICE_URL);
            if(logger.isTraceEnabled()) logger.trace("SidecarTokenHandler.handleRequest serviceId {} and serviceUrl {}.", serviceId, serviceUrl);
            if (serviceId != null || serviceUrl!=null) {
                if(logger.isTraceEnabled()) logger.trace("SidecarTokenHandler.handleRequest ends with calling TokenHandler");
                super.handleRequest(exchange);
            } else {
                if(logger.isTraceEnabled()) logger.trace("SidecarTokenHandler.handleRequest ends with skipping TokenHandler");
                Handler.next(exchange, next);
            }
        } else if (Constants.PROTOCOL.equalsIgnoreCase(sidecarConfig.getEgressIngressIndicator()) && HttpURL.PROTOCOL_HTTP.equalsIgnoreCase(exchange.getRequestScheme())){
            if(logger.isTraceEnabled()) logger.trace("SidecarTokenHandler.handleRequest ends with calling TokenHandler");
            super.handleRequest(exchange);
        } else {
            if(logger.isTraceEnabled()) logger.trace("SidecarTokenHandler.handleRequest ends with skipping TokenHandler");
            Handler.next(exchange, next);
        }
    }

}
