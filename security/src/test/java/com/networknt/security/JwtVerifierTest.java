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
import java.util.*;

public class JwtVerifierTest extends JwtVerifierJwkBase {
    static final String CONFIG_NAME = "security";
    @Test
    public void testVerifyJwtByJsonWebKeys() throws Exception {
        Key privateKey = KeyUtil.deserializePrivateKey(curr_key, KeyUtil.RSA);

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
        jws.setKeyIdHeaderValue(curr_kid);

        String jwt = jws.getCompactSerialization();

        Assert.assertNotNull(jwt);

        System.out.print("JWT = " + jwt);

        JwtVerifier jwtVerifier = new JwtVerifier(SecurityConfig.load(CONFIG_NAME));
        JwtClaims claims = jwtVerifier.verifyJwt(jwt, true, true, null, null, null, (kId, requestPath) -> {
            try {
                // use public key to create the JsonWebKey
                Key publicKey = KeyUtil.deserializePublicKey(curr_pub, KeyUtil.RSA);
                PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk(publicKey);
                jwk.setKeyId(curr_kid);
                List<JsonWebKey> jwkList = Arrays.asList(jwk);
                return new JwksVerificationKeyResolver(jwkList);
            } catch (JoseException | KeyStoreException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });

        Assert.assertNotNull(claims);
        Assert.assertEquals(iss, claims.getStringClaimValue("iss"));
    }

    @Test
    public void testGenerateJsonWebKeys() throws Exception {
        Key publicKey = KeyUtil.deserializePublicKey(curr_pub, KeyUtil.RSA);
        PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk(publicKey);
        jwk.setKeyId("111");
        List<JsonWebKey> jwkList = Arrays.asList(jwk);
        JsonWebKeySet jwks = new JsonWebKeySet(jwkList);
        String jwksJson = jwks.toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY);
        System.out.println(jwksJson);
    }

    @Test
    public void testStringList() {
        List<String> ids = new ArrayList<>();
        ids.add("abc");
        ids.add("xyz");
        System.out.println("ids = " + ids);
    }
}
