package com.networknt.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TlsPasswordLifecycleTest {
    private Field configNameField;
    private String previousConfigName;

    @BeforeEach
    void selectDefaultClientConfiguration() throws Exception {
        // Other client tests select named configurations through this shared static field.
        configNameField = ClientConfig.class.getDeclaredField("configName");
        configNameField.setAccessible(true);
        previousConfigName = (String) configNameField.get(null);
        configNameField.set(null, "client");
    }

    @AfterEach
    void restoreClientConfigurationSelection() throws Exception {
        configNameField.set(null, previousConfigName);
    }

    @Test
    void clientContextLoadsWithoutClearingBorrowedConfigurationPasswords() throws Exception {
        TlsConfig config = ClientConfig.get().getTls();
        char[] keyStore = config.getKeyStorePass().clone();
        char[] key = config.getKeyPass().clone();
        char[] trustStore = config.getTrustStorePass().clone();
        assertNotNull(Http2Client.createSSLContext());
        assertArrayEquals(keyStore, config.getKeyStorePass());
        assertArrayEquals(key, config.getKeyPass());
        assertArrayEquals(trustStore, config.getTrustStorePass());
    }

    @Test
    void defaultTrustLoadPreservesBorrowedPasswordAndSystemPropertyPrecedence() throws Exception {
        String[] properties = {"javax.net.ssl.trustStore", "javax.net.ssl.trustStorePassword", "javax.net.ssl.trustStoreType"};
        Map<String, String> previous = new HashMap<>();
        for (String property : properties) previous.put(property, System.getProperty(property));
        TlsConfig config = ClientConfig.get().getTls();
        Field field = TlsConfig.class.getDeclaredField("defaultCertPassword");
        field.setAccessible(true);
        char[] original = config.getDefaultCertPassword();
        char[] borrowed = "password".toCharArray();
        try {
            field.set(config, borrowed);
            System.setProperty(properties[0], Path.of(getClass().getClassLoader().getResource("config/client.truststore").toURI()).toString());
            System.setProperty(properties[2], "JKS");
            System.clearProperty(properties[1]);
            assertTrue(Http2Client.loadDefaultTrustStore().length > 0);
            assertArrayEquals("password".toCharArray(), borrowed);
            System.setProperty(properties[1], "incorrect-password");
            assertThrows(Exception.class, Http2Client::loadDefaultTrustStore);
            assertArrayEquals("password".toCharArray(), borrowed);
            System.setProperty(properties[1], "password");
            assertTrue(Http2Client.loadDefaultTrustStore().length > 0);
        } finally {
            field.set(config, original);
            for (String property : properties) {
                if (previous.get(property) == null) System.clearProperty(property);
                else System.setProperty(property, previous.get(property));
            }
        }
    }
}
