package com.networknt.ldap;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApacheDirectoryServerTlsTest {
    @Test
    void ldapsPresentsConfiguredServerCertificate() throws Exception {
        ApacheDirectoryServer.startServer();
        KeyStore store = KeyStore.getInstance("JKS");
        try (InputStream input = getClass().getResourceAsStream("/config/server.keystore")) {
            assertNotNull(input);
            store.load(input, "password".toCharArray());
        }
        TrustManagerFactory trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(store);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trust.getTrustManagers(), null);
        try (SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket()) {
            socket.connect(new InetSocketAddress("localhost", ApacheDirectoryServer.LDAPS_PORT), 5000);
            socket.setSoTimeout(5000);
            socket.startHandshake();
            Certificate presented = socket.getSession().getPeerCertificates()[0];
            List<String> aliases = Collections.list(store.aliases());
            boolean matches = false;
            for (String alias : aliases) {
                if (store.isKeyEntry(alias) && presented.equals(store.getCertificate(alias))) matches = true;
            }
            assertTrue(matches, "LDAPS must present the certificate from the configured server keystore");
        }
    }
}
