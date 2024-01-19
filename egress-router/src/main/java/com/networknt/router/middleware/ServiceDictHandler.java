package com.networknt.router.middleware;

import com.networknt.handler.Handler;
import com.networknt.handler.HandlerUtils;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Find service Ids using a combination of path prefix and request method.
 * 
 * @author Daniel Zhao
 *
 */

@SuppressWarnings("unchecked")
public class ServiceDictHandler implements MiddlewareHandler {
	private static final Logger logger = LoggerFactory.getLogger(ServiceDictHandler.class);
    protected volatile HttpHandler next;
    protected static ServiceDictConfig config;

    public ServiceDictHandler() {
        logger.info("ServiceDictHandler is constructed");
        config = ServiceDictConfig.load();
    }

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("ServiceDictHandler.handleRequest starts.");
        serviceDict(exchange);
        if(logger.isDebugEnabled()) logger.debug("ServiceDictHandler.handleRequest ends.");
        Handler.next(exchange, next);
	}

    protected void serviceDict(HttpServerExchange exchange) throws Exception {
        String requestPath = exchange.getRequestURI();
        String httpMethod = exchange.getRequestMethod().toString().toLowerCase();
        String[] serviceEntry = HandlerUtils.findServiceEntry(HandlerUtils.toInternalKey(httpMethod, requestPath), config.getMapping());

        HeaderValues serviceIdHeader = exchange.getRequestHeaders().get(HttpStringConstants.SERVICE_ID);
        String serviceId = serviceIdHeader != null ? serviceIdHeader.peekFirst() : null;
        if(serviceId == null && serviceEntry != null) {
            if(logger.isTraceEnabled()) logger.trace("serviceEntry found and header is set for service_id = " + serviceEntry[1]);
            exchange.getRequestHeaders().put(HttpStringConstants.SERVICE_ID, serviceEntry[1]);
        }

        if (serviceEntry != null) {
            if (logger.isTraceEnabled())
                logger.trace("serviceEntry found and endpoint is set to = '{}'", serviceEntry[0]);
            HandlerUtils.populateAuditAttachmentField(exchange, Constants.ENDPOINT_STRING, serviceEntry[0]);
        } else {
            if (logger.isTraceEnabled())
                logger.trace("serviceEntry is null and endpoint is set to = '{}@{}'", Constants.UNKOWN_STRING, exchange.getRequestMethod().toString().toLowerCase());
            // at this moment, we don't have a way to reliably determine the endpoint.
            HandlerUtils.populateAuditAttachmentField(exchange, Constants.ENDPOINT_STRING, Constants.UNKOWN_STRING + "@" + exchange.getRequestMethod().toString().toLowerCase());
        }
    }

	@Override
    public HttpHandler getNext() {
        return next;
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
        ModuleRegistry.registerModule(ServiceDictConfig.CONFIG_NAME, ServiceDictHandler.class.getName(), config.getMappedConfig(), null);
    }

    @Override
    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(ServiceDictConfig.CONFIG_NAME, ServiceDictHandler.class.getName(), config.getMappedConfig(), null);
        if(logger.isInfoEnabled()) logger.info("ServiceDictHandler is reloaded.");
    }
}
