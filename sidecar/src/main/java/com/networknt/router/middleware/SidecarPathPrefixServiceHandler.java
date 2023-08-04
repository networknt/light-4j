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
 * This is the same handler as the PathPrefixServiceHandler used in the client proxy and gateway;
 * however, it has logic to detect the traffic is incoming or outgoing. This handler is used for
 * outgoing traffic only for service to service invocation.
 * It depends on the gateway.yml to detect the traffic based on either request header or protocol.
 *
 * @author Steve Hu
 */
public class SidecarPathPrefixServiceHandler extends PathPrefixServiceHandler {
    private static final Logger logger = LoggerFactory.getLogger(SidecarPathPrefixServiceHandler.class);

    private static SidecarConfig sidecarConfig;

    public SidecarPathPrefixServiceHandler() {
        logger.info("SidecarPathPrefixServiceHandler is constructed");
        config = PathPrefixServiceConfig.load();
        sidecarConfig = (SidecarConfig) Config.getInstance().getJsonObjectConfig(SidecarConfig.CONFIG_NAME, SidecarConfig.class);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("SidecarPathPrefixServiceHandler.handleRequest starts.");
        if (Constants.HEADER.equalsIgnoreCase(sidecarConfig.getEgressIngressIndicator())) {
            if(logger.isTraceEnabled()) logger.trace("Outgoing request calls PathPrefixServiceHandler with header indicator");
            pathPrefixService(exchange);
        } else if (Constants.PROTOCOL.equalsIgnoreCase(sidecarConfig.getEgressIngressIndicator()) && HttpURL.PROTOCOL_HTTP.equalsIgnoreCase(exchange.getRequestScheme())){
            if(logger.isTraceEnabled()) logger.trace("Outgoing request calls PathPrefixServiceHandler with protocol indicator and http protocol");
            pathPrefixService(exchange);
        } else {
            // incoming request, let the proxy handler to handle it.
            if(logger.isDebugEnabled()) logger.debug("SidecarPathPrefixServiceHandler.handleRequest ends for incoming request.");
            Handler.next(exchange, next);
        }
    }
}
