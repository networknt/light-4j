package com.networknt.config;

import com.networknt.config.yml.DecryptConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DecryptConstructorTest {
    @Test
    public void testConstructor() {
        DecryptConstructor constructor = DecryptConstructor.create();
        Assertions.assertNotNull(constructor);
    }

    @Test
    void rejectsBaseProviderWithActionableTopLevelMessage() {
        var failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> DecryptConstructor.create("com.networknt.decrypt.AESSaltDecryptor"));
        Assertions.assertTrue(failure.getMessage().contains("has no password provider"));
        Assertions.assertTrue(failure.getMessage().contains("AutoAESSaltDecryptor"));
    }

    @Test
    void distinguishesInterfacesAbstractClassesAndUnrelatedClasses() {
        var failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> DecryptConstructor.create("com.networknt.decrypt.Decryptor"));
        Assertions.assertTrue(failure.getMessage().contains("concrete implementation"));
        Assertions.assertFalse(failure.getMessage().contains("password"));
        failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> DecryptConstructor.create(AbstractProvider.class.getName()));
        Assertions.assertTrue(failure.getMessage().contains("concrete class"));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> DecryptConstructor.create(String.class.getName()));
    }

    @Test
    void preservesUnderlyingConstructorFailure() {
        var failure = Assertions.assertThrows(IllegalArgumentException.class,
                () -> DecryptConstructor.create(BrokenProvider.class.getName()));
        Assertions.assertInstanceOf(IllegalStateException.class, failure.getCause());
        Assertions.assertEquals("Constructor failure", failure.getCause().getMessage());
    }

    @Test
    void preservesUnrelatedProviderFailureWithoutSuggestingMissingPassword() {
        var yaml = com.networknt.config.yml.YamlUtils.newLoad(UnavailableProvider.class.getName());
        RuntimeException failure = Assertions.assertThrows(RuntimeException.class,
                () -> yaml.loadFromString("password: CRYPT:secret-canary\n"));
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
