package com.networknt.router.middleware;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.StringUtils;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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
	protected static final String INTERNAL_KEY_FORMAT = "%s %s";
	
    public static final String CONFIG_NAME = "serviceDict";
    public static final String ENABLED = "enabled";
    public static final String MAPPING = "mapping";
    public static final String DELIMITOR = "@";

    public static Map<String, Object> config = Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME);
	protected static Map<String, String> rawMappings = (Map<String, String>)config.get(MAPPING);
	public static Map<String, String> mappings;
	
	static {
		mappings = new HashMap<>();
		
		for (Map.Entry<String, String> entry : rawMappings.entrySet()) {
			mappings.put(toInternalKey(entry.getKey()), entry.getValue());
		}
		
		mappings = Collections.unmodifiableMap(mappings);
	}
	
    
    protected volatile HttpHandler next;

    static final String STATUS_INVALID_REQUEST_PATH = "ERR10007";

    public ServiceDictHandler() {
        logger.info("ServiceDictHandler is constructed");
    }

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
        HeaderValues serviceIdHeader = exchange.getRequestHeaders().get(HttpStringConstants.SERVICE_ID);
        String serviceId = serviceIdHeader != null ? serviceIdHeader.peekFirst() : null;
        if(serviceId == null) {
            String requestPath = exchange.getRequestURI();
            String httpMethod = exchange.getRequestMethod().toString().toLowerCase();

            serviceId = HandlerUtils.findServiceId(toInternalKey(httpMethod, requestPath), mappings);
            if(serviceId == null) {
                HeaderValues serviceUrlHeader = exchange.getRequestHeaders().get(HttpStringConstants.SERVICE_URL);
                String serviceUrl = serviceUrlHeader != null ? serviceUrlHeader.peekFirst() : null;
                if (serviceUrl==null) {
                    setExchangeStatus(exchange, STATUS_INVALID_REQUEST_PATH, requestPath);
                    return;
                }
            } else {
                exchange.getRequestHeaders().put(HttpStringConstants.SERVICE_ID, serviceId);
            }
        }
        Handler.next(exchange, next);
	}

    private static String toInternalKey(String key) {
    	String[] tokens = StringUtils.trimToEmpty(key).split(DELIMITOR);
    	
    	if (tokens.length ==2) {
    		return toInternalKey(tokens[1], tokens[0]);
    	}
    	
    	logger.warn("Invalid key {}", key);
    	return key;
    }
    
    protected static String toInternalKey(String method, String path) {
    	return String.format(INTERNAL_KEY_FORMAT, method, HandlerUtils.normalisePath(path));
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
        Object object = config.get(ENABLED);
        return object != null && (Boolean)object;
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(ServiceDictHandler.class.getName(), config, null);
    }


}
