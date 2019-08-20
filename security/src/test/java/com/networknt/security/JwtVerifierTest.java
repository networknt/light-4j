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
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.jose4j.lang.JoseException;
import org.junit.Assert;
import org.junit.Ignore;
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
    static final String CONFIG_NAME = "security";
    @Test
    public void testReadCertificate() {
        Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
        Map<String, Object> jwtConfig = (Map<String, Object>)config.get(JwtIssuer.JWT_CONFIG);
        Map<String, Object> keyMap = (Map<String, Object>) jwtConfig.get(JwtVerifier.JWT_CERTIFICATE);
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
    public void testVerifyJwt() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("write:pets", "read:pets"));
        String jwt = JwtIssuer.getJwt(claims);
        claims = null;
        Assert.assertNotNull(jwt);
        JwtVerifier jwtVerifier = new JwtVerifier(Config.getInstance().getJsonMapConfig(CONFIG_NAME));
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
    public void testVerifyJwtByJsonWebKeys() throws Exception {
        Map<String, Object> secretConfig = Config.getInstance().getJsonMapConfig(JwtIssuer.SECRET_CONFIG);
        JwtConfig jwtConfig = (JwtConfig) Config.getInstance().getJsonObjectConfig(JwtIssuer.JWT_CONFIG, JwtConfig.class);

        String fileName = jwtConfig.getKey().getFilename();
        String alias = jwtConfig.getKey().getKeyName();

        KeyStore ks = loadKeystore(fileName, (String)secretConfig.get(JwtIssuer.JWT_PRIVATE_KEY_PASSWORD));
        Key privateKey = ks.getKey(alias, ((String) secretConfig.get(JwtIssuer.JWT_PRIVATE_KEY_PASSWORD)).toCharArray());

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

        JwtVerifier jwtVerifier = new JwtVerifier(Config.getInstance().getJsonMapConfig(CONFIG_NAME));
        JwtClaims claims = jwtVerifier.verifyJwt(jwt, true, true, (kId, isToken) -> {
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

    private static KeyStore loadKeystore(String fileName, String keyStorePass) throws Exception {
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] passwd = keyStorePass.toCharArray();
        keystore.load(Config.getInstance().getInputStreamFromFile(fileName), passwd);
        return keystore;
    }

    @Test
    public void testVerifyToken() throws Exception {
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("write:pets", "read:pets"));
        String jwt = JwtIssuer.getJwt(claims);
        claims = null;
        Assert.assertNotNull(jwt);
        JwtVerifier jwtVerifier = new JwtVerifier(Config.getInstance().getJsonMapConfig(CONFIG_NAME));
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
        JwtClaims claims = ClaimsUtil.getTestClaims("steve", "EMPLOYEE", "f7d42348-c647-4efb-a52d-4c5787421e72", Arrays.asList("write:pets", "read:pets"));
        String jwt = JwtIssuer.getJwt(claims);
        claims = null;
        Assert.assertNotNull(jwt);
        JwtVerifier jwtVerifier = new JwtVerifier(Config.getInstance().getJsonMapConfig(CONFIG_NAME));
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

    /**
     * This test needs light-oauth2 service to be up and running in order to test it
     * to start the light-oauth2 please refer to https://networknt.github.io/light-oauth2/tutorials
     */
    @Test
    @Ignore
    public void testGetCertForToken() {
        JwtVerifier jwtVerifier = new JwtVerifier(Config.getInstance().getJsonMapConfig(CONFIG_NAME));
        X509Certificate certificate = jwtVerifier.getCertForToken("100");
        System.out.println("certificate = " + certificate);
        Assert.assertNotNull(certificate);
    }

    /**
     * This test needs light-oauth2 service to be up and running in order to test it
     * to start the light-oauth2 please refer to https://networknt.github.io/light-oauth2/tutorials
     */
    @Test
    @Ignore
    public void testGetCertForSign() {
        JwtVerifier jwtVerifier = new JwtVerifier(Config.getInstance().getJsonMapConfig(CONFIG_NAME));
        X509Certificate certificate = jwtVerifier.getCertForSign("100");
        System.out.println("certificate = " + certificate);
        Assert.assertNotNull(certificate);
    }

}
