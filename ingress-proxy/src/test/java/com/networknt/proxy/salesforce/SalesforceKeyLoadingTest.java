package com.networknt.proxy.salesforce;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.Signature;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class SalesforceKeyLoadingTest {
    @Test
    void signingStillWorksAndInvalidPasswordFails() throws Exception {
        Constructor<SalesforceConfig> constructor = SalesforceConfig.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        SalesforceConfig config = constructor.newInstance();
        config.setCertFilename("client.keystore");
        config.setCertPassword("password");
        SalesforceHandler handler = new SalesforceHandler();
        Method create = SalesforceHandler.class.getDeclaredMethod("createJwt", String.class, String.class, String.class, SalesforceConfig.class);
        create.setAccessible(true);
        String jwt = (String) create.invoke(handler, "issuer", "subject", "audience", config);
        String[] parts = jwt.split("\\.");
        assertEquals(3, parts.length);
        KeyStore store = KeyStore.getInstance("JKS");
        try (var input = getClass().getResourceAsStream("/config/client.keystore")) {
            store.load(input, "password".toCharArray());
        }
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(store.getCertificate("client"));
        signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
        assertTrue(signature.verify(Base64.getUrlDecoder().decode(parts[2])));
        config.setCertPassword("incorrect-password");
        assertThrows(InvocationTargetException.class, () -> create.invoke(handler, "issuer", "subject", "audience", config));
    }
}
