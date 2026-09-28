package com.networknt.ldap;

import com.networknt.config.ConfigException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class LdapUtilEmptyPasswordTest {

    @Test
    void emptyPasswordsNeverContactLdap() throws Exception {
        assertLdapContact("", false);
        assertLdapContact(null, false);
        assertLdapContact("wrong", true);
    }

    @Test
    void escapesLdapFilterValues() {
        Assertions.assertEquals("alice\\2a\\28x\\29\\5c\\00", LdapUtil.escapeFilterValue("alice*(x)\\\0"));
    }

    @Test
    void loadsConfiguredTimeouts() {
        LdapConfig config = LdapConfig.load("ldap-probe");
        Assertions.assertEquals(750, config.getConnectTimeoutMs());
        Assertions.assertEquals(1250, config.getReadTimeoutMs());
        Assertions.assertThrows(ConfigException.class, () -> LdapConfig.load("ldap-probe-invalid"));
    }

    private static void assertLdapContact(String password, boolean expectedContact) throws Exception {
        LdapConfig config = LdapConfig.load("ldap-probe");
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            server.setSoTimeout(1500);
            config.uri = "ldap://127.0.0.1:" + server.getLocalPort();
            AtomicBoolean contacted = new AtomicBoolean();
            AtomicReference<IOException> listenerError = new AtomicReference<>();
            Thread listener = new Thread(() -> {
                try (Socket connection = server.accept()) {
                    contacted.set(connection.isConnected());
                } catch (SocketTimeoutException _) {
                    // An empty password must not connect.
                } catch (IOException e) {
                    listenerError.set(e);
                }
            });
            listener.setDaemon(true);
            listener.start();
            Assertions.assertFalse(LdapUtil.authenticate("jduke", password, () -> config));
            listener.join(2500);
            Assertions.assertFalse(listener.isAlive());
            Assertions.assertNull(listenerError.get());
            Assertions.assertEquals(expectedContact, contacted.get());
        }
    }
}
