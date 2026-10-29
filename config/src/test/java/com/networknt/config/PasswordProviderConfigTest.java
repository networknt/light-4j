package com.networknt.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import com.networknt.config.yml.YamlUtils;
import com.networknt.decrypt.AESSaltDecryptor;
import com.networknt.decrypt.MissingMasterPasswordProviderException;
import com.networknt.decrypt.AutoAESSaltDecryptor;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;


import static org.junit.jupiter.api.Assertions.*;

class PasswordProviderConfigTest {
    private static final String CRYPT = "CRYPT:e6e1b4a099902a2106509e87df909de4:cd9153addc755eb7e00d817c:e499d4501d0372fe37073d88a1025f1970a30cbd9acec7d5";

    @Test
    void subclassFallbackReportsProviderGuidanceAndSafeLocationAndLogsNoSecret() {
        Logger logger = (Logger) LoggerFactory.getLogger(AESSaltDecryptor.class);
        var appender = new ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            var reader = YamlUtils.newLoad(FallbackProvider.class.getName());
            var failure = assertThrows(YamlEngineException.class,
                    () -> reader.loadFromString("password: " + CRYPT));
            assertInstanceOf(IllegalStateException.class, failure.getCause());
            assertInstanceOf(MissingMasterPasswordProviderException.class, failure.getCause().getCause());
            assertTrue(failure.getMessage().contains("YAML line 1, column 11"));
            assertTrue(failure.getMessage().contains("No master-password provider configured"));
            assertFalse(failure.getMessage().contains("Check the master password"));
            assertFalse(failure.getMessage().contains(CRYPT));
            assertThrows(RuntimeException.class, () -> reader.loadFromString("password: " + CRYPT));
            assertEquals(1, appender.list.stream().filter(e -> e.getFormattedMessage().contains("No master-password provider configured")).count());
            assertTrue(appender.list.stream().noneMatch(e -> e.getFormattedMessage().contains(CRYPT)));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void malformedCryptKeepsFormatGuidanceWithoutEchoingSecret() {
        var reader = YamlUtils.newLoad(AutoAESSaltDecryptor.class.getName());
        var failure = assertThrows(YamlEngineException.class,
                () -> reader.loadFromString("password: CRYPT:invalid-canary"));
        assertTrue(failure.getMessage().contains("Check the master password and CRYPT value format"));
        assertFalse(failure.getMessage().contains("invalid-canary"));
    }

    @Test
    void constructionDiagnosticsIdentifyClassAndCauseTypeWithoutSecretMessage() {
        Logger logger = (Logger) LoggerFactory.getLogger(com.networknt.config.yml.DecryptConstructor.class);
        var appender = new ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThrows(IllegalStateException.class,
                    () -> com.networknt.config.yml.DecryptConstructor.create(BrokenProvider.class.getName()));
            assertThrows(IllegalStateException.class,
                    () -> com.networknt.config.yml.DecryptConstructor.create("missing.Provider"));
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
