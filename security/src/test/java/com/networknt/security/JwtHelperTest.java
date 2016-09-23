/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

import com.networknt.config.Config;
import com.networknt.utility.Constants;
import org.jose4j.jwt.JwtClaims;
import org.junit.Assert;
import org.junit.Test;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by steve on 01/09/16.
 */
public class JwtHelperTest {
    @Test
    public void testReadCertificate() {
        Map<String, Object> securityConfig = (Map<String, Object>) Config.getInstance().getJsonMapConfig(JwtHelper.SECURITY_CONFIG);
        Map<String, Object> jwtConfig = (Map<String, Object>)securityConfig.get(JwtHelper.JWT_CONFIG);
        Map<String, Object> keyMap = (Map<String, Object>) jwtConfig.get(JwtHelper.JWT_CERTIFICATE);
        Map<String, X509Certificate> certMap = new HashMap<String, X509Certificate>();
        for(String kid: keyMap.keySet()) {
            X509Certificate cert = null;
            try {
                cert = JwtHelper.readCertificate((String)keyMap.get(kid));
            } catch (Exception e) {
                e.printStackTrace();
            }
            certMap.put(kid, cert);
        }
        Assert.assertEquals(2, certMap.size());
    }

    @Test
    public void longLivedJwt() throws Exception {
        JwtClaims claims = getTestClaims();
        claims.setExpirationTimeMinutesInTheFuture(5256000);
        String jwt = JwtHelper.getJwt(claims);
        System.out.println("***LongLived JWT***: " + jwt);
    }

    private JwtClaims getTestClaims() {
        JwtClaims claims = JwtHelper.getDefaultJwtClaims();
        claims.setClaim("user_id", "steve");
        claims.setClaim("user_type", "EMPLOYEE");
        claims.setClaim("client_id", "aaaaaaaa-1234-1234-1234-bbbbbbbb");
        List<String> scope = Arrays.asList("api.r", "api.w");
        claims.setStringListClaim("scope", scope); // multi-valued claims work too and will end up as a JSON array
        return claims;
    }

    @Test
    public void testVerifyJwt() throws Exception {
        JwtClaims claims = getTestClaims();
        String jwt = JwtHelper.getJwt(claims);
        claims = null;
        Assert.assertNotNull(jwt);
        try {
            claims = JwtHelper.verifyJwt(jwt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(claims);
        Assert.assertEquals("steve", claims.getStringClaimValue(Constants.USER_ID));
        System.out.println("jwtClaims = " + claims);
    }

}
