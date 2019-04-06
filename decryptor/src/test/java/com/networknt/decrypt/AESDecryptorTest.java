package com.networknt.decrypt;

import org.junit.Assert;
import org.junit.Test;

public class AESDecryptorTest {

    @Test
    public void testConstructor() {
        AESDecryptor decryptor = new AESDecryptor();
        Assert.assertNotNull(decryptor);
    }

    @Test
    public void testForName() {
        String decryptorClass = "com.networknt.decrypt.AESDecryptor";
        try {
            Class<?> typeClass = Class.forName(decryptorClass);

            if (!typeClass.isInterface()) {
                Decryptor decryptor = (Decryptor) typeClass.getConstructor().newInstance();
                Assert.assertNotNull(decryptor);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to construct the decryptor due to lack of decryption password.", e);
        }

    }
}
