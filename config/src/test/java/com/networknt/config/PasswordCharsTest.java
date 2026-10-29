package com.networknt.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordCharsTest {
    @Test
    void clearsOwnedCopyAfterSuccessfulKeyStoreLoadWithoutChangingConfiguration() {
        char[] configured = "password".toCharArray();
        char[] temporary;
        PasswordChars copy = PasswordChars.copyOf(configured);
        try (copy) {
            temporary = copy.value();
            assertNotSame(configured, temporary);
            assertNotNull(TlsUtil.loadKeyStore("client.keystore", temporary));
        }
        assertArrayEquals(new char[temporary.length], temporary);
        assertArrayEquals("password".toCharArray(), configured);
        assertThrows(IllegalStateException.class, copy::value);
    }

    @Test
    void clearsOwnedCopyWhenKeyStoreLoadFails() {
        char[] temporary;
        PasswordChars copy = PasswordChars.copyOf("wrong-password");
        temporary = copy.value();
        assertThrows(RuntimeException.class, () -> {
            try (copy) {
                TlsUtil.loadKeyStore("client.keystore", temporary);
            }
        });
        assertArrayEquals(new char[temporary.length], temporary);
        copy.close();
    }

    @Test
    void preservesNullPasswordSemantics() {
        try (PasswordChars copy = PasswordChars.copyOf((String) null)) {
            assertNull(copy.value());
        }
    }
    @Test
    void requiredPasswordsRejectNullButOptionalCopiesAllowIt() {
        assertThrows(NullPointerException.class, () -> PasswordChars.required(null));
        try (PasswordChars password = PasswordChars.required("value")) {
            assertArrayEquals("value".toCharArray(), password.value());
        }
    }

}
