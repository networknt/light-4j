package com.networknt.client.oauth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.status.Status;
import io.undertow.client.ClientRequest;
import io.undertow.util.HeaderValues;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TokenManager {

    private TokenManager INSTANCE;
    private Map<Set<String>, Jwt> cachedJwts = new HashMap<>();
    private final static String SCOPES_HEADER = "scopes";
    private final static String UNABLE_PARSE_SCOPES_FROM_HEADER = "ERR13001";
    private TokenManager() {}

    public TokenManager getInstance() {
        if(INSTANCE == null) {
            synchronized (this) {
                if(INSTANCE == null) {
                    new TokenManager();
                }
            }
        }
        return INSTANCE;
    }

    public Result<Jwt> getJwt(ClientRequest request) {
        HeaderValues scopesValues = request.getRequestHeaders().get(SCOPES_HEADER);
        if(scopesValues == null) { return Failure.of(new Status(UNABLE_PARSE_SCOPES_FROM_HEADER)); }
        Set<String> scopes;
        String scopesStr = scopesValues.getFirst();
        try {
            scopes = Config.getInstance().getMapper().readValue(scopesStr, new TypeReference<Set<String>>(){});
        } catch (IOException e) {
            return Failure.of(new Status(UNABLE_PARSE_SCOPES_FROM_HEADER));
        }
        if(scopes != null) {
            return getJwt(scopes);
        }
        return Failure.of(new Status(UNABLE_PARSE_SCOPES_FROM_HEADER));
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
