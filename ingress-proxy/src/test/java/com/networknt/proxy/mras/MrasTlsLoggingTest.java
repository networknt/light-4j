package com.networknt.proxy.mras;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
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
}
