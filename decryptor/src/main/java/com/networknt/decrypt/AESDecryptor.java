package com.networknt.decrypt;

import com.networknt.utility.Constants;
import com.networknt.utility.Decryptor;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.KeySpec;

public class AESDecryptor implements Decryptor {
    private static final int ITERATIONS = 65536;

    private static final String STRING_ENCODING = "UTF-8";

    /**
     * If we user Key size of 256 we will get java.security.InvalidKeyException:
     * Illegal key size or default parameters , Unless we configure Java
     * Cryptography Extension 128
     */
    private static final int KEY_SIZE = 128;

    private static final byte[] SALT = { (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0 };

    private SecretKeySpec secret;

    private Cipher cipher;

    private BASE64Decoder base64Decoder;

    public AESDecryptor() {
        try
        {
            /* Derive the key, given password and salt. */
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec;

            spec = new PBEKeySpec(Constants.FRAMEWORK_NAME.toCharArray(), SALT, ITERATIONS, KEY_SIZE);
            SecretKey tmp = factory.generateSecret(spec);
            secret = new SecretKeySpec(tmp.getEncoded(), "AES");

            // CBC = Cipher Block chaining
            // PKCS5Padding Indicates that the keys are padded
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            // For production use commons base64 encoder
            base64Decoder = new BASE64Decoder();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to initialize AESDecryptor", e);
        }
    }

    public String decrypt(String input) {
        if (!input.startsWith(CRYPT_PREFIX))
        {
            throw new RuntimeException("Unable to decrypt, input string does not start with 'CRYPT'");
        }

        try
        {
            byte[] data = base64Decoder.decodeBuffer(input.substring(6, input.length()));
            int keylen = KEY_SIZE / 8;
            byte[] iv = new byte[keylen];
            System.arraycopy(data, 0, iv, 0, keylen);
            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
            return new String(cipher.doFinal(data, keylen, data.length - keylen), STRING_ENCODING);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to decrypt.", e);
        }
    }
}
