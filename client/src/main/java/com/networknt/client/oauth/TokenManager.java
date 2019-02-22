package com.networknt.client.oauth;

import com.networknt.monad.Result;
import io.undertow.client.ClientRequest;
import io.undertow.util.HeaderValues;

import java.util.HashMap;
import java.util.Map;

public class TokenManager {

    private static TokenManager INSTANCE;
    private Map<Integer, Jwt> cachedJwts = new HashMap<>();

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

    /**
     * get a Jwt with a provided Key (Key is either scope or a service id inputted by user, for caching usage):
     * 1.if a token is cached with provided key
     *      - if the token is expired, renew it right away.
     *      - if the token is almost expired, use this token and renew it silently.
     *      - if the token is not almost expired, just use this token.
     * 2.if a token is not cached with provided key
     *      - get a new jwt from oauth server
     * @param key either based on scope or service id
     * @return a Jwt if successful, otherwise return error Status.
     */
    public Result<Jwt> getJwt(Jwt.Key key) {
        Jwt cachedJwt = cachedJwts.get(key);
        Result<Jwt> result = cachedJwt == null ? OauthHelper.populateCCToken(new Jwt(key)) : OauthHelper.populateCCToken(cachedJwt);
        if (result.isSuccess()) {
            cachedJwts.put(key.hashCode(), result.getResult());
        }
        return result;
    }

    /**
     * get a Jwt with a provided clientRequest, if the user declared scope in header, it will get jwt based on scope
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
