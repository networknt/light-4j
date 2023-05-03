package com.networknt.router.middleware;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.router.GatewayConfig;
import com.networknt.url.HttpURL;
import com.networknt.utility.Constants;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the same handler as the ServiceDictHandler used in the client proxy and gateway;
 * however, it has logic to detect the traffic is incoming or outgoing. This handler is used for
 * outgoing traffic only for service to service invocation.
 * It depends on the gateway.yml to detect the traffic based on either request header or protocol.
 *
 * @author Gavin Chen
 */
public class GatewayServiceDictHandler extends ServiceDictHandler {
    private static final Logger logger = LoggerFactory.getLogger(GatewayServiceDictHandler.class);

    private static GatewayConfig gatewayConfig;

    public GatewayServiceDictHandler() {
        logger.info("GatewayServiceDictHandler is constructed");
        config = ServiceDictConfig.load();
        gatewayConfig = (GatewayConfig)Config.getInstance().getJsonObjectConfig(GatewayConfig.CONFIG_NAME, GatewayConfig.class);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (Constants.HEADER.equalsIgnoreCase(gatewayConfig.getEgressIngressIndicator())) {
            if(logger.isTraceEnabled()) logger.trace("Outgoing request with header indicator");
            serviceDict(exchange);
        } else if (Constants.PROTOCOL.equalsIgnoreCase(gatewayConfig.getEgressIngressIndicator()) && HttpURL.PROTOCOL_HTTP.equalsIgnoreCase(exchange.getRequestScheme())){
            if(logger.isTraceEnabled()) logger.trace("Outgoing request with protocol indicator and http protocol");
            serviceDict(exchange);
        } else {
            if(logger.isTraceEnabled()) logger.trace("Incoming request");
        }
        Handler.next(exchange, next);
    }
}
