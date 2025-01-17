package com.networknt.token.limit;

import com.networknt.cache.CacheManager;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.httpstring.CacheTask;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import java.nio.ByteBuffer;

/**
 * This handler should be used on the oauth-kafka or a dedicated light-gateway instance for all OAuth 2.0
 * instances or providers.
 *
 * @author Steve Hu
 */
public class TokenLimitHandler implements MiddlewareHandler {
    static final Logger logger = LoggerFactory.getLogger(TokenLimitHandler.class);
    // the cacheName in the cache.yml
    static final String TOKEN_LIMIT = "token-limit";
    static final String CLIENT_TOKEN = "client-token";
    static final String GRANT_TYPE = "grant_type";
    static final String CLIENT_CREDENTIALS = "client_credentials";
    static final String AUTHORIZATION_CODE = "authorization_code";
    static final String CLIENT_ID = "client_id";
    static final String CLIENT_SECRET = "client_secret";
    static final String SCOPE = "scope";
    static final String CODE = "code";
    static final String TOKEN_LIMIT_ERROR = "ERR10091";

    private volatile HttpHandler next;
    private final TokenLimitConfig config;
    private List<Pattern> patterns;

    CacheManager cacheManager = CacheManager.getInstance();

    public TokenLimitHandler() throws Exception{
        config = TokenLimitConfig.load();
        List<String> tokenPathTemplates = config.getTokenPathTemplates();
        if(tokenPathTemplates != null && !tokenPathTemplates.isEmpty()) {
            patterns = tokenPathTemplates.stream().map(Pattern::compile).collect(Collectors.toList());
        }
        logger.info("TokenLimitHandler constructed.");
    }

    /**
     * This is a constructor for test cases only. Please don't use it.
     *
     * @param cfg token limit config
     * @throws Exception thrown when config is wrong.
     *
     */
    @Deprecated
    public TokenLimitHandler(TokenLimitConfig cfg) throws Exception{
        config = cfg;
        List<String> tokenPathTemplates = config.getTokenPathTemplates();
        if(tokenPathTemplates != null && !tokenPathTemplates.isEmpty()) {
            patterns = tokenPathTemplates.stream().map(Pattern::compile).collect(Collectors.toList());
        }
        logger.info("TokenLimitHandler constructed.");
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
        return config.isEnabled();    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(TokenLimitConfig.CONFIG_NAME, TokenLimitHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(TokenLimitConfig.CONFIG_NAME), null);

    }

    @Override
    public void reload() {
        config.reload();
        // after reload, we need to update the config in the module registry to ensure that server info returns the latest configuration.
        ModuleRegistry.registerModule(TokenLimitConfig.CONFIG_NAME, TokenLimitHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(TokenLimitConfig.CONFIG_NAME), null);
        if(logger.isInfoEnabled()) logger.info("TokenLimitHandler is reloaded.");
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("TokenLimitHandler.handleRequest starts.");
        String key = null;
        // get the client ip address.
        InetSocketAddress sourceAddress = exchange.getSourceAddress();
        String clientIpAddress = sourceAddress.getAddress().getHostAddress();
        if(logger.isTraceEnabled()) logger.trace("client address {}", clientIpAddress);

        // firstly, we need to identify if the request path ends with /token. If not, call next handler.
        String requestPath = exchange.getRequestPath();
        if(matchPath(requestPath) && cacheManager != null) {
            if(logger.isTraceEnabled()) logger.trace("request path {} matches with one of the {} patterns.", requestPath, config.getTokenPathTemplates().size());
            // this assumes that either BodyHandler(oauth-kafka) or RequestBodyInterceptor(light-gateway) is used in the chain.
            String requestBodyString = exchange.getAttachment(AttachmentConstants.REQUEST_BODY_STRING);
            if(logger.isTraceEnabled()) logger.trace("requestBodyString = {}", requestBodyString);
            // grant_type=client_credentials&client_id=0oa6wgbkbcF27GoqA1d7&client_secret=GInDco_MGt6Fz0oHxmgk1LluEHb6qJ4RQY0MvH3Q&scope=lg.localoauth.corp
            // convert the string to a hashmap via a converter. We need to consider both x-www-urlencoded and json request body.
            Map<String, String> bodyMap = convertStringToHashMap(requestBodyString);
            String grantType = bodyMap.get(GRANT_TYPE);
            String clientId = bodyMap.get(CLIENT_ID);

            // secondly, we need to identify if the ClientID is considered Legacy or not. If it is, bypass limit, cache and call next handler.
            List<String> legacyClient = config.getLegacyClient();
            if(legacyClient.contains(clientId)) {
                if(logger.isTraceEnabled()) logger.trace("client {} is configured as Legacy, bypass the token limit.", clientId);
                //  check if cache key exists in cache manager, if exists return cached token
                key = clientId + ":" + bodyMap.get(CLIENT_SECRET) + ":" + bodyMap.get(SCOPE).replace(" ", "");
                ByteBuffer cachedResponse = (ByteBuffer)cacheManager.get(CLIENT_TOKEN, key);
                if (cachedResponse != null) {
                    if(logger.isTraceEnabled()) logger.trace("legacy client cache key {} has token value, returning cached token.", key);
                    exchange.getResponseSender().send(cachedResponse);
                } else {
                    if(logger.isTraceEnabled()) logger.trace("legacy client cache key {} has NO token cached, calling next handler.", key);
                    exchange.putAttachment(AttachmentConstants.RESPONSE_CACHE, new CacheTask(CLIENT_TOKEN, key));
                    Handler.next(exchange, next);
                }
                return;
            }

            // construct the key based on grant_type and client_id or code.
            if(grantType.equals(CLIENT_CREDENTIALS)) {
                key = clientId + ":" + sourceAddress;
                if(logger.isTraceEnabled()) logger.trace("client credentials key = {}", key);
            } else if(grantType.equals(AUTHORIZATION_CODE)) {
                String code = bodyMap.get(CODE);
                key = clientId  + ":" + code + ":" + sourceAddress;
                if(logger.isTraceEnabled()) logger.trace("authorization code key = {}", key);
            } else {
                // other grant_type, ignore it.
                if(logger.isTraceEnabled()) logger.trace("other grant type {}, ignore it", grantType);
            }

            if(key != null) {
                // check if the key is in the cache manager.
                Integer count = (Integer)cacheManager.get(TOKEN_LIMIT, key);
                if(count != null) {
                    // check if the count is reached limit already.
                    if(count >= config.duplicateLimit) {
                        if(config.errorOnLimit) {
                            // return an error to the caller.
                            setExchangeStatus(exchange, TOKEN_LIMIT_ERROR);
                        } else {
                            // log the error in the log.
                            logger.error("Too many token requests. Please cache the token on the client side.");
                        }
                    } else {
                        cacheManager.put(TOKEN_LIMIT, key, ++count);
                    }
                } else {
                    // add count 1 into the cache.
                    cacheManager.put(TOKEN_LIMIT, key, 1);
                }
            }
        }
        Handler.next(exchange, next);
        if(logger.isDebugEnabled()) logger.debug("TokenLimitHandler.handleRequest ends.");
    }

    public Map<String, String> convertStringToHashMap(String input) {
        Map<String, String> map = new HashMap<>();

        String[] pairs = input.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];
                map.put(key, value);
            }
        }
        return map;
    }

    public boolean matchPath(String path) {
        // if there is no configured pattern, not matched.
        if(patterns == null || patterns.isEmpty()) return false;
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(path);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }
}
