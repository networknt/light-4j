package com.networknt.security;

import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * This is a utility class that is used to serialize and deserialize keys and jwks.
 *
 * @author Steve Hu
 */
public class KeyUtil {
    public  static final Logger logger = LoggerFactory.getLogger(KeyUtil.class);
    public static KeyPair generateKeyPair(String algorithm, int keySize) throws Exception {
        // Generate a key pair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
        keyPairGenerator.initialize(keySize);
        return keyPairGenerator.generateKeyPair();
    }
    public static String serializePublicKey(PublicKey publicKey) throws Exception {
        // Serialize the public key
        byte[] publicKeyBytes = publicKey.getEncoded();
        return Base64.getEncoder().encodeToString(publicKeyBytes);
    }

    public static String serializePrivateKey(PrivateKey privateKey) throws Exception {
        // Serialize the private key
        byte[] privateKeyBytes = privateKey.getEncoded();
        return Base64.getEncoder().encodeToString(privateKeyBytes);
    }

    public static PrivateKey deserializePrivateKey(String serializedPrivateKey, String algorithm) throws Exception {
        // Deserialize the private key
        byte[] decodedPrivateKeyBytes = Base64.getDecoder().decode(serializedPrivateKey);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decodedPrivateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        return keyFactory.generatePrivate(privateKeySpec);
    }

    public static PublicKey deserializePublicKey(String serializedPublicKey, String algorithm) throws Exception {
        // Deserialize the public key
        byte[] decodedPublicKeyBytes = Base64.getDecoder().decode(serializedPublicKey);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decodedPublicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        return keyFactory.generatePublic(publicKeySpec);
    }

    public static String generateJwk(PublicKey longKey, String longKeyId, PublicKey currKey, String currKeyId, PublicKey prevKey, String prevKeyId) {
        List<JsonWebKey> jwkList = new ArrayList<>();
        try {
            // Create a JWK object from the long live public key
            PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk(longKey);
            jwk.setKeyId(longKeyId);
            jwkList.add(jwk);
            // Create a JWK object from the current public key
            jwk = PublicJsonWebKey.Factory.newPublicJwk(currKey);
            jwk.setKeyId(currKeyId);
            jwkList.add(jwk);
            // Create a JWK object from the previous public key
            if(prevKey != null && prevKeyId != null) {
                jwk = PublicJsonWebKey.Factory.newPublicJwk(prevKey);
                jwk.setKeyId(prevKeyId);
                jwkList.add(jwk);
            }
        } catch (JoseException e) {
            logger.error("Exception:", e);
        }
        // create a JsonWebKeySet object with the list of JWK objects
        JsonWebKeySet jwks = new JsonWebKeySet(jwkList);
        // and output the JSON of the JWKS
        return jwks.toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY);
    }
}
