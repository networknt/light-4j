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
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Map;

/**
 * JWT token issuer helper utility that use by light-ouath2 token and code services to
 * generate JWT tokens.
 *
 * @author Steve Hu
 */
public class JwtIssuer {
    private static final Logger logger = LoggerFactory.getLogger(JwtIssuer.class);
    private static final JwtConfig jwtConfig = JwtConfig.load();


    /**
     * A static method that generate JWT token from JWT claims object and a given private key. This private key
     * should be from the host_key_t database table.
     *
     * @param claims JwtClaims object
     * @param kid Key id
     * @param privateKey PrivateKey object
     * @return A string represents jwt token
     * @throws JoseException JoseException
     */
    public static String getJwt(JwtClaims claims, String kid, PrivateKey privateKey) throws JoseException {
        String jwt;
        // A JWT is a JWS and/or a JWE with JSON claims as the payload.
        // In this example it is a JWS nested inside a JWE
        // So we first create a JsonWebSignature object.
        JsonWebSignature jws = new JsonWebSignature();

        // The payload of the JWS is JSON content of the JWT Claims
        jws.setPayload(claims.toJson());

        // The JWT is signed using the sender's private key
        jws.setKey(privateKey);
        jws.setKeyIdHeaderValue(kid);

        // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        // Sign the JWS and produce the compact serialization, which will be the inner JWT/JWS
        // representation, which is a string consisting of three dot ('.') separated
        // base64url-encoded parts in the form Header.Payload.Signature
        jwt = jws.getCompactSerialization();
        return jwt;
    }

    /**
     * Construct a default JwtClaims
     *
     * @return JwtClaims
     */
    public static JwtClaims getDefaultJwtClaims() {
        JwtClaims claims = new JwtClaims();

        claims.setIssuer(jwtConfig.getIssuer());
        claims.setAudience(jwtConfig.getAudience());
        claims.setExpirationTimeMinutesInTheFuture(jwtConfig.getExpiredInMinutes());
        claims.setGeneratedJwtId(); // a unique identifier for the token
        claims.setIssuedAtToNow();  // when the token was issued/created (now)
        claims.setNotBeforeMinutesInThePast(2); // time before which the token is not yet valid (2 minutes ago)
        claims.setClaim("ver", jwtConfig.getVersion());
        return claims;

    }

    /**
     * Construct a default JwtClaims
     * @param expiresIn expires in
     * @return JwtClaims
     */
    public static JwtClaims getJwtClaimsWithExpiresIn(int expiresIn) {
        JwtClaims claims = new JwtClaims();

        claims.setIssuer(jwtConfig.getIssuer());
        claims.setAudience(jwtConfig.getAudience());
        claims.setExpirationTimeMinutesInTheFuture(expiresIn/60);
        claims.setGeneratedJwtId(); // a unique identifier for the token
        claims.setIssuedAtToNow();  // when the token was issued/created (now)
        claims.setNotBeforeMinutesInThePast(2); // time before which the token is not yet valid (2 minutes ago)
        claims.setClaim("ver", jwtConfig.getVersion());
        return claims;
    }

    /**
     * Get private key from java key store
     *
     * @param filename Key store file name
     * @param password Key store password
     * @param key key name in keystore
     * @return A PrivateKey object
     */
    private static PrivateKey getPrivateKey(String filename, String password, String key) {
        if(logger.isDebugEnabled()) logger.debug("filename = " + filename + " key = " + key);
        PrivateKey privateKey = null;

        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(Config.getInstance().getInputStreamFromFile(filename),
                    password.toCharArray());

            privateKey = (PrivateKey) keystore.getKey(key,
                    password.toCharArray());
        } catch (Exception e) {
            logger.error("Exception:", e);
        }

        if (privateKey == null) {
            logger.error("Failed to retrieve private key from keystore");
        }

        return privateKey;
    }

}
