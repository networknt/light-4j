package com.networknt.client.oauth.cache;

import com.networknt.client.oauth.Jwt;

public interface ICacheStrategy {
    void cacheJwt(Jwt.Key key, Jwt result);

    Jwt getCachedJwt(Jwt.Key key);
}
