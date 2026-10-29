package com.networknt.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import com.networknt.config.yml.DecryptConstructor;
import com.networknt.config.yml.YmlConstants;
import org.yaml.snakeyaml.resolver.Resolver;
import org.yaml.snakeyaml.Yaml;
import com.networknt.decrypt.AESSaltDecryptor;
import com.networknt.decrypt.MissingMasterPasswordProviderException;
import com.networknt.decrypt.fixture.FixturePasswordDecryptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PasswordProviderConfigTest {
    private static final String CRYPT = "CRYPT:e6e1b4a099902a2106509e87df909de4:cd9153addc755eb7e00d817c:e499d4501d0372fe37073d88a1025f1970a30cbd9acec7d5";
    @TempDir Path directory;

    @Test
    void configYmlRejectsBareBaseBeforeReadingAnyCryptValue() throws Exception {
        Config.getInstance(); // Initialize the shared instance with the normal test configuration.
        Files.writeString(directory.resolve("config.yml"), "decryptorClass: " + AESSaltDecryptor.class.getName());
        var failure = assertThrows(InvocationTargetException.class, this::newFileConfig);
        assertInstanceOf(MissingMasterPasswordProviderException.class, failure.getCause());
        assertTrue(failure.getCause().getMessage().contains("light_4j_config_password"));
    }

    @Test
    void explicitProviderLoadsCryptThroughRealFileConfig() throws Exception {
        Config.getInstance();
        Files.writeString(directory.resolve("config.yml"), "decryptorClass: " + FixturePasswordDecryptor.class.getName());
        Files.writeString(directory.resolve("provider-secret.yml"), "password: " + CRYPT);
        assertEquals("password", newFileConfig().getJsonMapConfig("provider-secret").get("password"));
    }

    @Test
    void subclassFallbackReportsProviderGuidanceAndSafeLocationAndLogsNoSecret() {
        Logger logger = (Logger) LoggerFactory.getLogger(AESSaltDecryptor.class);
        var appender = new ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            var reader = newReader(FallbackProvider.class.getName());
            var failure = assertThrows(IllegalStateException.class,
                    () -> reader.load("password: " + CRYPT));
            assertInstanceOf(MissingMasterPasswordProviderException.class, failure.getCause());
            assertTrue(failure.getMessage().contains("YAML line 1, column 11"));
            assertTrue(failure.getMessage().contains("No master-password provider configured"));
            assertFalse(failure.getMessage().contains("Check the master password"));
            assertFalse(failure.getMessage().contains(CRYPT));
            assertTrue(appender.list.stream().anyMatch(e -> e.getFormattedMessage().contains("No master-password provider configured")));
            assertTrue(appender.list.stream().noneMatch(e -> e.getFormattedMessage().contains(CRYPT)));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void malformedCryptKeepsFormatGuidanceWithoutEchoingSecret() {
        var reader = newReader(FixturePasswordDecryptor.class.getName());
        var failure = assertThrows(IllegalArgumentException.class,
                () -> reader.load("password: CRYPT:invalid-canary"));
        assertTrue(failure.getMessage().contains("Check the master password and CRYPT value format"));
        assertFalse(failure.getMessage().contains("invalid-canary"));
    }

    // Config exposes singleton access only; use a fresh instance so selections cannot
    // contaminate other tests or be hidden by a previously initialized YAML reader.
    private Config newFileConfig() throws Exception {
        String previous = System.getProperty(Config.LIGHT_4J_CONFIG_DIR);
        try {
            System.setProperty(Config.LIGHT_4J_CONFIG_DIR, directory.toString());
            var constructor = Class.forName("com.networknt.config.Config$FileConfigImpl").getDeclaredConstructor();
            constructor.setAccessible(true);
            return (Config) constructor.newInstance();
        } finally {
            if (previous == null) System.clearProperty(Config.LIGHT_4J_CONFIG_DIR);
            else System.setProperty(Config.LIGHT_4J_CONFIG_DIR, previous);
        }
    }

    private static Yaml newReader(String provider) {
        var resolver = new Resolver();
        resolver.addImplicitResolver(YmlConstants.CRYPT_TAG, YmlConstants.CRYPT_PATTERN, YmlConstants.CRYPT_FIRST);
        return new Yaml(DecryptConstructor.getInstance(provider), new org.yaml.snakeyaml.representer.Representer(new org.yaml.snakeyaml.DumperOptions()),
                new org.yaml.snakeyaml.DumperOptions(), resolver);
    }

    public static class FallbackProvider extends AESSaltDecryptor {
        @Override protected char[] getPassword() { return super.getPassword(); }
    }
}
