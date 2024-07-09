package com.networknt.cache;

import com.networknt.config.JsonMapper;
import com.networknt.handler.LightHttpHandler;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
    public static final String CACHE_NAME = "name";
    public static final String JWK = "jwk";
    public static final String OBJECT_NOT_FOUND = "ERR11637";

    private static final Logger logger = LoggerFactory.getLogger(CacheExplorerHandler.class);

    public CacheExplorerHandler() {
        if(logger.isInfoEnabled()) logger.info("CacheExplorerHandler constructed");
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        if(logger.isTraceEnabled()) logger.trace("CacheExplorerHandler handleRequest");
        String name = exchange.getQueryParameters().get(CACHE_NAME).getFirst();
        CacheManager cacheManager = SingletonServiceFactory.getBean(CacheManager.class);
        if(cacheManager != null) {
            Map<Object, Object> cacheMap = cacheManager.getCache(name);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            if(name.equals(JWK)) {
                Map<String, String> map = new HashMap<>();
                cacheMap.forEach((k, v) -> {
                    map.put((String)k, v.toString());
                });
                exchange.getResponseSender().send(JsonMapper.toJson(map));
            } else {
                exchange.getResponseSender().send(JsonMapper.toJson(cacheMap));
            }
        } else {
            setExchangeStatus(exchange, OBJECT_NOT_FOUND, "cache", name);
        }
    }
}
