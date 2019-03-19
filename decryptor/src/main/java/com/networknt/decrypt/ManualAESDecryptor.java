package com.networknt.decrypt;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Scanner;

public class ManualAESDecryptor implements Decryptor {
    private static char[] PASSWORD;
    private static final byte[] SALT = { (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0 };
    private static final int ITERATIONS = 65536;
    private static final String STRING_ENCODING = "UTF-8";
    private static final int KEY_SIZE = 128;

    private SecretKeySpec secret;

    private Cipher cipher;

    private Base64.Decoder base64Decoder = Base64.getDecoder();

    public ManualAESDecryptor() {
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
            throw new RuntimeException("Unable to initialize ManualAESDecryptor", e);
        }
    }

    private static void init() throws IOException {
        Console console = System.console();
        if (console != null) {
            PASSWORD = console.readPassword("Password for config decryption: ");
        } else {
            System.out.print("Password for config decryption: ");
            Scanner sc = new Scanner(System.in);
            PASSWORD = sc.next().toCharArray();
            sc.close();
        }
        if (PASSWORD == null) {
            throw new RuntimeException("The password of config decryption should not be empty");
        }
    }

    @Override
    public String decrypt(String input) {
        if (!input.startsWith(CRYPT_PREFIX)) {
            throw new RuntimeException("Unable to decrypt, input string does not start with 'CRYPT'");
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
