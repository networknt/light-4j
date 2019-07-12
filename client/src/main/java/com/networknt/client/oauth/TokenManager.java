package com.networknt.client.oauth;

import com.networknt.client.ClientConfig;
import com.networknt.client.Http2Client;
import com.networknt.client.oauth.cache.ICacheStrategy;
import com.networknt.client.oauth.cache.LongestExpireCacheStrategy;
import com.networknt.config.Config;
import com.networknt.monad.Result;
import io.undertow.client.ClientRequest;
import io.undertow.util.HeaderValues;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is a singleton to manage ALL tokens.
 * This TokenManager provides a simple method to consumer to get a token.
 * It manages caches based on different cache strategies underneath.
 */
public class TokenManager {
    private Map<String, Object> clientConfig = Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_NAME);
    /**
     * @deprecated will be moved to {@link ClientConfig#CACHE}
     */
    @Deprecated
    public static final String CACHE = "cache";

    /**
     * @deprecated will be moved to {@link ClientConfig#OAUTH}
     */
    @Deprecated
    public static final String OAUTH = "oauth";

    /**
     * @deprecated will be moved to {@link ClientConfig#TOKEN}
     */
    @Deprecated
    public static final String TOKEN = "token";

    /**
     * @deprecated will be moved to {@link ClientConfig#CAPACITY}
     */
    @Deprecated
    public static final String CAPACITY_CONFIG = "capacity";

    private static volatile TokenManager INSTANCE;
    private static int CAPACITY = 200;

    private ICacheStrategy cacheStrategy;

    private TokenManager() {
        //set CAPACITY based on config
        Map<String, Object> tokenConfig = ClientConfig.get().getTokenConfig();
        if(tokenConfig != null) {
            Map<String, Object> cacheConfig = (Map<String, Object>)tokenConfig.get(ClientConfig.CACHE);
            if(cacheConfig != null) {
                if(cacheConfig.get(ClientConfig.CAPACITY) != null) {
                    CAPACITY = (Integer)cacheConfig.get(ClientConfig.CAPACITY);
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
        Jwt cachedJwt = getJwt(cacheStrategy, key);

        Result<Jwt> result = OauthHelper.populateCCToken(cachedJwt);
        //update JWT
        if (result.isSuccess()) {
            cacheStrategy.cacheJwt(key, result.getResult());
        }
        return result;
    }

    /**
     * cache jwt if not exist
     */
    private synchronized Jwt getJwt(ICacheStrategy cacheStrategy, Jwt.Key key) {
        Jwt result = cacheStrategy.getCachedJwt(key);
        if(result == null) {
            //cache an empty JWT first.
            result = new Jwt(key);
            cacheStrategy.cacheJwt(key, result);
        }
        return result;
    }

    /**
     * get a Jwt with a provided clientRequest,
     * it will get token based on Jwt.Key (either scope or service_id)
     * if the user declared both scope and service_id in header, it will get jwt based on scope
     * @param clientRequest client request
     * @return Result
     */
    public Result<Jwt> getJwt(ClientRequest clientRequest) {
        HeaderValues scope = clientRequest.getRequestHeaders().get(ClientConfig.SCOPE);
        if(scope != null) {
            String scopeStr = scope.getFirst();
            Set<String> scopeSet = new HashSet<>();
            scopeSet.addAll(Arrays.asList(scopeStr.split(" ")));
            return getJwt(new Jwt.Key(scopeSet));
        }
        HeaderValues serviceId = clientRequest.getRequestHeaders().get(ClientConfig.SERVICE_ID);
        if(serviceId != null) {
            return getJwt(new Jwt.Key(serviceId.getFirst()));
        }
        return getJwt(new Jwt.Key());
    }
}