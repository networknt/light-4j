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
 * This is the same handler as the PathPrefixServiceHandler used in the client proxy and gateway;
 * however, it has logic to detect the traffic is incoming or outgoing. This handler is used for
 * outgoing traffic only for service to service invocation.
 * It depends on the gateway.yml to detect the traffic based on either request header or protocol.
 *
 * @author Steve Hu
 */
public class GatewayPathPrefixServiceHandler extends PathPrefixServiceHandler {
    private static final Logger logger = LoggerFactory.getLogger(GatewayPathPrefixServiceHandler.class);

    private GatewayConfig gatewayConfig;

    public GatewayPathPrefixServiceHandler() {
        logger.info("GatewayPathPrefixServiceHandler is constructed");
        config = PathPrefixServiceConfig.load();
        gatewayConfig = (GatewayConfig) Config.getInstance().getJsonObjectConfig(GatewayConfig.CONFIG_NAME, GatewayConfig.class);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (Constants.HEADER.equalsIgnoreCase(gatewayConfig.getEgressIngressIndicator())) {
            pathPrefixService(exchange);
        } else if (Constants.PROTOCOL.equalsIgnoreCase(gatewayConfig.getEgressIngressIndicator()) && HttpURL.PROTOCOL_HTTP.equalsIgnoreCase(exchange.getRequestScheme())){
            pathPrefixService(exchange);
        } else {
            // incoming request, let the proxy handler to handle it.
            Handler.next(exchange, next);
        }
    }

    protected void pathPrefixService(HttpServerExchange exchange) throws Exception {
        String[] serviceEntry = null;
        HeaderValues serviceUrlHeader = exchange.getRequestHeaders().get(HttpStringConstants.SERVICE_URL);
        String serviceUrl = serviceUrlHeader != null ? serviceUrlHeader.peekFirst() : null;
        if (serviceUrl == null) {
            HeaderValues serviceIdHeader = exchange.getRequestHeaders().get(HttpStringConstants.SERVICE_ID);
            String serviceId = serviceIdHeader != null ? serviceIdHeader.peekFirst() : null;
            if (serviceId == null) {
                String requestPath = exchange.getRequestURI();
                serviceEntry = HandlerUtils.findServiceEntry(HandlerUtils.normalisePath(requestPath), config.getMapping());
                if (serviceEntry != null) {
                    exchange.getRequestHeaders().put(HttpStringConstants.SERVICE_ID, serviceEntry[1]);
                }
            }
        }
        Map<String, Object> auditInfo = exchange.getAttachment(AttachmentConstants.AUDIT_INFO);
        if(auditInfo == null) {
            // AUDIT_INFO is created for light-gateway to populate the endpoint as the OpenAPI handlers might not be available.
            auditInfo = new HashMap<>();
            auditInfo.put(Constants.ENDPOINT_STRING, serviceEntry[0] + "@" + exchange.getRequestMethod().toString().toLowerCase());
            exchange.putAttachment(AttachmentConstants.AUDIT_INFO, auditInfo);
        }
        Handler.next(exchange, this.next);
    }
}
