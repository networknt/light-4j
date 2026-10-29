package com.networknt.decrypt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * Decrypts authenticated four-part CRYPT values produced by light-encryptor.
 * Legacy three-part CBC values must be re-encrypted before upgrading.
 */
public class AESSaltDecryptor implements Decryptor {
    private static final Logger logger = LoggerFactory.getLogger(AESSaltDecryptor.class);

    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String GCM_CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int ITERATIONS = 65536;
    private static final int KEY_SIZE = 256;
    private static final int GCM_TAG_LENGTH = 128;
    // Exists only during password acquisition/derivation on the current thread.
    // Providers register fresh arrays at creation; overridden getPassword methods
    // may return borrowed arrays, which must never be inferred to be owned.
    private final ThreadLocal<List<char[]>> ownedPasswords = new ThreadLocal<>();
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
                case 3 -> throw new IllegalArgumentException(
                        "Legacy three-part CRYPT values are no longer supported. Re-encrypt with light-encryptor using AES-GCM before upgrading.");
                case 4 -> decryptGcm(parts);
                default -> throw invalidCryptFormat(parts.length);
            };
        } catch (GeneralSecurityException e) {
            logger.error("Unable to decrypt CRYPT value. The master password may be incorrect or the ciphertext may be corrupted.");
            throw new RuntimeException("Unable to decrypt because the decrypted password is incorrect.", e);
        }
    }

    private String decryptGcm(String[] parts) throws GeneralSecurityException {
        String saltHex = requireHexPart(parts, 1, "salt");
        String ivHex = requireHexPart(parts, 2, "iv");
        String hashHex = requireHexPart(parts, 3, "hash");
        if (saltHex.length() != 32 || ivHex.length() != 24) {
            throw new IllegalArgumentException("CRYPT AES-GCM requires a 16-byte salt and a 12-byte nonce.");
        }
        byte[] salt = fromHex(saltHex, "salt");
        byte[] nonce = fromHex(ivHex, "iv");
        byte[] ciphertext = fromHex(hashHex, "hash");
        if (ciphertext.length < GCM_TAG_LENGTH / 8) {
            throw new IllegalArgumentException("CRYPT AES-GCM ciphertext must contain an authentication tag.");
        }
        SecretKeySpec secret = getSecret(salt);
        Cipher cipher = Cipher.getInstance(GCM_CIPHER_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secret, new GCMParameterSpec(GCM_TAG_LENGTH, nonce));
        byte[] plaintext = cipher.doFinal(ciphertext);
        try {
            authenticatedKey(salt, secret);
            return new String(plaintext, StandardCharsets.UTF_8);
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    /** Called only after successful GCM authentication; the base provider retains no key. */
    protected void authenticatedKey(byte[] salt, SecretKeySpec secret) {
        // No cache in the base provider; the interactive manual provider overrides this hook.
    }

    protected SecretKeySpec getSecret(byte[] salt) throws GeneralSecurityException {
        List<char[]> previous = ownedPasswords.get();
        List<char[]> owned = new ArrayList<>();
        ownedPasswords.set(owned);
        char[] password = null;
        PBEKeySpec spec = null;
        byte[] encodedKey = null;
        try {
            password = getPassword();
            validatePassword(password);
            // PBEKeySpec copies the password; never clear a custom provider's borrowed array.
            spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_SIZE);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
            SecretKey key = factory.generateSecret(spec);
            encodedKey = key.getEncoded();
            return new SecretKeySpec(encodedKey, "AES");
        } finally {
            if (spec != null) spec.clearPassword();
            if (encodedKey != null) Arrays.fill(encodedKey, (byte) 0);
            try {
                releasePassword(password);
            } finally {
                for (char[] value : owned) Arrays.fill(value, '\0');
                if (previous == null) ownedPasswords.remove();
                else ownedPasswords.set(previous);
            }
        }
    }

    /**
     * Registers a newly allocated provider password for this derivation's cleanup.
     * Outside a derivation the returned array belongs to the caller. Never register
     * a borrowed configuration array.
     *
     * @param password freshly allocated password array
     * @return the same array
     */
    protected final char[] ownedPassword(char[] password) {
        List<char[]> owned = ownedPasswords.get();
        if (owned != null && password != null) owned.add(password);
        return password;
    }

    /**
     * Releases a password returned by getPassword. Custom providers retain ownership
     * by default; providers returning fresh arrays can override this to clear them.
     *
     * @param password password obtained for this operation, possibly null
     */
    protected void releasePassword(char[] password) {
        // A custom getPassword implementation may return shared configuration.
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
            throw new IllegalArgumentException("Unable to decrypt, input string is not formatted correctly with CRYPT:salt:iv:hash.");
        }
        return parts[index];
    }

    private static IllegalArgumentException invalidCryptFormat(int partCount) {
        logger.error("The secret text is not formatted correctly. Expected 4 parts, got {}.", partCount);
        return new IllegalArgumentException("Unable to decrypt, input string is not formatted correctly with CRYPT:salt:iv:hash.");
    }

    /**
     * Gets the password from a subclass provider. The base class has no default secret.
     *
     * @return char array of the password
     */
    protected char[] getPassword() {
        throw new IllegalStateException("No master-password provider configured. Select AutoAESSaltDecryptor "
                + "with an explicit light_4j_config_password, ManualAESSaltDescryptor, or a custom password provider.");
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
