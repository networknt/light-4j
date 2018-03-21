package com.networknt.security;

import org.jose4j.jwt.JwtClaims;

import java.util.List;
import java.util.Map;

public class ClaimsUtil {
    public static JwtClaims getTestClaims(String userId, String userType, String clientId, List<String> scope) {
        JwtClaims claims = JwtIssuer.getDefaultJwtClaims();
        claims.setClaim("user_id", userId);
        claims.setClaim("user_type", userType);
        claims.setClaim("client_id", clientId);
        if(scope != null) claims.setStringListClaim("scope", scope); // multi-valued claims work too and will end up as a JSON array
        return claims;
    }

    public static JwtClaims getCustomClaims(String userId, String userType, String clientId, List<String> scope, Map<String, String> custom) {
        JwtClaims claims = JwtIssuer.getDefaultJwtClaims();
        claims.setClaim("user_id", userId);
        claims.setClaim("user_type", userType);
        claims.setClaim("client_id", clientId);
        custom.forEach((k, v) -> claims.setClaim(k, v));
        if(scope != null) claims.setStringListClaim("scope", scope); // multi-valued claims work too and will end up as a JSON array
        return claims;
    }


}
