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
    // All junit tests configuration are using this password to encrypt sensitive info in config files. This Decryptor
    // can detect if the current thread is started by the JUnit test case so that default password is going to be used.
    // In this way, all other developers can run the build locally without providing the light_4j_config_password as an
    // environment variable in the .profile or .bashrc file.
    private final static String DEFAULT_JUNIT_TEST_PASSWORD = "light";

    static char[] password = null;

    @Override
    protected char[] getPassword() {
        if(password != null) {
            // password is cached at the class level as a static variable. Once it is resolve, it won't be retrieved again.
            return password;
        } else {
            // get from the -Dlight-4j-config-password Java command line option.
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
            if (passwordStr == null || passwordStr.isEmpty()) {
                // we cannot get the password from the environment, check if we are in the JUnit tests. If it is we can use the default password "light"
                // as all test cases are using it to encrypt secret in config files.
                if(isJUnitTest()) {
                    passwordStr = DEFAULT_JUNIT_TEST_PASSWORD;
                } else {
                    throw new RuntimeException("Unable to retrieve decrypted password of configuration files from environment variables.");
                }
            }
            password = passwordStr.toCharArray();
            return password;
        }
    }

    public static boolean isJUnitTest() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (element.getClassName().startsWith("org.junit.")) {
                return true;
            }
        }
        return false;
    }
}
