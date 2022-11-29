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

import com.networknt.config.Config;
import com.networknt.utility.Constants;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.jose4j.lang.JoseException;
import org.junit.Assert;
import org.junit.Test;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JwtVerifierTest {
    static final String CONFIG_NAME = "security-509";
    static final String CONFIG_NAME_OPENAPI = "openapi-security-no-default-jwtcertificate";

    static final String CONFIG_RELAXED_VERIFICATION = "security-relaxedVerification";
    @Test
    public void testReadCertificate() {
        SecurityConfig config = SecurityConfig.load(CONFIG_NAME);
        Map<String, Object> keyMap = config.getCertificate();
        Map<String, X509Certificate> certMap = new HashMap<>();
        JwtVerifier jwtVerifier = new JwtVerifier(config);
        for(String kid: keyMap.keySet()) {
            X509Certificate cert = null;
            try {
                cert = jwtVerifier.readCertificate((String)keyMap.get(kid));
            } catch (Exception e) {
                e.printStackTrace();
            }
            certMap.put(kid, cert);
        }
        Assert.assertEquals(2, certMap.size());
    }

    @Test
    public void testReadCertificate2() {
        SecurityConfig config = SecurityConfig.load(CONFIG_NAME_OPENAPI);
        Map<String, X509Certificate> certMap = new HashMap<>();
        if (config.getCertificate()!=null) {
            Map<String, Object> keyMap = config.getCertificate();
            JwtVerifier jwtVerifier = new JwtVerifier(config);
            for(String kid: keyMap.keySet()) {
                X509Certificate cert = null;
                try {
                    cert = jwtVerifier.readCertificate((String)keyMap.get(kid));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                certMap.put(kid, cert);
            }
        }

        Assert.assertEquals(0, certMap.size());
    }

    @Test
    public void testVerifyJwtByJsonWebKeys() throws Exception {
        JwtConfig jwtConfig = (JwtConfig) Config.getInstance().getJsonObjectConfig(JwtIssuer.JWT_CONFIG, JwtConfig.class);

        String fileName = jwtConfig.getKey().getFilename();
        String alias = jwtConfig.getKey().getKeyName();

        KeyStore ks = loadKeystore(fileName, jwtConfig.getKey().getPassword());
        Key privateKey = ks.getKey(alias, jwtConfig.getKey().getPassword().toCharArray());

        JsonWebSignature jws = new JsonWebSignature();

        String iss = "my.test.iss";
        JwtClaims jwtClaims = JwtClaims.parse("{\n" +
                "  \"sub\": \"5745ed4b-0158-45ff-89af-4ce99bc6f4de\",\n" +
                "  \"iss\": \"" + iss  +"\",\n" +
                "  \"subject_type\": \"client-id\",\n" +
                "  \"exp\": 1557419531,\n" +
                "  \"iat\": 1557419231,\n" +
                "  \"scope\": [\n" +
                "    \"my.test.scope.read\",\n" +
                "    \"my.test.scope.write\",\n" +
                "  ],\n" +
                "  \"consumer_application_id\": \"389\",\n" +
                "  \"request_transit\": \"63092\"\n" +
                "}");

        // The payload of the JWS is JSON content of the JWT Claims
        jws.setPayload(jwtClaims.toJson());

        // use private key to sign the JWT
        jws.setKey(privateKey);

        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        String jwt = jws.getCompactSerialization();

        Assert.assertNotNull(jwt);

        System.out.print("JWT = " + jwt);

        JwtVerifier jwtVerifier = new JwtVerifier(SecurityConfig.load(CONFIG_NAME));
        JwtClaims claims = jwtVerifier.verifyJwt(jwt, true, true, null, (kId, requestPath) -> {
            try {
                // use public key to create the the JsonWebKey
                Key publicKey = ks.getCertificate(alias).getPublicKey();
                PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk(publicKey);
                List<JsonWebKey> jwkList = Arrays.asList(jwk);
                return new JwksVerificationKeyResolver(jwkList);
            } catch (JoseException | KeyStoreException e) {
                throw new RuntimeException(e);
            }
        });

        Assert.assertNotNull(claims);
        Assert.assertEquals(iss, claims.getStringClaimValue("iss"));
    }

    @Test
    public void testGenerateJsonWebKeys() throws Exception {
        JwtConfig jwtConfig = (JwtConfig) Config.getInstance().getJsonObjectConfig(JwtIssuer.JWT_CONFIG, JwtConfig.class);

        String fileName = jwtConfig.getKey().getFilename();
        String alias = jwtConfig.getKey().getKeyName();

        KeyStore ks = loadKeystore(fileName, jwtConfig.getKey().getPassword());
        Key privateKey = ks.getKey(alias, jwtConfig.getKey().getPassword().toCharArray());
        Key publicKey = ks.getCertificate(alias).getPublicKey();
        PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk(publicKey);
        jwk.setKeyId("111");
        List<JsonWebKey> jwkList = Arrays.asList(jwk);
        JsonWebKeySet jwks = new JsonWebKeySet(jwkList);
        String jwksJson = jwks.toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY);
        System.out.println(jwksJson);
    }

    private static KeyStore loadKeystore(String fileName, String keyStorePass) throws Exception {
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] passwd = keyStorePass.toCharArray();
        keystore.load(Config.getInstance().getInputStreamFromFile(fileName), passwd);
        return keystore;
    }

    @Test
    public void testVerifyJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("write:pets", "read:pets"), "user");
        String jwt = JwtIssuer.getJwt(claims);
        claims = null;
        Assert.assertNotNull(jwt);
        JwtVerifier jwtVerifier = new JwtVerifier(SecurityConfig.load(CONFIG_NAME));
        try {
            claims = jwtVerifier.verifyJwt(jwt, false, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(claims);
        Assert.assertEquals("steve", claims.getStringClaimValue(Constants.USER_ID_STRING));

        try {
            claims = jwtVerifier.verifyJwt(jwt, false, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("jwtClaims = " + claims);
    }

    @Test
    public void testVerifySign() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("write:pets", "read:pets"), "user");
        String jwt = JwtIssuer.getJwt(claims);
        claims = null;
        Assert.assertNotNull(jwt);
        JwtVerifier jwtVerifier = new JwtVerifier(SecurityConfig.load(CONFIG_NAME));
        try {
            claims = jwtVerifier.verifyJwt(jwt, false, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(claims);
        Assert.assertEquals("steve", claims.getStringClaimValue(Constants.USER_ID_STRING));

        try {
            claims = jwtVerifier.verifyJwt(jwt, false, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("jwtClaims = " + claims);
    }

    @Test
    public void testVerifyToken() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("write:pets", "read:pets"), "user");
        String jwt = JwtIssuer.getJwt(claims);
        claims = null;
        Assert.assertNotNull(jwt);
        JwtVerifier jwtVerifier = new JwtVerifier(SecurityConfig.load(CONFIG_NAME));
        try {
            claims = jwtVerifier.verifyJwt(jwt, false, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(claims);
        Assert.assertEquals("steve", claims.getStringClaimValue(Constants.USER_ID_STRING));

        try {
            claims = jwtVerifier.verifyJwt(jwt, false, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("jwtClaims = " + claims);
    }
    
    @Test
    public void testRelaxedKeyValidation() throws Exception {
        String jwt = "eyJraWQiOiJ7XCJwcm92aWRlcl90eXBlXCI6XCJkYlwiLFwiYWxpYXNcIjpcImtleXRlc3RcIixcInR5cGVcIjpcImxvY2FsXCIsXCJ2ZXJzaW9uXCI6XCIxXCJ9IiwiYWxnIjoiUlMyNTYifQ" +
                "." +
                "eyJzdWIiOiJDT05TVU1FUlxcMTYwMTIzMDIxNjUzIiwicGFydHlJZCI6IjMxNzMyNDk3IiwiZXhwIjoxNjA2NDk5NjgzLCJpYXQiOjE2MDY0OTg3ODMsImp0aSI6IjAzYjA5YmY3LTFlMzktNDNlOC05NDAwLWQzYTcxMDkwYjMxNCJ9" +
                "." +
                "ob9aglgNHcdr2wYXnhwL_P-m76MCd2tZyRJWl9GIhRaZkR_FYoQTXQKz3WINGwn4aynzmkqZx28HlhKttA-A4WQNwEgETFOoq1tCrZ9ZQOrxaexccYdhLCuqzNllDD8OfXm-vFLp52-UpLvVIr3ySPvv9d034IhSw38EkZmV0Vc";

        JwtClaims claims = null;

        /* config points to our test pub cert for this JWT */
        JwtVerifier jwtVerifier = new JwtVerifier(SecurityConfig.load(CONFIG_RELAXED_VERIFICATION));

        Assert.assertTrue(jwtVerifier.enableRelaxedKeyValidation);

        try {
            claims = jwtVerifier.verifyJwt(jwt, true, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* assert that our claims are not null, and that we can find the claim 'sub' */
        Assert.assertNotNull(claims);
        Assert.assertEquals("CONSUMER\\160123021653", claims.getStringClaimValue("sub"));
        System.out.println("jwt = " + jwt);
        System.out.println("jwtClaims = " + claims);
    }


}
