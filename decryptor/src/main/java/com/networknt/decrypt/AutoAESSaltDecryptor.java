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
    /**
     * Default constructor for AutoAESSaltDecryptor.
     */
    public AutoAESSaltDecryptor() {
        super();
    }

    private final static String LIGHT_4J_CONFIG_PASSWORD = "light_4j_config_password";
    @Override
    protected char[] getPassword() {
        String passwordStr = getRuntimePassword();
        if (passwordStr == null || passwordStr.isEmpty()) {
            throw new IllegalStateException("No configuration master password supplied. Set light_4j_config_password explicitly.");
        }

        return ownedPassword(passwordStr.toCharArray());
    }

    String getRuntimePassword() {
        // Read the explicit light_4j_config_password system property first.
        String passwordStr = System.getProperty(LIGHT_4J_CONFIG_PASSWORD);
        if(passwordStr == null || passwordStr.isEmpty()) {
            passwordStr = System.getProperty(LIGHT_4J_CONFIG_PASSWORD.toUpperCase());
        }
        // The environment variable name can be in lower or upper case to be suitable for all operating systems.
        if(passwordStr == null || passwordStr.isEmpty()) {
            passwordStr = System.getenv(LIGHT_4J_CONFIG_PASSWORD);
        }
        if(passwordStr == null || passwordStr.isEmpty()) {
            passwordStr = System.getenv(LIGHT_4J_CONFIG_PASSWORD.toUpperCase());
        }
        return passwordStr;
    }

}
