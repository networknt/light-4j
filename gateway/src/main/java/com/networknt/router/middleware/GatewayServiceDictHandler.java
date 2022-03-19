package com.networknt.router.middleware;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.router.GatewayConfig;
import com.networknt.url.HttpURL;
import com.networknt.utility.Constants;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;

public class GatewayServiceDictHandler extends ServiceDictHandler {

    public static GatewayConfig gatewayConfig = (GatewayConfig)Config.getInstance().getJsonObjectConfig(GatewayConfig.CONFIG_NAME, GatewayConfig.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        if (Constants.HEADER.equalsIgnoreCase(gatewayConfig.getEgressIngressIndicator())) {
            serviceDict(exchange);
        } else if (Constants.PROTOCOL.equalsIgnoreCase(gatewayConfig.getEgressIngressIndicator()) && HttpURL.PROTOCOL_HTTP.equalsIgnoreCase(exchange.getRequestScheme())){
            serviceDict(exchange);
        } else {
            Handler.next(exchange, next);
        }
    }

    protected void serviceDict(HttpServerExchange exchange) throws Exception {
        HeaderValues serviceIdHeader = exchange.getRequestHeaders().get(HttpStringConstants.SERVICE_ID);
        String serviceId = serviceIdHeader != null ? serviceIdHeader.peekFirst() : null;
        if (serviceId == null) {
            String requestPath = exchange.getRequestURI();
            String httpMethod = exchange.getRequestMethod().toString().toLowerCase();
            serviceId = HandlerUtils.findServiceId(toInternalKey(httpMethod, requestPath), mappings);
            if (serviceId != null) {
                exchange.getRequestHeaders().put(HttpStringConstants.SERVICE_ID, serviceId);
            }
        }

        Handler.next(exchange, this.next);
    }


}
