package com.networknt.router.middleware;

import com.networknt.handler.AuditAttachmentUtil;
import com.networknt.handler.Handler;
import com.networknt.handler.config.HandlerUtils;
import com.networknt.router.RouterConfig;
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

    public SidecarPathPrefixServiceHandler() {
        logger.info("SidecarPathPrefixServiceHandler is constructed");
        SidecarConfig.load();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        SidecarConfig config = SidecarConfig.load();
        if(logger.isDebugEnabled()) logger.debug("SidecarPathPrefixServiceHandler.handleRequest starts.");
        if (Constants.HEADER.equalsIgnoreCase(config.getEgressIngressIndicator())) {
            if(logger.isTraceEnabled()) logger.trace("Outgoing request calls PathPrefixServiceHandler with header indicator");
            super.handleRequest(exchange);
        } else if (Constants.PROTOCOL.equalsIgnoreCase(config.getEgressIngressIndicator()) && HttpURL.PROTOCOL_HTTP.equalsIgnoreCase(exchange.getRequestScheme())){
            if(logger.isTraceEnabled()) logger.trace("Outgoing request calls PathPrefixServiceHandler with protocol indicator and http protocol");
            super.handleRequest(exchange);
        } else {
            // incoming request, let the proxy handler to handle it.
            if(logger.isDebugEnabled()) logger.debug("SidecarPathPrefixServiceHandler.handleRequest ends for incoming request.");
            String requestPath = exchange.getRequestURI();
            String[] serviceEntry = HandlerUtils.findServiceEntry(HandlerUtils.normalisePath(requestPath), PathPrefixServiceConfig.load().getMapping());
            if(serviceEntry != null)
                AuditAttachmentUtil.populateAuditAttachmentField(exchange, Constants.ENDPOINT_STRING, serviceEntry[0] + "@" + exchange.getRequestMethod().toString().toLowerCase());
            Handler.next(exchange, next);
        }
    }
}
