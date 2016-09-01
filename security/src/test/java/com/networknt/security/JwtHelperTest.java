package com.networknt.security;

import com.networknt.config.Config;
import com.networknt.utility.Constants;
import org.jose4j.jwt.JwtClaims;
import org.junit.Assert;
import org.junit.Test;

import java.security.cert.X509Certificate;
import java.util.Arrays;
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
        List<String> files = (List<String>) jwtConfig.get(JwtHelper.JWT_CERTIFICATE);
        String primaryCert = files.get(0);
        Assert.assertEquals("oauth/primary.crt", primaryCert);
        X509Certificate cert = null;
        try {
            cert = JwtHelper.readCertificate(primaryCert);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(cert);
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
        claims.setClaim("client_id", "ddcaf0ba-1131-2232-3313-d6f2753f25dc");
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
