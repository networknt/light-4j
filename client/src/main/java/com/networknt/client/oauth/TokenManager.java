package com.networknt.client.oauth;

import com.networknt.client.Http2Client;
import com.networknt.client.oauth.cache.ICacheStrategy;
import com.networknt.client.oauth.cache.LongestExpireCacheStrategy;
import com.networknt.config.Config;
import com.networknt.monad.Result;
import io.undertow.client.ClientRequest;
import io.undertow.util.HeaderValues;

import java.util.Map;

/**
 * This class is a singleton to manage ALL tokens.
 * This TokenManager provides a simple method to consumer to get a token.
 * It manages caches based on different cache strategies underneath.
 */
public class TokenManager {
    Map<String, Object> clientConfig = Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_NAME);
    public static final String CACHE = "cache";
    public static final String CAPACITY_CONFIG = "capacity";

    private static volatile TokenManager INSTANCE;
    private static int CAPACITY = 200;

    private ICacheStrategy cacheStrategy;

    private TokenManager() {
        //set CAPACITY based on config
        if(clientConfig != null) {
            Map<String, Object> cacheConfig = (Map<String, Object>)clientConfig.get(CACHE);
            if(cacheConfig != null) {
                if(cacheConfig.get(CAPACITY_CONFIG) != null) {
                    CAPACITY = (Integer)cacheConfig.get(CAPACITY_CONFIG);
                }
            }
        }
        cacheStrategy = new LongestExpireCacheStrategy(CAPACITY);
    }

    public static TokenManager getInstance() {
        if(INSTANCE == null) {
            synchronized (TokenManager.class) {
                if(INSTANCE == null) {
                    INSTANCE = new TokenManager();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * get a Jwt with a provided Key (Key is either scope or a service id inputted by user, for caching usage):
     * 1.if a token is cached with provided key
     *      - if the token is expired, renew it right away.
     *      - if the token is almost expired, use this token and renew it silently.
     *      - if the token is not almost expired, just use this token.
     * 2.if a token is not cached with provided key
     *      - get a new jwt from oauth server
     * 3.after getting the valid token, cache that token no matter if it's already cached or not. The strategy should determine how to cache it.
     * @param key either based on scope or service id
     * @return a Jwt if successful, otherwise return error Status.
     */
    public Result<Jwt> getJwt(Jwt.Key key) {
        Jwt cachedJwt = cacheStrategy.getCachedJwt(key);
        Result<Jwt> result = cachedJwt == null ? OauthHelper.populateCCToken(new Jwt(key)) : OauthHelper.populateCCToken(cachedJwt);
        if (result.isSuccess()) {
            cacheStrategy.cacheJwt(key, result.getResult());
        }
        return result;
    }

    /**
     * get a Jwt with a provided clientRequest,
     * it will get token based on Jwt.Key (either scope or service_id)
     * if the user declared both scope and service_id in header, it will get jwt based on scope
     * @param clientRequest
     * @return
     */
    public Result<Jwt> getJwt(ClientRequest clientRequest) {
        HeaderValues scope = clientRequest.getRequestHeaders().get(OauthHelper.SCOPE);
        if(scope != null) {
            return getJwt(new Jwt.Key(scope.getFirst()));
        }
        HeaderValues serviceId = clientRequest.getRequestHeaders().get(OauthHelper.SERVICE_ID);
        if(serviceId != null) {
            return getJwt(new Jwt.Key(serviceId.getFirst()));
        }
        return getJwt(new Jwt.Key());
    }
}