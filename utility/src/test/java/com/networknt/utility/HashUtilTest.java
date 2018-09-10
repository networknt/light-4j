package com.networknt.utility;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by stevehu on 2016-12-23.
 */
public class HashUtilTest {
    @Test
    public void testMd5Hex() {
        String md5 = HashUtil.md5Hex("stevehu@gmail.com");
        Assert.assertEquals(md5, "417bed6d9644f12d8bc709059c225c27");
    }
    @Test
    public void testPasswordHash() throws Exception {
        String p = "123456";
        String hashedPass = HashUtil.generateStrongPasswordHash(p);
        System.out.println("hashedPass = " + hashedPass);
        Assert.assertTrue(HashUtil.validatePassword(p.toCharArray(), hashedPass));
    }

    @Test
    public void testClientSecretHash() throws Exception {
        String s = "f6h1FTI8Q3-7UScPZDzfXA";
        String hashedPass = HashUtil.generateStrongPasswordHash(s);
        System.out.println("hashedSecret = " + hashedPass);
        Assert.assertTrue(HashUtil.validatePassword(s.toCharArray(), hashedPass));
    }

}
