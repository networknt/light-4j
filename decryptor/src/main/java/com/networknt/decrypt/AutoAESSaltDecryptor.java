package com.networknt.decrypt;

/**
 * This decryptor supports retrieving decrypted password of configuration files
 * from environment variables. If password cannot be found, a runtimeException
 * will be thrown.
 * <p>
 * To use this decryptor, adding the following line into config.yml
 * decryptorClass: com.networknt.decrypt.AutoAESSaltDecryptor
 *
 * The difference between this implementation and the AutoAESDecryptor is that
 * this one supports the dynamic salt and the salt will be part of the secret
 * to make the encryption stronger.
 */
public class AutoAESSaltDecryptor extends AESSaltDecryptor {
    private final static String LIGHT_4J_CONFIG_PASSWORD = "light_4j_config_password";

    @Override
    protected char[] getPassword() {
        String passwordStr = System.getenv(LIGHT_4J_CONFIG_PASSWORD);
        if (passwordStr == null || passwordStr.trim().equals("")) {
            throw new RuntimeException("Unable to retrieve decrypted password of configuration files from environment variables.");
        }
        return passwordStr.toCharArray();
    }

}
