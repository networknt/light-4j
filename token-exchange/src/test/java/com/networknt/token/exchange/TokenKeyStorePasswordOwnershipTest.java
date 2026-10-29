package com.networknt.token.exchange;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenKeyStorePasswordOwnershipTest {
    @Test
    void borrowedPasswordsRemainUsableAfterSuccessAndFailure() {
        TokenKeyStoreManager manager = new TokenKeyStoreManager();
        char[] storePassword = "password".toCharArray();
        char[] keyPassword = "password".toCharArray();
        assertNotNull(manager.getPrivateKey("client.keystore", storePassword, "client", keyPassword));
        assertTrue(manager.getKeyManagers("client.keystore", storePassword, keyPassword, null).length > 0);
        assertTrue(manager.getTrustManagers("client.keystore", storePassword, null).length > 0);
        char[] incorrect = "incorrect-password".toCharArray();
        assertThrows(RuntimeException.class,
                () -> manager.getPrivateKey("client.keystore", storePassword, "client", incorrect));
        assertArrayEquals("incorrect-password".toCharArray(), incorrect);
        assertArrayEquals("password".toCharArray(), storePassword);
        assertArrayEquals("password".toCharArray(), keyPassword);
        assertNotNull(manager.getPrivateKey("client.keystore", storePassword, "client", keyPassword));
    }
}
