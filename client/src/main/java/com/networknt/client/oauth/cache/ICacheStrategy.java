package com.networknt.client.oauth.cache;

import com.networknt.client.oauth.Jwt;

/**
 * An interface to describes a cache strategy.
 */
public interface ICacheStrategy {
    /**
     * caches a jwt to the cache pool with provided cache key and jwt.
     * @param key Jwt.Key cache key
     * @param jwt Jwt
     */
    void cacheJwt(Jwt.Key key, Jwt jwt);

    /**
     * get Jwt from cache pool with provided Jwt.Key cache key
     * @param key Jwt.Key
     * @return Jwt jwt
     */
    Jwt getCachedJwt(Jwt.Key key);
}
