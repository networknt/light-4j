/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
