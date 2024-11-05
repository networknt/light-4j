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

    /**
     * A test case that can be used as a utility to decrypt encrypted value based on the master secret or password.
     * The master secret will be loaded from the environment variable in this test case so that users master secret
     * won't be revealed accidentally.
     */
    @Test
    public void testDecryptWithEnv() {
        String secretText = "CRYPT:ebfab3ef4261185776a026acf72d24ee:5eb239d133b840503eedf9b7de490341";
        AutoAESSaltDecryptor decryptor = new AutoAESSaltDecryptor();
        String clearText = decryptor.decrypt(secretText);
        System.out.println("clearText = " + clearText);
    }
}
