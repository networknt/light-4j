package com.networknt.restrans;

import com.networknt.cache.CacheManager;
import com.networknt.config.Config;
import com.networknt.handler.BuffersUtils;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.ResponseInterceptor;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.httpstring.CacheTask;
import com.networknt.rule.RuleConstants;
import com.networknt.rule.RuleLoaderStartupHook;
import com.networknt.utility.ConfigUtils;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.networknt.utility.Constants.ERROR_MESSAGE;

/**
 * This is a generic response interceptor for caching the downstream response to save the external API calls on the
 * light-gateway or http-sidecar. Please make sure that you only cache the response for certain period of time you
 * know it will be the same. Another usage is to cache the jwt tokens from cloud service to save the cost as a token
 * can last at 10 to 20 minutes.
 *
 * The interceptor depending on the cache.yml configuration to set up a cache and expiration time. It checks exchange
 * attachment to determine if it takes action with the cache name and the key for the cache. The usage of the cache
 * is up for the individual handle who is sending the attachment.
 *
 * Please note that the interceptor will only take action if response code is successful (less than 400).
 *
 * @author Steve Hu
 */
public class ResponseCacheInterceptor implements ResponseInterceptor {
    static final Logger logger = LoggerFactory.getLogger(ResponseCacheInterceptor.class);
    private static final String RESPONSE_BODY = "responseBody";
    private static final String METHOD = "method";
    private static final String POST = "post";
    private static final String PUT = "put";
    private static final String PATCH = "patch";
    private static final String AUDIT_INFO = "auditInfo";
    private static final String STATUS_CODE = "statusCode";

    private static final String RESPONSE_FILTER = "res-fil";
    private static final String PERMISSION = "permission";

    private static ResponseCacheConfig config;
    private volatile HttpHandler next;

    /**
     * ResponseCacheInterceptor constructor
     */
    public ResponseCacheInterceptor() {
        if (logger.isInfoEnabled()) logger.info("ResponseCacheInterceptor is loaded");
        config = ResponseCacheConfig.load();
        ModuleRegistry.registerModule(ResponseCacheConfig.CONFIG_NAME, ResponseCacheInterceptor.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(ResponseCacheConfig.CONFIG_NAME), null);
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(HttpHandler next) {
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
        ModuleRegistry.registerModule(ResponseCacheConfig.CONFIG_NAME, ResponseCacheInterceptor.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(ResponseCacheConfig.CONFIG_NAME), null);
    }

    @Override
    public void reload() {
        config.reload();
        ModuleRegistry.registerModule(ResponseCacheConfig.CONFIG_NAME, ResponseCacheInterceptor.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(ResponseCacheConfig.CONFIG_NAME), null);
        if(logger.isTraceEnabled()) logger.trace("ResponseCacheInterceptor is reloaded.");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (logger.isDebugEnabled()) logger.trace("ResponseCacheInterceptor.handleRequest starts.");
        // check the status code. the cache will only be applied to successful request.
        if(exchange.getStatusCode() >= 400) {
            if(logger.isTraceEnabled()) logger.trace("Skip on error code {}.  ResponseCacheInterceptor.handleRequest ends.", exchange.getStatusCode());
            return;
        }
        String requestPath = exchange.getRequestPath();
        // check if the request path should be applied for cache.
        if (config.getAppliedPathPrefixes() != null) {
            Optional<String> match = findMatchingPrefix(requestPath, config.getAppliedPathPrefixes());
            if(match.isPresent()) {
                // path prefix is matched, check if the exchange attachment exists.
                CacheTask cacheTask = exchange.getAttachment(AttachmentConstants.RESPONSE_CACHE);
                if(cacheTask != null) {
                    // the attachment exists, perform the caching.
                    String responseBody = BuffersUtils.toString(getBuffer(exchange), StandardCharsets.UTF_8);
                    if (logger.isTraceEnabled())
                        logger.trace("original response body = {}", responseBody);
                    String name = cacheTask.getName();
                    String key = cacheTask.getKey();
                    CacheManager cacheManager = CacheManager.getInstance();
                    if(cacheManager == null) {
                        logger.error("Could not get CacheManager instance");
                    } else {
                        Map<Object, Object> cache = cacheManager.getCache(name);
                        if(cache == null) {
                            logger.error("Cache {} is not configured in cache.yml", name);
                        } else {
                            if(logger.isTraceEnabled()) logger.trace("put key {} into cache {}", key, name);
                            cacheManager.put(name, key, responseBody);
                        }
                    }
                }
            }
        }
        if (logger.isDebugEnabled()) logger.trace("ResponseCacheInterceptor.handleRequest ends.");
    }

    @Override
    public boolean isRequiredContent() {
        return true;
    }

}
