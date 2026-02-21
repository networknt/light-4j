package com.networknt.ldap;

import org.ietf.jgss.Oid;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Disabled
public class LdapUtilTest {

    static final Logger logger = LoggerFactory.getLogger(LdapUtilTest.class);
    public static Oid SPNEGO;

    @BeforeAll
    public static void setup() throws Exception {
        ApacheDirectoryServer.startServer();
        SPNEGO = new Oid("1.3.6.1.5.5.2");
    }

    @Test
    public void testAuthentication() throws Exception {
        String user = "jduke";
        String password = "theduke";

        Assertions.assertEquals(true, LdapUtil.authenticate(user, password));
    }

    @Test
    public void testAuthorization() throws Exception {
        String user = "jduke";
        String expectedGroups =
            "cn=just-users,ou=users,dc=undertow,dc=io,cn=best-users,ou=users,dc=undertow,dc=io";

        Assertions.assertEquals(
            expectedGroups,
            String.join(",", LdapUtil.authorize(user))
        );
    }

    @Test
    public void testAuth() throws Exception {
        String user = "jduke";
        String password = "theduke";

        // function returns null always
        Assertions.assertEquals(null, LdapUtil.auth(user, password));
    }
}
