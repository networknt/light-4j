package com.networknt.decrypt;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class ManualDecryptorLifecycleTest {
    private static final class Prompt extends ManualAESSaltDescryptor {
        private int prompts;
        private String password = "manual-master-canary";
        private final List<char[]> issued = new ArrayList<>();

        private Prompt(int capacity, AtomicLong clock) {
            super(capacity, 100, clock::get);
        }

        @Override protected char[] readPassword() {
            prompts++;
            char[] value = password.toCharArray();
            issued.add(value);
            return value;
        }
    }

    @Test
    void repeatDecryptDoesNotPromptAndEvictionAndCloseClearOwnedKeys() throws Exception {
        AtomicLong clock = new AtomicLong();
        Prompt decryptor = new Prompt(1, clock);
        String first = AESSaltSecurityTest.encrypt("manual-master-canary", "first");
        String second = AESSaltSecurityTest.encrypt("manual-master-canary", "second");
        assertEquals("first", decryptor.decrypt(first));
        byte[] firstKey = cachedKey(decryptor);
        assertEquals("first", decryptor.decrypt(first));
        assertEquals(1, decryptor.prompts);
        assertEquals("second", decryptor.decrypt(second));
        assertEquals(2, decryptor.prompts);
        assertArrayEquals(new byte[firstKey.length], firstKey);
        byte[] secondKey = cachedKey(decryptor);
        decryptor.close();
        assertArrayEquals(new byte[secondKey.length], secondKey);
        assertThrows(IllegalStateException.class, () -> decryptor.decrypt(second));
        assertEquals(2, decryptor.prompts);
        for (char[] value : decryptor.issued) assertArrayEquals(new char[value.length], value);
    }

    @Test
    void expiredKeyIsClearedAndRequiresFreshPrompt() throws Exception {
        AtomicLong clock = new AtomicLong();
        try (Prompt decryptor = new Prompt(2, clock)) {
            String encrypted = AESSaltSecurityTest.encrypt("manual-master-canary", "plaintext");
            assertEquals("plaintext", decryptor.decrypt(encrypted));
            byte[] oldKey = cachedKey(decryptor);
            clock.set(100);
            assertEquals("plaintext", decryptor.decrypt(encrypted));
            assertEquals(2, decryptor.prompts);
            assertArrayEquals(new byte[oldKey.length], oldKey);
        }
    }

    @Test
    void failedAuthenticationDoesNotCacheTheWrongKey() throws Exception {
        try (Prompt decryptor = new Prompt(2, new AtomicLong())) {
            String encrypted = AESSaltSecurityTest.encrypt("manual-master-canary", "plaintext");
            decryptor.password = "wrong-master-canary";
            assertThrows(RuntimeException.class, () -> decryptor.decrypt(encrypted));
            decryptor.password = "manual-master-canary";
            assertEquals("plaintext", decryptor.decrypt(encrypted));
            assertEquals(2, decryptor.prompts);
            for (char[] value : decryptor.issued) assertArrayEquals(new char[value.length], value);
        }
    }

    private static byte[] cachedKey(ManualAESSaltDescryptor decryptor) throws Exception {
        Field cache = ManualAESSaltDescryptor.class.getDeclaredField("keys");
        cache.setAccessible(true);
        Object entry = ((Map<?, ?>) cache.get(decryptor)).values().iterator().next();
        Field bytes = entry.getClass().getDeclaredField("bytes");
        bytes.setAccessible(true);
        return (byte[]) bytes.get(entry);
    }
}
