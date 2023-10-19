package com.networknt.client.oauth;

import com.networknt.client.ClientConfig;
import com.networknt.client.oauth.cache.ICacheStrategy;
import com.networknt.client.oauth.cache.LongestExpireCacheStrategy;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This class is a singleton to manage ALL tokens.
 * This TokenManager provides a simple method to consumer to get a token.
 * It manages caches based on different cache strategies underneath.
 */
public class TokenManager {
    private Logger logger = LoggerFactory.getLogger(TokenManager.class);
    private static final String CONFIG_PROPERTY_MISSING = "ERR10057";

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
     * @param ccConfig a map of target auth server client credentials config
     * @return a Jwt if successful, otherwise return error Status.
     */
    public Result<Jwt> getJwt(Jwt.Key key, Map<String, Object> ccConfig) {
        Jwt cachedJwt = getJwt(cacheStrategy, key);
        if(ccConfig != null) cachedJwt.setCcConfig(ccConfig);
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
     * get a Jwt with a provided clientRequest. Decision needs to be made based on the path if
     * multiple auth server is configured in the client.yml file.
     * it will get token based on Jwt.Key (either scope or service_id) first from the cache.
     * if the user declared both scope and service_id in header, it will get jwt based on scope
     * @param requestPath String
     * @param scopes String
     * @param serviceId String
     * @return Result
     */
    public Result<Jwt> getJwt(String requestPath, String scopes, String serviceId) {
        // check the client.yml to see if multiple auth server is enabled.
        if(ClientConfig.get().isMultipleAuthServers()) {
            if(logger.isTraceEnabled()) logger.trace("requestPath = " + requestPath + " scopes = " + scopes + " serviceId = " + serviceId);
            // Get the target serviceId based on the request path.
            Map<String, String> pathPrefixServices = ClientConfig.get().getPathPrefixServices();
            // lookup the serviceId based on the full path and the prefix mapping by iteration here.
            for(Map.Entry<String, String> entry: pathPrefixServices.entrySet()) {
                if(requestPath.startsWith(entry.getKey())) {
                    serviceId = entry.getValue();
                }
            }
            if(logger.isTraceEnabled()) logger.trace("serviceId = " + serviceId);
            // based on the serviceId, we can find the configuration of the auth server from the client credentials
            Map<String, Object> clientCredentials = (Map<String, Object>)ClientConfig.get().getTokenConfig().get(ClientConfig.CLIENT_CREDENTIALS);
            Map<String, Object> serviceIdAuthServers = (Map<String, Object>)clientCredentials.get(ClientConfig.SERVICE_ID_AUTH_SERVERS);
            if(serviceIdAuthServers == null) {
                Status status = new Status(CONFIG_PROPERTY_MISSING, "serviceIdAuthServers", "client.yml");
                return Failure.of(status);
            }
            Map<String, Object> ccConfig = (Map<String, Object>)serviceIdAuthServers.get(serviceId);
            // pass the ccConfig to the get token request so that this section can be populated with the object.
            // we always use the serviceId as the cache in the multiple auth server situation, and it won't be null.
            return getJwt(new Jwt.Key(serviceId), ccConfig);
        } else {
            // single auth server, keep the existing logic.
            if(scopes != null) {
                Set<String> scopeSet = new HashSet<>();
                scopeSet.addAll(Arrays.asList(scopes.split(" ")));
                return getJwt(new Jwt.Key(scopeSet), null);
            }
            if(serviceId != null) {
                return getJwt(new Jwt.Key(serviceId), null);
            }
            return getJwt(new Jwt.Key(), null);
        }
    }
}
