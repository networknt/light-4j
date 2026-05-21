package com.networknt.decrypt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.HexFormat;

public class AESDecryptorTest {

    private static final int ITERATIONS = 65536;
    private static final int KEY_SIZE = 256;
    private static final byte[] LEGACY_IV = new byte[16];
    private static final String GCM_PASSWORD_VALUE = "CRYPT:e6e1b4a099902a2106509e87df909de4:cd9153addc755eb7e00d817c:e499d4501d0372fe37073d88a1025f1970a30cbd9acec7d5";

    @Test
    public void testConstructor() {
        AESSaltDecryptor decryptor = new AESSaltDecryptor();
        Assertions.assertNotNull(decryptor);
    }

    @Test
    public void testForName() {
        String decryptorClass = "com.networknt.decrypt.AESSaltDecryptor";
        try {
            Class<?> typeClass = Class.forName(decryptorClass);

            if (!typeClass.isInterface()) {
                Decryptor decryptor = (Decryptor) typeClass.getConstructor().newInstance();
                Assertions.assertNotNull(decryptor);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to construct the decryptor due to lack of decryption password.", e);
        }

    }

    @Test
    public void testDecryptLegacyCbcFormat() throws Exception {
        AESSaltDecryptor decryptor = new AESSaltDecryptor();
        String secretText = legacyEncrypt("password", "00112233445566778899aabbccddeeff");
        Assertions.assertEquals("password", decryptor.decrypt(secretText));
    }

    @Test
    public void testDecryptGcmFormat() {
        AESSaltDecryptor decryptor = new AESSaltDecryptor();
        Assertions.assertEquals("password", decryptor.decrypt(GCM_PASSWORD_VALUE));
    }

    @Test
    public void testDecryptRejectsInvalidPartCount() {
        AESSaltDecryptor decryptor = new AESSaltDecryptor();
        Assertions.assertThrows(IllegalArgumentException.class, () -> decryptor.decrypt("CRYPT:001122"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> decryptor.decrypt("CRYPT:001122:334455:667788:99aabb"));
    }

    @Test
    public void testDecryptRejectsEmptyParts() {
        AESSaltDecryptor decryptor = new AESSaltDecryptor();
        Assertions.assertThrows(IllegalArgumentException.class, () -> decryptor.decrypt("CRYPT:001122::667788"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> decryptor.decrypt("CRYPT:001122:334455:"));
    }

    @Test
    public void testDecryptRejectsBlankMasterPassword() {
        AESSaltDecryptor decryptor = new AESSaltDecryptor() {
            @Override
            protected char[] getPassword() {
                return "   ".toCharArray();
            }
        };
        Assertions.assertThrows(IllegalStateException.class, () -> decryptor.decrypt(GCM_PASSWORD_VALUE));
    }

    /**
     * A test case that can be used as a utility to decrypt encrypted value based on the master secret or password.
     * The master secret will be loaded from the environment variable in this test case so that the user's master secret
     * won't be revealed accidentally.
     *
     * Here is the doc on how to use this test case as a utility. https://doc.networknt.com/concern/decryptor/
     */
    @Disabled
    @Test
    public void testDecryptWithEnv() {
        String secretText = "CRYPT:69c73ee9840e4d4f8b53115361236b52:aaf53a8a0efb269253fbb0a8:bbe1b8c479234458830998eeb39f78b6dbe038d5150f9f57";
        AutoAESSaltDecryptor decryptor = new AutoAESSaltDecryptor();
        String clearText = decryptor.decrypt(secretText);
        System.out.println("clearText = " + clearText);
    }

    private static String legacyEncrypt(String plaintext, String saltHex) throws Exception {
        byte[] salt = HexFormat.of().parseHex(saltHex);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec("light".toCharArray(), salt, ITERATIONS, KEY_SIZE);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secret = new SecretKeySpec(tmp.getEncoded(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(LEGACY_IV));
        return Decryptor.CRYPT_PREFIX + ":" + saltHex + ":" + HexFormat.of().formatHex(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
    }
}
