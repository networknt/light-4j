package com.networknt.client.oauth;

import com.networknt.monad.Result;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TokenManager {

    private static TokenManager INSTANCE;
    private Map<Set<String>, Jwt> cachedJwts = new HashMap<>();

    private TokenManager() {}

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

    public Result<Jwt> getJwt(Jwt jwt) {
        Set<String> scopes = jwt.getScopes();
        return getJwt(scopes);
    }

    /**
     * get a Jwt within a provided scope:
     * 1.if a token is cached with provided scope
     *      - if the token is expired, renew it right away.
     *      - if the token is almost expired, use this token and renew it silently.
     *      - if the token is not almost expired, just use this token.
     * 2.if a token is not cached with provided scope
     *      - get a new jwt from oauth server
     * @param scopes a set of scopes
     * @return a Jwt if successful, otherwise return error Status.
     */
    public Result<Jwt> getJwt(Set<String> scopes) {
        Jwt cachedJwt = cachedJwts.get(scopes);
        Result<Jwt> result = cachedJwt == null ? OauthHelper.populateCCToken(new Jwt(scopes)) : OauthHelper.populateCCToken(cachedJwt);
        if (result.isSuccess()) {
            cachedJwts.put(scopes, result.getResult());
        }
        return result;
    }
}
