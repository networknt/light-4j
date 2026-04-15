package com.networknt.token;

import com.networknt.cache.CacheManager;

/**
 * CacheTokenClient implements the Decorator Pattern to wrap an underlying TokenClient (like HttpTokenClient)
 * with the core Light-4j CacheManager. Because CacheManager dynamically resolves to either a local Caffeine 
 * cache or a distributed Redis/Hazelcast cache based on service.yml, this completely fulfills L1 and L2 mapping!
 */
public class CacheTokenClient implements TokenClient {
    private static final String CACHE_NAME = "token_vault_cache";
    private final TokenClient delegate;
    
    public CacheTokenClient(TokenClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public String tokenize(String value, int schemeId) {
        String cacheKey = schemeId + ":" + value;
        
        CacheManager cache = CacheManager.getInstance();
        if (cache != null) {
            Object cachedToken = cache.get(CACHE_NAME, cacheKey);
            if (cachedToken != null) {
                return (String) cachedToken;
            }
        }

        // Cache Miss: Drop down to the delegate (e.g. underlying L3 Http service)
        String generatedToken = delegate.tokenize(value, schemeId);
        
        if (cache != null && generatedToken != null && !generatedToken.equals(value)) {
            // Bi-directional caching. Cache the cleartext -> token
            cache.put(CACHE_NAME, cacheKey, generatedToken);
            // Cache the token -> cleartext for fast reverse lookup
            cache.put(CACHE_NAME, "reverse:" + generatedToken, value);
        }
        
        return generatedToken;
    }

    @Override
    public String detokenize(String token) {
        String cacheKey = "reverse:" + token;
        
        CacheManager cache = CacheManager.getInstance();
        if (cache != null) {
            Object cachedCleartext = cache.get(CACHE_NAME, cacheKey);
            if (cachedCleartext != null) {
                return (String) cachedCleartext;
            }
        }

        // Cache Miss
        String cleartext = delegate.detokenize(token);
        
        if (cache != null && cleartext != null && !cleartext.equals(token)) {
            cache.put(CACHE_NAME, cacheKey, cleartext);
        }
        
        return cleartext;
    }
}
