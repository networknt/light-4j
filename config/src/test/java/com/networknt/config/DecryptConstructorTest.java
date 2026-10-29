package com.networknt.config;

import com.networknt.config.yml.DecryptConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DecryptConstructorTest {
    @Test
    public void testConstructor() {
        DecryptConstructor constructor = DecryptConstructor.getInstance();
        Assertions.assertNotNull(constructor);
    }

    @Test
    void rejectsAbstractProviderDuringConstruction() {
        RuntimeException failure = Assertions.assertThrows(RuntimeException.class,
                () -> DecryptConstructor.getInstance("com.networknt.decrypt.AESSaltDecryptor"));
        Assertions.assertTrue(failure.getMessage().contains("class and configuration"));
        Assertions.assertTrue(failure.getCause().getMessage().contains("concrete password-provider"));
    }

    @Test
    void reportsUnavailableProviderWithoutSuggestingWrongPassword() {
        var resolver = new org.yaml.snakeyaml.resolver.Resolver();
        resolver.addImplicitResolver(com.networknt.config.yml.YmlConstants.CRYPT_TAG,
                com.networknt.config.yml.YmlConstants.CRYPT_PATTERN,
                com.networknt.config.yml.YmlConstants.CRYPT_FIRST);
        var options = new org.yaml.snakeyaml.DumperOptions();
        var yaml = new org.yaml.snakeyaml.Yaml(
                DecryptConstructor.getInstance(UnavailableProvider.class.getName()),
                new org.yaml.snakeyaml.representer.Representer(options), options, resolver);
        RuntimeException failure = Assertions.assertThrows(RuntimeException.class,
                () -> yaml.load("password: CRYPT:secret-canary\n"));
        Throwable diagnostic = failure;
        while (diagnostic.getCause() != null && !(diagnostic instanceof IllegalStateException)) {
            diagnostic = diagnostic.getCause();
        }
        Assertions.assertInstanceOf(IllegalStateException.class, diagnostic);
        Assertions.assertTrue(diagnostic.getMessage().contains("explicit master-password provider"));
        Assertions.assertTrue(diagnostic.getMessage().contains("YAML line 1"));
        Assertions.assertFalse(diagnostic.getMessage().contains("Check the master password"));
        Assertions.assertFalse(diagnostic.getMessage().contains("secret-canary"));
        Assertions.assertInstanceOf(IllegalStateException.class, diagnostic.getCause());
    }

    public static class UnavailableProvider implements com.networknt.decrypt.Decryptor {
        @Override
        public String decrypt(String input) {
            throw new IllegalStateException("No test provider available");
        }
    }
}
