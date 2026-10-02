package com.networknt.ldap;

import com.networknt.config.ConfigException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.UTF_8;

class LdapUtilEmptyPasswordTest {

    @Test
    void emptyBindPasswordsAreRejectedBeforeConfigurationAccess() throws Exception {
        // A null configuration proves rejection happens before configuration access or LDAP contact.
        Assertions.assertFalse(LdapUtil.testBind("uid=jduke", "", null));
        Assertions.assertFalse(LdapUtil.testBind("uid=jduke", null, null));
        Assertions.assertFalse(LdapUtil.testBind("", "secret", null));
        Assertions.assertFalse(LdapUtil.testBind(null, "secret", null));
    }

    @Test
    void emptyBindPasswordsNeverContactLdap() throws Exception {
        // Reject at the server too: catching a bind failure must not mask an unwanted connection.
        assertUserBind("uid=jduke", "", false, false, 49);
        assertUserBind("uid=jduke", null, false, false, 49);
    }

    @Test
    void emptyBindDnsNeverContactLdap() throws Exception {
        assertUserBind("", "secret", false, false, 49);
        assertUserBind(null, "secret", false, false, 49);
    }

    @Test
    void nonemptyBindPasswordsAreSentUnchanged() throws Exception {
        assertUserBind("uid=jduke", "secret", true, true, 0);
        assertUserBind("uid=jduke", " \t ", true, true, 0);
    }

    @Test
    void invalidBindCredentialsAreRejected() throws Exception {
        assertUserBind("uid=jduke", "wrong", true, false, 49);
    }

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

    private static void assertUserBind(String dn, String password, boolean expectedContact,
                                       boolean expectedAuthenticated, int resultCode) throws Exception {
        LdapConfig config = LdapConfig.load("ldap-probe");
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            server.setSoTimeout(1500);
            config.uri = "ldap://127.0.0.1:" + server.getLocalPort();
            AtomicBoolean contacted = new AtomicBoolean();
            AtomicReference<String> submittedDn = new AtomicReference<>();
            AtomicReference<byte[]> submittedPassword = new AtomicReference<>();
            AtomicReference<Throwable> listenerError = new AtomicReference<>();
            Thread listener = new Thread(() -> {
                try (Socket connection = server.accept()) {
                    contacted.set(true);
                    connection.setSoTimeout(1500);
                    // Read an LDAPMessage containing a BindRequest, then reply with a BindResponse.
                    DataInputStream message = new DataInputStream(new ByteArrayInputStream(
                            readElement(new DataInputStream(connection.getInputStream()), 0x30)));
                    byte[] messageId = readElement(message, 0x02);
                    DataInputStream bind = new DataInputStream(new ByteArrayInputStream(readElement(message, 0x60)));
                    Assertions.assertArrayEquals(new byte[] {3}, readElement(bind, 0x02));
                    submittedDn.set(new String(readElement(bind, 0x04), UTF_8));
                    submittedPassword.set(readElement(bind, 0x80));
                    OutputStream response = connection.getOutputStream();
                    response.write(new byte[] {0x30, (byte) (11 + messageId.length), 0x02, (byte) messageId.length});
                    response.write(messageId);
                    response.write(new byte[] {0x61, 0x07, 0x0a, 0x01, (byte) resultCode, 0x04, 0x00, 0x04, 0x00});
                    response.flush();
                } catch (SocketTimeoutException e) {
                    if (contacted.get()) listenerError.set(e);
                } catch (Exception | AssertionError e) {
                    listenerError.set(e);
                }
            });
            listener.setDaemon(true);
            listener.start();
            try {
                Assertions.assertEquals(expectedAuthenticated, LdapUtil.testBind(dn, password, config));
            } finally {
                listener.join(2500);
            }
            Assertions.assertFalse(listener.isAlive());
            Assertions.assertNull(listenerError.get());
            Assertions.assertEquals(expectedContact, contacted.get());
            if (expectedContact) {
                Assertions.assertEquals(dn, submittedDn.get());
                Assertions.assertArrayEquals(password.getBytes(UTF_8), submittedPassword.get());
            }
        }
    }

    private static byte[] readElement(DataInputStream input, int expectedTag) throws IOException {
        Assertions.assertEquals(expectedTag, input.readUnsignedByte());
        int length = input.readUnsignedByte();
        if ((length & 0x80) != 0) {
            int lengthBytes = length & 0x7f;
            // The loopback fixture only accepts small, definite-length BER elements.
            Assertions.assertTrue(lengthBytes > 0 && lengthBytes <= 2);
            length = 0;
            for (int i = 0; i < lengthBytes; i++) {
                length = (length << 8) | input.readUnsignedByte();
            }
        }
        byte[] value = new byte[length];
        input.readFully(value);
        return value;
    }
}
