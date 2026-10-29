package com.networknt.decrypt;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class AESSaltSecurityTest {
    private static final HexFormat HEX = HexFormat.of();

    @Test
    void rejectsLegacyBeforeRequestingPasswordAndDoesNotEchoCiphertext() {
        AESSaltDecryptor decryptor = new AESSaltDecryptor() {
            @Override protected char[] getPassword() {
                fail("Legacy input must be rejected before password retrieval");
                return null;
            }
        };
        String input = "CRYPT:00112233445566778899aabbccddeeff:0123456789abcdef";
        Exception failure = assertThrows(IllegalArgumentException.class, () -> decryptor.decrypt(input));
        assertTrue(failure.getMessage().contains("Re-encrypt"));
        assertFalse(failure.getMessage().contains(input));
    }

    @Test
    void honorsIndependentRandomSaltsAndNonces() throws Exception {
        String first = encrypt("master-canary", "plaintext-canary");
        String second = encrypt("master-canary", "plaintext-canary");
        assertNotEquals(first.split(":")[1], second.split(":")[1]);
        assertNotEquals(first.split(":")[2], second.split(":")[2]);
        AESSaltDecryptor decryptor = borrowed("master-canary".toCharArray());
        assertEquals("plaintext-canary", decryptor.decrypt(first));
        assertEquals("plaintext-canary", decryptor.decrypt(second));
    }

    @Test
    void rejectsTamperedSaltNonceAndAuthenticatedPayload() throws Exception {
        String encrypted = encrypt("master-canary", "plaintext-canary");
        AESSaltDecryptor decryptor = borrowed("master-canary".toCharArray());
        for (int field = 1; field <= 3; field++) {
            String[] parts = encrypted.split(":");
            parts[field] = (parts[field].charAt(0) == '0' ? "1" : "0") + parts[field].substring(1);
            assertThrows(RuntimeException.class, () -> decryptor.decrypt(String.join(":", parts)));
        }
    }

    @Test
    void neverClearsBorrowedProviderPasswordAndNeverReusesAStaleKey() throws Exception {
        char[] password = "first-master".toCharArray();
        AESSaltDecryptor decryptor = borrowed(password);
        String encrypted = encrypt("first-master", "plaintext-canary");
        assertEquals("plaintext-canary", decryptor.decrypt(encrypted));
        assertArrayEquals("first-master".toCharArray(), password);
        Arrays.fill(password, 'x');
        assertThrows(RuntimeException.class, () -> decryptor.decrypt(encrypted));
        assertArrayEquals("xxxxxxxxxxxx".toCharArray(), password);
    }

    @Test
    void autoProviderClearsOwnedPasswordOnSuccessAndAuthenticationFailure() throws Exception {
        String key = "light_4j_config_password";
        String previous = System.getProperty(key);
        class Observed extends AutoAESSaltDecryptor {
            char[] issued;
            @Override protected char[] getPassword() {
                return issued = super.getPassword();
            }
        }
        try {
            System.setProperty(key, "first-master");
            Observed decryptor = new Observed();
            String encrypted = encrypt("first-master", "plaintext-canary");
            assertEquals("plaintext-canary", decryptor.decrypt(encrypted));
            assertArrayEquals(new char[decryptor.issued.length], decryptor.issued);
            System.setProperty(key, "second-master");
            assertThrows(RuntimeException.class, () -> decryptor.decrypt(encrypted));
            assertArrayEquals(new char[decryptor.issued.length], decryptor.issued);
            assertEquals("updated", decryptor.decrypt(encrypt("second-master", "updated")));
        } finally {
            if (previous == null) System.clearProperty(key);
            else System.setProperty(key, previous);
        }
    }

    @Test
    void ownedPasswordIsReleasedEvenWhenValidationFails() throws Exception {
        class Owned extends AESSaltDecryptor {
            final char[] password = "   ".toCharArray();
            @Override protected char[] getPassword() { return password; }
            @Override protected void releasePassword(char[] value) { Arrays.fill(value, '\0'); }
        }
        Owned decryptor = new Owned();
        String encrypted = encrypt("master", "plaintext");
        assertThrows(IllegalStateException.class, () -> decryptor.decrypt(encrypted));
        assertArrayEquals(new char[3], decryptor.password);
    }

    private static AESSaltDecryptor borrowed(char[] password) {
        return new AESSaltDecryptor() {
            @Override protected char[] getPassword() { return password; }
        };
    }

    // Synthetic fixtures only. The external light-encryptor is the production producer.
    private static String encrypt(String master, String plaintext) throws Exception {
        byte[] salt = new byte[16];
        byte[] nonce = new byte[12];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        random.nextBytes(nonce);
        char[] password = master.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
        byte[] encoded = null;
        byte[] input = plaintext.getBytes(StandardCharsets.UTF_8);
        try {
            encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encoded, "AES"), new GCMParameterSpec(128, nonce));
            return "CRYPT:" + HEX.formatHex(salt) + ":" + HEX.formatHex(nonce) + ":" + HEX.formatHex(cipher.doFinal(input));
        } finally {
            spec.clearPassword();
            Arrays.fill(password, '\0');
            Arrays.fill(input, (byte) 0);
            if (encoded != null) Arrays.fill(encoded, (byte) 0);
        }
    }
}
