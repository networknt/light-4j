package com.networknt.security;

import com.networknt.utility.HashUtil;
import com.networknt.utility.UuidUtil;
import org.junit.Test;

import java.security.KeyPair;

public class KeyUtilTest {
    @Test
    public void testSerializePublicKey() throws Exception {
        KeyPair keyPair = KeyUtil.generateKeyPair("RSA", 2048);
        System.out.println("public key = " + KeyUtil.serializePublicKey(keyPair.getPublic()));
        System.out.println("private key = " + KeyUtil.serializePrivateKey(keyPair.getPrivate()));
    }

    @Test
    public void testDeserializeKey() throws Exception {
        KeyPair keyPair = KeyUtil.generateKeyPair("RSA", 2048);
        String publicKey = KeyUtil.serializePublicKey(keyPair.getPublic());
        String privateKey = KeyUtil.serializePrivateKey(keyPair.getPrivate());
        System.out.println("public key = " + publicKey);
        System.out.println("private key = " + privateKey);
        KeyUtil.deserializePrivateKey(privateKey, "RSA");
        KeyUtil.deserializePublicKey(publicKey, "RSA");
    }

    /**
     * This is the method to generate a JWK and keys for the database population per environment.
     *
     * @throws Exception exception
     */
    @Test
    public void testGenerateJwk() throws Exception {
        KeyPair longKeyPair = KeyUtil.generateKeyPair("RSA", 2048);
        String longKeyId = UuidUtil.uuidToBase64(UuidUtil.getUUID());
        System.out.println("longKeyId = " + longKeyId);
        String publicKey = KeyUtil.serializePublicKey(longKeyPair.getPublic());
        System.out.println("long public key = " + publicKey);
        String privateKey = KeyUtil.serializePrivateKey(longKeyPair.getPrivate());
        System.out.println("long private key = " + privateKey);

        KeyPair currKeyPair = KeyUtil.generateKeyPair("RSA", 2048);
        String currKeyId = UuidUtil.uuidToBase64(UuidUtil.getUUID());
        System.out.println("currKeyId = " + currKeyId);
        publicKey = KeyUtil.serializePublicKey(currKeyPair.getPublic());
        System.out.println("curr public key = " + publicKey);
        privateKey = KeyUtil.serializePrivateKey(currKeyPair.getPrivate());
        System.out.println("curr private key = " + privateKey);

        String jwk = KeyUtil.generateJwk(longKeyPair.getPublic(), longKeyId, currKeyPair.getPublic(), currKeyId, null, null);
        System.out.println("jwk = " + jwk);
    }
}
