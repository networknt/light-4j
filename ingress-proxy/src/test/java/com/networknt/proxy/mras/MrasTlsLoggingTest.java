package com.networknt.proxy.mras;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Collections;
import javax.net.ssl.SSLContext;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MrasTlsLoggingTest {
    @Test
    void traceNeverExposesPasswordPrefixesAndAcceptsShortPasswords() throws Exception {
        MrasHandler handler = new MrasHandler();
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Logger logger = (Logger) LoggerFactory.getLogger(MrasHandler.class);
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
        try {
            Constructor<MrasConfig> constructor = MrasConfig.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            MrasConfig config = constructor.newInstance();
            // No files are opened: exercise the diagnostic statements even without TLS stores.
            config.keyStoreName = null;
            config.keyStorePass = "Q7z9-store-canary";
            config.keyPass = "R8y2-key-canary";
            Method initialize = MrasHandler.class.getDeclaredMethod("createSSLContext", MrasConfig.class);
            initialize.setAccessible(true);
            assertDoesNotThrow(() -> initialize.invoke(handler, config));
            assertFalse(appender.list.isEmpty(), "Capture must contain actual handler events");
            for (ILoggingEvent event : appender.list) {
                String details = event.getFormattedMessage() + Arrays.deepToString(event.getArgumentArray())
                        + event.getMDCPropertyMap()
                        + (event.getThrowableProxy() == null ? "" : ThrowableProxyUtil.asString(event.getThrowableProxy()));
                for (String canary : List.of("Q7z9", "R8y2")) {
                    assertFalse(details.contains(canary), "Credential prefix reached a log event");
                }
            }
            config.keyStorePass = "x";
            config.keyPass = "y";
            assertDoesNotThrow(() -> initialize.invoke(handler, config));
        } finally {
            root.detachAppender(appender);
            logger.detachAppender(appender);
            appender.stop();
            logger.setLevel(previous);
        }
    }
    @Test
    void realTlsStoreAndKeyPasswordsWorkAndFailuresNeverLeakCredentials() throws Exception {
        MrasHandler handler = new MrasHandler();
        Constructor<MrasConfig> constructor = MrasConfig.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        MrasConfig config = constructor.newInstance();
        Method initialize = MrasHandler.class.getDeclaredMethod("createSSLContext", MrasConfig.class);
        initialize.setAccessible(true);
        KeyStore store = KeyStore.getInstance("JKS");
        try (InputStream input = getClass().getResourceAsStream("/config/client.keystore")) {
            assertNotNull(input);
            store.load(input, "password".toCharArray());
        }
        String storePassword = "Q7z9-store-canary";
        String keyPassword = "R8y2-key-canary";
        for (String alias : Collections.list(store.aliases())) {
            if (store.isKeyEntry(alias)) {
                store.setKeyEntry(alias, store.getKey(alias, "password".toCharArray()),
                        keyPassword.toCharArray(), store.getCertificateChain(alias));
            }
        }
        // Put the transient fixture beside an existing test resource so Config's
        // actual classpath lookup finds it without changing global configuration.
        Path resources = Path.of(getClass().getResource("/config/client.truststore").toURI()).getParent();
        Path fixture = Files.createTempFile(resources, "mras-review-", ".keystore");
        Logger logger = (Logger) LoggerFactory.getLogger("com.networknt");
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
        try {
            try (OutputStream output = Files.newOutputStream(fixture)) {
                store.store(output, storePassword.toCharArray());
            }
            config.keyStoreName = fixture.getFileName().toString();
            config.keyStorePass = storePassword;
            config.keyPass = keyPassword;
            SSLContext context = (SSLContext) initialize.invoke(handler, config);
            assertNotNull(context.getSocketFactory(), "Both key and trust manager paths must initialize TLS");
            config.keyPass = "S6w3-incorrect-key-canary";
            assertThrows(InvocationTargetException.class, () -> initialize.invoke(handler, config));
            config.keyStorePass = "T5v4-incorrect-store-canary";
            assertThrows(InvocationTargetException.class, () -> initialize.invoke(handler, config));
            assertFalse(appender.list.isEmpty());
            for (ILoggingEvent event : appender.list) {
                String details = event.getFormattedMessage() + Arrays.deepToString(event.getArgumentArray())
                        + event.getMDCPropertyMap()
                        + (event.getThrowableProxy() == null ? "" : ThrowableProxyUtil.asString(event.getThrowableProxy()));
                for (String canary : List.of("Q7z9", "R8y2", "S6w3", "T5v4")) {
                    assertFalse(details.contains(canary), "TLS credential reached a log event");
                }
            }
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
            appender.stop();
            Files.deleteIfExists(fixture);
        }
    }

}
