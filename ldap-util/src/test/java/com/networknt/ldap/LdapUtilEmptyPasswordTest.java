package com.networknt.ldap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

public class LdapUtilEmptyPasswordTest {

    @Test
    public void rejectsEmptyPassword() {
        Assertions.assertFalse(LdapUtil.authenticate("jduke", ""));
    }

    @Test
    public void rejectsNullPassword() {
        Assertions.assertFalse(LdapUtil.authenticate("jduke", null));
    }

    @Test
    public void neverBindsWithEmptyPassword() throws Exception {
        Method testBind = LdapUtil.class.getDeclaredMethod("testBind", String.class, String.class, LdapConfig.class);
        testBind.setAccessible(true);
        Assertions.assertFalse((Boolean) testBind.invoke(null, "uid=jduke,dc=example,dc=com", "", null));
        Assertions.assertFalse((Boolean) testBind.invoke(null, "uid=jduke,dc=example,dc=com", null, null));
    }
}
