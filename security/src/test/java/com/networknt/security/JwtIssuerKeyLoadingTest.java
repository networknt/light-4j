package com.networknt.security;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.security.PrivateKey;

import static org.junit.jupiter.api.Assertions.*;

class JwtIssuerKeyLoadingTest {
    @Test
    void loadsSigningKeyAndPreservesExistingFailureBehavior() throws Exception {
        Method load = JwtIssuer.class.getDeclaredMethod("getPrivateKey", String.class, String.class, String.class);
        load.setAccessible(true);
        assertInstanceOf(PrivateKey.class, load.invoke(null, "server.keystore", "password", "server"));
        assertNull(load.invoke(null, "server.keystore", "incorrect-password", "server"));
        assertNull(load.invoke(null, "missing-review.keystore", "password", "server"));
    }
}
