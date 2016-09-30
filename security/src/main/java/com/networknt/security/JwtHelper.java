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
import com.networknt.exception.ExpiredTokenException;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.resolvers.X509VerificationKeyResolver;
import org.owasp.encoder.Encode;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by steve on 01/09/16.
 */
public class JwtHelper {
    static final XLogger logger = XLoggerFactory.getXLogger(JwtHelper.class);
    public static final String JWT_CONFIG = "jwt";
    public static final String KEY = "key";
    public static final String FILENAME = "filename";
    public static final String PASSWORD = "password";
    public static final String KEY_NAME = "keyName";
    public static final String KID = "kid";
    public static final String SECURITY_CONFIG = "security";
    public static final String JWT_CERTIFICATE = "certificate";
    public static final String JwT_CLOCK_SKEW_IN_SECONDS = "clockSkewInSeconds";
    public static final String ENABLE_VERIFY_JWT = "enableVerifyJwt";

    static Map<String, X509Certificate> certMap;

    static Map<String, Object> securityConfig = (Map)Config.getInstance().getJsonMapConfig(SECURITY_CONFIG);
    static Map<String, Object> securityJwtConfig = (Map)securityConfig.get(JWT_CONFIG);
    static JwtConfig jwtConfig = (JwtConfig) Config.getInstance().getJsonObjectConfig(JWT_CONFIG, JwtConfig.class);

    public static String getJwt(JwtClaims claims) throws Exception {
        String jwt = null;
        RSAPrivateKey privateKey = (RSAPrivateKey) getPrivateKey(
                jwtConfig.getKey().getFilename(), jwtConfig.getKey().getPassword(), jwtConfig.getKey().getKeyName());

        // A JWT is a JWS and/or a JWE with JSON claims as the payload.
        // In this example it is a JWS nested inside a JWE
        // So we first create a JsonWebSignature object.
        JsonWebSignature jws = new JsonWebSignature();

        // The payload of the JWS is JSON content of the JWT Claims
        jws.setPayload(claims.toJson());

        // The JWT is signed using the sender's private key
        jws.setKey(privateKey);
        jws.setKeyIdHeaderValue(jwtConfig.getKey().getKid());

        // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        // Sign the JWS and produce the compact serialization, which will be the inner JWT/JWS
        // representation, which is a string consisting of three dot ('.') separated
        // base64url-encoded parts in the form Header.Payload.Signature
        jwt = jws.getCompactSerialization();
        return jwt;
    }

    public static JwtClaims getDefaultJwtClaims() {
        JwtConfig config = (JwtConfig) Config.getInstance().getJsonObjectConfig(JWT_CONFIG, JwtConfig.class);

        JwtClaims claims = new JwtClaims();

        claims.setIssuer(config.getIssuer());
        claims.setAudience(config.getAudience());
        claims.setExpirationTimeMinutesInTheFuture(config.getExpiredInMinutes());
        claims.setGeneratedJwtId(); // a unique identifier for the token
        claims.setIssuedAtToNow();  // when the token was issued/created (now)
        claims.setNotBeforeMinutesInThePast(2); // time before which the token is not yet valid (2 minutes ago)
        claims.setClaim("version", config.getVersion());
        return claims;

    }

    private static PrivateKey getPrivateKey(String filename, String password, String key) {
        PrivateKey privateKey = null;

        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(JwtHelper.class.getResourceAsStream(filename),
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


    static public X509Certificate readCertificate(String filename)
            throws Exception {
        InputStream inStream = null;
        X509Certificate cert = null;
        try {
            inStream = Config.getInstance().getInputStreamFromFile(filename);
            if (inStream != null) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                cert = (X509Certificate) cf.generateCertificate(inStream);
            } else {
                logger.info("Certificate " + Encode.forJava(filename) + " not found.");
            }
        } catch (Exception e) {
            logger.error("Exception: ", e);
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException ioe) {
                    logger.error("Exception: ", ioe);
                }
            }
        }
        return cert;
    }

    static {
        certMap = new HashMap<String, X509Certificate>();
        Map<String, Object> keyMap = (Map<String, Object>) securityJwtConfig.get(JwtHelper.JWT_CERTIFICATE);
        for(String kid: keyMap.keySet()) {
            X509Certificate cert = null;
            try {
                cert = JwtHelper.readCertificate((String)keyMap.get(kid));
            } catch (Exception e) {
                logger.error("Exception:", e);
            }
            certMap.put(kid, cert);
        }
    }

    public static String getJwtFromAuthorization(String authorization) {
        String jwt = null;
        if(authorization != null) {
            String[] parts = authorization.split(" ");
            if (parts.length == 2) {
                String scheme = parts[0];
                String credentials = parts[1];
                Pattern pattern = Pattern.compile("^Bearer$", Pattern.CASE_INSENSITIVE);
                if (pattern.matcher(scheme).matches()) {
                    jwt = credentials;
                }
            }
        }
        return jwt;
    }

    public static JwtClaims verifyJwt(String jwt) throws InvalidJwtException, ExpiredTokenException {
        JwtClaims claims = null;
        JwtConsumer consumer = new JwtConsumerBuilder()
                .setSkipAllValidators()
                .setDisableRequireSignature()
                .setSkipSignatureVerification()
                .build();

        JwtContext jwtContext = consumer.process(jwt);
        JwtClaims jwtClaims = jwtContext.getJwtClaims();
        JsonWebStructure structure = jwtContext.getJoseObjects().get(0);
        String kid = structure.getKeyIdHeaderValue();

        int secondsOfAllowedClockSkew = 30;
        try {
            if ((NumericDate.now().getValue() - secondsOfAllowedClockSkew) >= jwtClaims.getExpirationTime().getValue())
            {
                logger.info("jwt token is expired!");
                throw new ExpiredTokenException("Token is expired");
            }
        } catch (MalformedClaimException e) {
            logger.error("MalformedClaimException:", e);
            throw new InvalidJwtException("MalformedClaimException", e);
        }

        X509VerificationKeyResolver x509VerificationKeyResolver = new X509VerificationKeyResolver(certMap.get(kid));
        x509VerificationKeyResolver.setTryAllOnNoThumbHeader(true);
        consumer = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(
                        (Integer) securityJwtConfig.get(JwT_CLOCK_SKEW_IN_SECONDS))
                .setSkipDefaultAudienceValidation()
                .setVerificationKeyResolver(x509VerificationKeyResolver)
                .build();

        // Validate the JWT and process it to the Claims
        jwtContext = consumer.process(jwt);
        claims = jwtContext.getJwtClaims();
        return claims;
    }
}
