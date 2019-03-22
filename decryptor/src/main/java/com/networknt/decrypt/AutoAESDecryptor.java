package com.networknt.decrypt;

/**
 * This decryptor supports retrieving decrypted password of configuration files
 * from environment variables. If password cannot be found, a runtimeException
 * will be thrown.
 * <p>
 * To use this decryptor, adding the following line into config.yml
 * decryptorClass: com.networknt.decrypt.AutoAESDecryptor
 */
public class AutoAESDecryptor extends AESDecryptor {
    private final static String LIGHT_4J_CONFIG_PASSWORD = "light-4j-config-password";

    @Override
    protected char[] getPassword() {
        String passwordStr = System.getenv(LIGHT_4J_CONFIG_PASSWORD);
        if (passwordStr == null || passwordStr.trim().equals("")) {
            throw new RuntimeException("Unable to retrieve decrypted password of configuration files from environment variables.");
        }
        return passwordStr.toCharArray();
    }
}
