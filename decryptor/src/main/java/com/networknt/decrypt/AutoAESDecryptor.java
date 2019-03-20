package com.networknt.decrypt;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * This decryptor supports retrieving decrypted password of configuration files
 * from environment variables. If password cannot be found, a runtimeException
 * will be thrown.
 *
 * To use this decryptor, adding the following line into config.yml
 * decryptorClass: com.networknt.decrypt.AutoAESDecryptor
 */
public class AutoAESDecryptor implements Decryptor {
    private static char[] PASSWORD;
    private static final byte[] SALT = { (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0 };
    private static final int ITERATIONS = 65536;
    // environment variable key of decrypted password
    private static final String CONFIG_PASSWORD = "config_password";
    private static final String STRING_ENCODING = "UTF-8";
    private static final int KEY_SIZE = 128;

    private SecretKeySpec secret;

    private Cipher cipher;

    private Base64.Decoder base64Decoder = Base64.getDecoder();

    public AutoAESDecryptor() {
        try {
            init();
            /* Derive the key, given password and salt. */
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec;

            spec = new PBEKeySpec(PASSWORD, SALT, ITERATIONS, KEY_SIZE);
            SecretKey tmp = factory.generateSecret(spec);
            secret = new SecretKeySpec(tmp.getEncoded(), "AES");

            // CBC = Cipher Block chaining
            // PKCS5Padding Indicates that the keys are padded
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize AutoAESDecryptor.", e);
        }
    }

    private static void init() {
        String passwordStr = System.getenv(CONFIG_PASSWORD);
        if (passwordStr == null || passwordStr.trim().equals("")) {
            throw new RuntimeException("Unable to retrieve decrypted password of configuration files from environment variables.");
        }
        PASSWORD = passwordStr.toCharArray();
    }

    @Override
    public String decrypt(String input) {
        if (!input.startsWith(CRYPT_PREFIX)) {
            throw new RuntimeException("Unable to decrypt, input string does not start with 'CRYPT'.");
        }

        try {
            String encodedValue = input.substring(6, input.length());
            byte[] data = base64Decoder.decode(encodedValue);
            int keylen = KEY_SIZE / 8;
            byte[] iv = new byte[keylen];
            System.arraycopy(data, 0, iv, 0, keylen);
            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
            return new String(cipher.doFinal(data, keylen, data.length - keylen), STRING_ENCODING);
        } catch (Exception e) {
            throw new RuntimeException("Unable to decrypt.", e);
        }
    }
}
