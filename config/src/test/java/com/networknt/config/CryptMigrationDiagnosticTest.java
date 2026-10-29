package com.networknt.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CryptMigrationDiagnosticTest {
    @Test
    void identifiesConfigurationFileAndYamlLocationWithoutEchoingSecret() {
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> Config.getInstance().getJsonMapConfigNoCache("legacy-crypt-review"));
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
}
