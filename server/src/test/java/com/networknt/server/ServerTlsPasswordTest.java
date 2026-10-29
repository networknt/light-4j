package com.networknt.server;

import com.networknt.info.ServerInfoUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ServerTlsPasswordTest {
    @Test
    void serverStoresAndPrivateKeyLoadAndInvalidPasswordsFail() throws Exception {
        ServerConfig config = ServerConfig.load();
        String storeName = config.getKeystoreName();
        String storePass = config.getKeystorePass();
        String keyPass = config.getKeyPass();
        String trustName = config.getTruststoreName();
        String trustPass = config.getTruststorePass();
        try {
            config.setKeystoreName("server.keystore");
            config.setKeystorePass("password");
            config.setKeyPass("password");
            config.setTruststoreName("server.truststore");
            config.setTruststorePass("password");
            assertNotNull(Server.loadKeyStore());
            assertNotNull(Server.loadTrustStore());
            Method initialize = Server.class.getDeclaredMethod("createSSLContext");
            initialize.setAccessible(true);
            assertNotNull(initialize.invoke(null));
            config.setKeyPass("incorrect-password");
            assertThrows(InvocationTargetException.class, () -> initialize.invoke(null));
            config.setKeystorePass("incorrect-password");
            assertThrows(RuntimeException.class, Server::loadKeyStore);
            config.setTruststorePass("incorrect-password");
            assertThrows(RuntimeException.class, Server::loadTrustStore);
        } finally {
            config.setKeystoreName(storeName);
            config.setKeystorePass(storePass);
            config.setKeyPass(keyPass);
            config.setTruststoreName(trustName);
            config.setTruststorePass(trustPass);
        }
    }

    @Test
    void fingerprintReadsRealCertificateAndHandlesInvalidPassword() {
        ServerConfig config = ServerConfig.load();
        String name = config.getKeystoreName();
        String password = config.getKeystorePass();
        try {
            config.setKeystoreName("server.keystore");
            config.setKeystorePass("password");
            assertNotNull(ServerInfoUtil.getServerTlsFingerPrint());
            config.setKeystorePass("incorrect-password");
            assertNull(ServerInfoUtil.getServerTlsFingerPrint());
        } finally {
            config.setKeystoreName(name);
            config.setKeystorePass(password);
        }
    }

    @Test
    void bootstrapLoadersUseRealTrustStoreAndRetainTheirFailureContracts() throws Exception {
        assumeTrue(System.getenv(UrlConfigLoader.CLIENT_TRUSTSTORE_LOC) == null
                && System.getenv(UrlConfigLoader.CLIENT_TRUSTSTORE_PASS) == null,
                "Do not consume operator-provided bootstrap credentials in this fixture");
        ServerConfig config = ServerConfig.load();
        String name = config.getBootstrapStoreName();
        String password = config.getBootstrapStorePass();
        String locationProperty = System.getProperty(DefaultConfigLoader.CLIENT_TRUSTSTORE_LOC);
        String passwordProperty = System.getProperty(DefaultConfigLoader.CLIENT_TRUSTSTORE_PASS);
        try {
            config.setBootstrapStoreName(Path.of(getClass().getResource("/config/server.truststore").toURI()).toString());
            config.setBootstrapStorePass("password");
            System.setProperty(DefaultConfigLoader.CLIENT_TRUSTSTORE_LOC, config.getBootstrapStoreName());
            System.setProperty(DefaultConfigLoader.CLIENT_TRUSTSTORE_PASS, "password");
            Method defaults = DefaultConfigLoader.class.getDeclaredMethod("loadBootstrapTrustStore");
            Method urls = UrlConfigLoader.class.getDeclaredMethod("loadBootstrapTrustStore");
            defaults.setAccessible(true);
            urls.setAccessible(true);
            assertNotNull(defaults.invoke(null));
            assertNotNull(urls.invoke(null));
            config.setBootstrapStorePass("incorrect-password");
            System.setProperty(DefaultConfigLoader.CLIENT_TRUSTSTORE_PASS, "incorrect-password");
            assertThrows(InvocationTargetException.class, () -> defaults.invoke(null));
            assertNull(urls.invoke(null));
        } finally {
            config.setBootstrapStoreName(name);
            config.setBootstrapStorePass(password);
            if (locationProperty == null) System.clearProperty(DefaultConfigLoader.CLIENT_TRUSTSTORE_LOC);
            else System.setProperty(DefaultConfigLoader.CLIENT_TRUSTSTORE_LOC, locationProperty);
            if (passwordProperty == null) System.clearProperty(DefaultConfigLoader.CLIENT_TRUSTSTORE_PASS);
            else System.setProperty(DefaultConfigLoader.CLIENT_TRUSTSTORE_PASS, passwordProperty);
        }
    }
}
