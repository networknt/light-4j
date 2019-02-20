package com.networknt.client.oauth;

import com.networknt.monad.Result;
import io.undertow.client.ClientRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class TokenManager {

    private TokenManager INSTANCE;
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
        return null;
    }

    public Result<Jwt> getJwt(Jwt jwt) {
        return null;
    }

    public Result<Jwt> getJwt(Set<String> scopes) {
        return null;
    }

}
