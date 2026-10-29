package com.networknt.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CryptMigrationDiagnosticTest {
    @Test
    void identifiesConfigurationFileAndYamlLocationWithoutEchoingSecret() {
        Config config = Config.getInstance();
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> config.getJsonMapConfigNoCache("legacy-crypt-review"));
        StringBuilder messages = new StringBuilder();
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            messages.append(cause.getMessage()).append('\n');
        }
        String diagnostic = messages.toString();
        assertTrue(diagnostic.contains("legacy-crypt-review.yml"));
        assertTrue(diagnostic.contains("YAML line 2"));
        assertTrue(diagnostic.contains("column 13"));
        assertTrue(diagnostic.contains("Re-encrypt"));
        assertFalse(diagnostic.contains("legacy-salt-canary"));
        assertFalse(diagnostic.contains("legacy-ciphertext-canary"));
    }
    @Test
    void authenticationFailureDoesNotSuggestLegacyMigration() {
        String previous = System.getProperty("light_4j_config_password");
        System.setProperty("light_4j_config_password", "light");
        try {
            Config config = Config.getInstance();
            RuntimeException failure = assertThrows(RuntimeException.class,
                    () -> config.getJsonMapConfigNoCache("gcm-crypt-review"));
            boolean authenticationFailure = false;
            StringBuilder messages = new StringBuilder();
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                authenticationFailure |= cause instanceof javax.crypto.AEADBadTagException;
                messages.append(cause.getMessage()).append('\n');
            }
            String diagnostic = messages.toString();
            assertTrue(authenticationFailure);
            assertTrue(diagnostic.contains("gcm-crypt-review.yml"));
            assertTrue(diagnostic.contains("YAML line 2"));
            assertFalse(diagnostic.contains("Re-encrypt"));
            assertFalse(diagnostic.contains("legacy three-part"));
            assertFalse(diagnostic.contains("e499d4501d0372fe37073d88a1025f19"));
        } finally {
            if (previous == null) System.clearProperty("light_4j_config_password");
            else System.setProperty("light_4j_config_password", previous);
        }
    }

}
