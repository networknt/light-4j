package com.networknt.cache;

import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is one of the adm handlers that is used to explore the cache. It has an optional query
 * parameter to specify the cache name. If the cache name is not specified, it will return all
 * the general info about caches. If the cache name is specified, it will return the cache key
 * and value pairs in a list to the caller.
 *
 * If a user knows the cache name, he can use the cache name to get the cache directly from
 * the CacheManager.
 *
 * @author Steve Hu
 */
public class CacheExplorerHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(CacheExplorerHandler.class);
    public CacheExplorerHandler() {
        if(logger.isInfoEnabled()) logger.info("CacheExplorerHandler constructed");
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        if(logger.isTraceEnabled()) logger.trace("CacheExplorerHandler handleRequest");

    }
}
