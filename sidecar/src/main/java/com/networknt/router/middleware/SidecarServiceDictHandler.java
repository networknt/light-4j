package com.networknt.router.middleware;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.router.SidecarConfig;
import com.networknt.url.HttpURL;
import com.networknt.utility.Constants;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the same handler as the ServiceDictHandler used in the client proxy and gateway;
 * however, it has logic to detect the traffic is incoming or outgoing. This handler is used for
 * outgoing traffic only for service to service invocation.
 * It depends on the gateway.yml to detect the traffic based on either request header or protocol.
 *
 * @author Gavin Chen
 */
public class SidecarServiceDictHandler extends ServiceDictHandler {
    private static final Logger logger = LoggerFactory.getLogger(SidecarServiceDictHandler.class);

    private static SidecarConfig sidecarConfig;

    public SidecarServiceDictHandler() {
        logger.info("SidecarServiceDictHandler is constructed");
        config = ServiceDictConfig.load();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        SidecarConfig sidecarConfig = SidecarConfig.load();
        if(logger.isDebugEnabled()) logger.debug("SidecarServiceDictHandler.handleRequest starts.");
        if (Constants.HEADER.equalsIgnoreCase(sidecarConfig.getEgressIngressIndicator())) {
            if(logger.isTraceEnabled()) logger.trace("Outgoing request with header indicator");
            serviceDict(exchange);
        } else if (Constants.PROTOCOL.equalsIgnoreCase(sidecarConfig.getEgressIngressIndicator()) && HttpURL.PROTOCOL_HTTP.equalsIgnoreCase(exchange.getRequestScheme())){
            if(logger.isTraceEnabled()) logger.trace("Outgoing request with protocol indicator and http protocol");
            serviceDict(exchange);
        } else {
            if(logger.isTraceEnabled()) logger.trace("Incoming request");
        }
        if(logger.isDebugEnabled()) logger.debug("SidecarServiceDictHandler.handleRequest ends.");
        Handler.next(exchange, next);
    }
}
