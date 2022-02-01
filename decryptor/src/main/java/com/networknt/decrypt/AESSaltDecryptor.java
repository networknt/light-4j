package com.networknt.decrypt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This implementation is replaced by AESSaltDecryptor with dynamic salt instead of static one.
 * It allows different application to use different salt so that it is harder to any attacker
 * to perform dictionary attack.
 */
public class AESSaltDecryptor implements Decryptor {
    private static final Logger logger = LoggerFactory.getLogger(AESSaltDecryptor.class);

    private static final int ITERATIONS = 65536;
    private static final int KEY_SIZE = 256;
    private static final String STRING_ENCODING = "UTF-8";
    private static final byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    // cache the secret to void recreating instances for each decrypt call as all config files
    // will use the same salt per application.
    private Map<String, SecretKeySpec> secretMap = new ConcurrentHashMap<>();
    private Cipher cipher;
    IvParameterSpec ivSpec;

    public AESSaltDecryptor() {
        try {
            // CBC = Cipher Block chaining
            // PKCS5Padding Indicates that the keys are padded
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            ivSpec = new IvParameterSpec(iv);
        } catch (Exception e) {
            logger.error("Failed to get the Cipher instance:", e);
            throw new RuntimeException("Unable to initialize", e);
        }
    }

    @Override
    public String decrypt(String input) {
        if (!input.startsWith(CRYPT_PREFIX)) {
            logger.error("The secret text is not started with prefix " + CRYPT_PREFIX);
            throw new RuntimeException("Unable to decrypt, input string does not start with 'CRYPT'.");
        }
        String[] parts = input.split(":");
        // need to make sure that the salt is in the secret text.
        if(parts.length != 3) {
            logger.error("The secret text is not formatted correctly with CRYPT:salt:hash");
            throw new RuntimeException("Unable to decrypt, input string is not formatted correctly with CRYPT:salt:hash");
        }

        try {
            byte[] salt = fromHex(parts[1]);
            byte[] hash = fromHex(parts[2]);
            // try to get the secret from the cache first.
            SecretKeySpec secret = secretMap.get(parts[1]);
            if(secret == null) {
                KeySpec spec = new PBEKeySpec(getPassword(), salt, ITERATIONS, KEY_SIZE);
                /* Derive the key, given password and salt. */
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                SecretKey tmp = factory.generateSecret(spec);
                secret = new SecretKeySpec(tmp.getEncoded(), "AES");
                secretMap.put(parts[1], secret);
            }
            cipher.init(Cipher.DECRYPT_MODE, secret, ivSpec);
            return new String(cipher.doFinal(hash), STRING_ENCODING);
        } catch (Exception e) {
            throw new RuntimeException("Unable to decrypt because the decrypted password is incorrect.", e);
        }
    }

    protected char[] getPassword() {
        return "light".toCharArray();
    }

    private static byte[] fromHex(String hex) throws NoSuchAlgorithmException
    {
        byte[] bytes = new byte[hex.length() / 2];
        for(int i = 0; i < bytes.length ;i++)
        {
            bytes[i] = (byte)Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }
}
