package com.networknt.decrypt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This implementation is replaced by AESSaltDecryptor with dynamic salt instead of static one.
 * It allows different application to use different salt so that it is harder to any attacker
 * to perform dictionary attack.
 */
public class AESSaltDecryptor implements Decryptor {
    private static final Logger logger = LoggerFactory.getLogger(AESSaltDecryptor.class);

    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String LEGACY_CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String GCM_CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int ITERATIONS = 65536;
    private static final int KEY_SIZE = 256;
    private static final int GCM_TAG_LENGTH = 128;
    private static final byte[] LEGACY_IV = new byte[16];
    private static final AtomicBoolean LEGACY_WARNING_LOGGED = new AtomicBoolean(false);

    // cache the secret to void recreating instances for each decrypt call as all config files
    // will use the same salt per application.
    private final Map<String, SecretKeySpec> secretMap = new ConcurrentHashMap<>();

    /**
     * Default constructor for AESSaltDecryptor.
     */
    public AESSaltDecryptor() {
    }

    @Override
    public String decrypt(String input) {
        if (input == null || !input.startsWith(CRYPT_PREFIX + ":")) {
            logger.error("The secret text is null or does not start with prefix {}:", CRYPT_PREFIX);
            throw new IllegalArgumentException("Unable to decrypt, input string does not start with 'CRYPT:'.");
        }

        String[] parts = input.split(":", -1);
        try {
            return switch (parts.length) {
                case 3 -> decryptLegacyCbc(parts);
                case 4 -> decryptGcm(parts);
                default -> throw invalidCryptFormat(parts.length);
            };
        } catch (GeneralSecurityException e) {
            logger.error("Unable to decrypt CRYPT value. The master password may be incorrect or the ciphertext may be corrupted.");
            throw new RuntimeException("Unable to decrypt because the decrypted password is incorrect.", e);
        }
    }

    private String decryptLegacyCbc(String[] parts) throws GeneralSecurityException {
        String saltHex = requireHexPart(parts, 1, "salt");
        String hashHex = requireHexPart(parts, 2, "hash");
        SecretKeySpec secret = getSecret(saltHex, fromHex(saltHex, "salt"));
        Cipher cipher = Cipher.getInstance(LEGACY_CIPHER_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(LEGACY_IV));
        String decrypted = new String(cipher.doFinal(fromHex(hashHex, "hash")), StandardCharsets.UTF_8);
        if (LEGACY_WARNING_LOGGED.compareAndSet(false, true)) {
            logger.warn("Legacy three-part CRYPT value decrypted. Regenerate this secret with the current light-encryptor to migrate to AES-GCM.");
        }
        return decrypted;
    }

    private String decryptGcm(String[] parts) throws GeneralSecurityException {
        String saltHex = requireHexPart(parts, 1, "salt");
        String ivHex = requireHexPart(parts, 2, "iv");
        String hashHex = requireHexPart(parts, 3, "hash");
        SecretKeySpec secret = getSecret(saltHex, fromHex(saltHex, "salt"));
        Cipher cipher = Cipher.getInstance(GCM_CIPHER_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secret, new GCMParameterSpec(GCM_TAG_LENGTH, fromHex(ivHex, "iv")));
        return new String(cipher.doFinal(fromHex(hashHex, "hash")), StandardCharsets.UTF_8);
    }

    private SecretKeySpec getSecret(String saltHex, byte[] salt) throws GeneralSecurityException {
        String cacheKey = KDF_ALGORITHM + ":" + ITERATIONS + ":" + KEY_SIZE + ":" + saltHex;
        SecretKeySpec secret = secretMap.get(cacheKey);
        if(secret == null) {
            char[] password = getPassword();
            validatePassword(password);
            KeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_SIZE);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec newSecret = new SecretKeySpec(tmp.getEncoded(), "AES");
            SecretKeySpec cachedSecret = secretMap.putIfAbsent(cacheKey, newSecret);
            secret = cachedSecret == null ? newSecret : cachedSecret;
        }
        return secret;
    }

    private static void validatePassword(char[] password) {
        if(password == null || password.length == 0 || isBlank(password)) {
            logger.error("The configuration decryption password is empty.");
            throw new IllegalStateException("Unable to decrypt, configuration decryption password is empty.");
        }
    }

    private static boolean isBlank(char[] password) {
        for(char ch : password) {
            if(!Character.isWhitespace(ch)) {
                return false;
            }
        }
        return true;
    }

    private static String requireHexPart(String[] parts, int index, String name) {
        if(parts[index] == null || parts[index].isEmpty()) {
            logger.error("The secret text is not formatted correctly. The {} field is empty.", name);
            throw new IllegalArgumentException("Unable to decrypt, input string is not formatted correctly with CRYPT:salt:hash or CRYPT:salt:iv:hash.");
        }
        return parts[index];
    }

    private static IllegalArgumentException invalidCryptFormat(int partCount) {
        logger.error("The secret text is not formatted correctly. Expected 3 or 4 parts, got {}.", partCount);
        return new IllegalArgumentException("Unable to decrypt, input string is not formatted correctly with CRYPT:salt:hash or CRYPT:salt:iv:hash.");
    }

    /**
     * Gets the password for decryption.
     *
     * @return char array of the password
     */
    protected char[] getPassword() {
        return "light".toCharArray();
    }

    private static byte[] fromHex(String hex, String name) {
        if((hex.length() & 1) != 0) {
            logger.error("The secret text is not formatted correctly. The {} field is not valid hex.", name);
            throw new IllegalArgumentException("Unable to decrypt, " + name + " is not valid hex.");
        }
        byte[] bytes = new byte[hex.length() / 2];
        try {
            for(int i = 0; i < bytes.length ;i++)
            {
                bytes[i] = (byte)Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
            }
        } catch (NumberFormatException e) {
            logger.error("The secret text is not formatted correctly. The {} field is not valid hex.", name);
            throw new IllegalArgumentException("Unable to decrypt, " + name + " is not valid hex.", e);
        }
        return bytes;
    }
}
