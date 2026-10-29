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
import com.networknt.test.ForkedStartup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PasswordProviderConfigTest {
    private static final String CRYPT = "CRYPT:e6e1b4a099902a2106509e87df909de4:cd9153addc755eb7e00d817c:e499d4501d0372fe37073d88a1025f1970a30cbd9acec7d5";
    @TempDir Path directory;

    @Test
    void configYmlRejectsBareBaseBeforeReadingAnyCryptValue() throws Exception {
        Files.writeString(directory.resolve("config.yml"), "decryptorClass: " + AESSaltDecryptor.class.getName());
        var result = ForkedStartup.run(PasswordProviderConfigProbe.class, directory, "invalid");
        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("No master-password provider configured"));
    }

    @Test
    void explicitProviderLoadsCryptThroughRealFileConfig() throws Exception {
        Files.writeString(directory.resolve("config.yml"), "decryptorClass: " + FixturePasswordDecryptor.class.getName());
        Files.writeString(directory.resolve("provider-secret.yml"), "password: " + CRYPT);
        var result = ForkedStartup.run(PasswordProviderConfigProbe.class, directory, "valid");
        assertEquals(0, result.exitCode(), result.output());
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
            assertThrows(RuntimeException.class, () -> reader.load("password: " + CRYPT));
            assertEquals(1, appender.list.stream().filter(e -> e.getFormattedMessage().contains("No master-password provider configured")).count());
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

    private static Yaml newReader(String provider) {
        var resolver = new Resolver();
        resolver.addImplicitResolver(YmlConstants.CRYPT_TAG, YmlConstants.CRYPT_PATTERN, YmlConstants.CRYPT_FIRST);
        return new Yaml(DecryptConstructor.getInstance(provider), new org.yaml.snakeyaml.representer.Representer(new org.yaml.snakeyaml.DumperOptions()),
                new org.yaml.snakeyaml.DumperOptions(), resolver);
    }

    @Test
    void constructionDiagnosticsIdentifyClassAndCauseTypeWithoutSecretMessage() {
        Logger logger = (Logger) LoggerFactory.getLogger(com.networknt.config.yml.DecryptConstructor.class);
        var appender = new ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThrows(IllegalStateException.class,
                    () -> com.networknt.config.yml.DecryptConstructor.getInstance(BrokenProvider.class.getName()));
            assertThrows(IllegalStateException.class,
                    () -> com.networknt.config.yml.DecryptConstructor.getInstance("missing.Provider"));
            String messages = appender.list.stream().map(e -> e.getFormattedMessage()).collect(java.util.stream.Collectors.joining("\n"));
            assertTrue(messages.contains(BrokenProvider.class.getName()));
            assertTrue(messages.contains(UnsupportedOperationException.class.getName()));
            assertTrue(messages.contains("missing.Provider"));
            assertTrue(messages.contains(ClassNotFoundException.class.getName()));
            assertFalse(messages.contains("constructor-secret-canary"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    public static class BrokenProvider implements com.networknt.decrypt.Decryptor {
        public BrokenProvider() { throw new UnsupportedOperationException("constructor-secret-canary"); }
        @Override public String decrypt(String value) { return value; }
    }

    public static class FallbackProvider extends AESSaltDecryptor {
        @Override protected char[] getPassword() { return super.getPassword(); }
    }
}
