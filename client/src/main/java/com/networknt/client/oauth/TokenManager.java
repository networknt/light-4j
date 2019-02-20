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

    public Result<Jwt> getJwt(Set<String> scopes) {
        Jwt cachedJwt = cachedJwts.get(scopes);
        Result<Jwt> result = cachedJwt == null ? OauthHelper.populateCCToken(new Jwt()) : OauthHelper.populateCCToken(cachedJwt);
        if (result.isSuccess()) {
            cachedJwts.put(scopes, result.getResult());
        }
        return result;
    }
}
