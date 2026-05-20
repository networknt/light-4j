package com.networknt.decrypt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class AESDecryptorTest {

    @Test
    public void testConstructor() {
        AESSaltDecryptor decryptor = new AESSaltDecryptor();
        Assertions.assertNotNull(decryptor);
    }

    @Test
    public void testForName() {
        String decryptorClass = "com.networknt.decrypt.AESSaltDecryptor";
        try {
            Class<?> typeClass = Class.forName(decryptorClass);

            if (!typeClass.isInterface()) {
                Decryptor decryptor = (Decryptor) typeClass.getConstructor().newInstance();
                Assertions.assertNotNull(decryptor);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to construct the decryptor due to lack of decryption password.", e);
        }

    }

    /**
     * A test case that can be used as a utility to decrypt encrypted value based on the master secret or password.
     * The master secret will be loaded from the environment variable in this test case so that users master secret
     * won't be revealed accidentally.
     *
     * Here is the doc on how to use this test case as a utility. https://doc.networknt.com/concern/decryptor/
     */
    @Disabled
    @Test
    public void testDecryptWithEnv() {
        String secretText = "CRYPT:69c73ee9840e4d4f8b53115361236b52:aaf53a8a0efb269253fbb0a8:bbe1b8c479234458830998eeb39f78b6dbe038d5150f9f57";
        AutoAESSaltDecryptor decryptor = new AutoAESSaltDecryptor();
        String clearText = decryptor.decrypt(secretText);
        System.out.println("clearText = " + clearText);
    }
}
