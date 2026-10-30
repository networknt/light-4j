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
    void rejectsBaseProviderWithActionableTopLevelMessage() {
        var failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> DecryptConstructor.getInstance("com.networknt.decrypt.AESSaltDecryptor"));
        Assertions.assertTrue(failure.getMessage().contains("has no password provider"));
        Assertions.assertTrue(failure.getMessage().contains("AutoAESSaltDecryptor"));
    }

    @Test
    void distinguishesInterfacesAbstractClassesAndUnrelatedClasses() {
        var failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> DecryptConstructor.getInstance("com.networknt.decrypt.Decryptor"));
        Assertions.assertTrue(failure.getMessage().contains("concrete implementation"));
        Assertions.assertFalse(failure.getMessage().contains("password"));
        failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> DecryptConstructor.getInstance(AbstractProvider.class.getName()));
        Assertions.assertTrue(failure.getMessage().contains("concrete class"));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> DecryptConstructor.getInstance(String.class.getName()));
    }

    @Test
    void preservesUnderlyingConstructorFailure() {
        var failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> DecryptConstructor.getInstance(BrokenProvider.class.getName()));
        Assertions.assertInstanceOf(IllegalStateException.class, failure.getCause());
        Assertions.assertEquals("Constructor failure", failure.getCause().getMessage());
    }

    @Test
    void preservesUnrelatedProviderFailureWithoutSuggestingMissingPassword() {
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
        while (diagnostic.getCause() != null && !(diagnostic.getCause() instanceof IllegalStateException)) {
            diagnostic = diagnostic.getCause();
        }
        Assertions.assertTrue(diagnostic.getMessage().contains("YAML line 1"));
        Assertions.assertFalse(diagnostic.getMessage().contains("explicit master-password provider"));
        Assertions.assertFalse(diagnostic.getMessage().contains("secret-canary"));
        Assertions.assertInstanceOf(IllegalStateException.class, diagnostic.getCause());
        Assertions.assertEquals("Provider is closed", diagnostic.getCause().getMessage());
    }

    public static class UnavailableProvider implements com.networknt.decrypt.Decryptor {
        @Override
        public String decrypt(String input) {
            throw new IllegalStateException("Provider is closed");
        }
    }

    public abstract static class AbstractProvider implements com.networknt.decrypt.Decryptor {
    }

    public static class BrokenProvider extends UnavailableProvider {
        public BrokenProvider() {
            throw new IllegalStateException("Constructor failure");
        }
    }

}
