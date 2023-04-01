package com.networknt.decrypt;

import org.junit.Assert;
import org.junit.Test;

public class AESDecryptorTest {

    @Test
    public void testConstructor() {
        AESSaltDecryptor decryptor = new AESSaltDecryptor();
        Assert.assertNotNull(decryptor);
    }

    @Test
    public void testForName() {
        String decryptorClass = "com.networknt.decrypt.AESSaltDecryptor";
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
