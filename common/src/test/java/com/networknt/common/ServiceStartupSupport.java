package com.networknt.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import com.networknt.config.Config;
import com.networknt.decrypt.AESSaltDecryptor;
import com.networknt.decrypt.Decryptor;
import com.networknt.service.SingletonServiceFactory;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Module-local fixtures; Surefire isolates each concrete startup test class in a fresh JVM. */
public abstract class ServiceStartupSupport {
    @TempDir Path directory;
    private static boolean before;
    private static boolean after;

    protected void check(String binding, String implementation, Class<? extends Throwable> expectedCause) throws Exception {
        check(binding, implementation, expectedCause, true);
    }

    protected void checkLegacy(String binding, String implementation, Class<? extends Throwable> expectedCause) throws Exception {
        check(binding, implementation, expectedCause, false);
    }

    private void check(String binding, String implementation, Class<? extends Throwable> expectedCause, boolean fatal) throws Exception {
        assertTrue(System.getenv("light_4j_config_password") == null, "Runtime password environment must be excluded");
        assertTrue(System.getenv("LIGHT_4J_CONFIG_PASSWORD") == null, "Runtime password environment must be excluded");
        Files.writeString(directory.resolve("config.yml"), "decryptorClass: com.networknt.decrypt.AutoAESSaltDecryptor\nallowDefaultValueEmpty: true\n");
        Files.writeString(directory.resolve("service.yml"), """
                singletons:
                - java.lang.Runnable:
                  - com.networknt.common.ServiceStartupSupport$Before
                - %s:
                  - %s
                - java.lang.Object:
                  - com.networknt.common.ServiceStartupSupport$After
                """.formatted(binding, implementation));
        String previous = System.getProperty(Config.LIGHT_4J_CONFIG_DIR);
        String previousPassword = System.getProperty("light_4j_config_password");
        Logger factoryLogger = (Logger) LoggerFactory.getLogger("com.networknt.service.SingletonServiceFactory");
        var factoryLogs = new ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        factoryLogs.start();
        factoryLogger.addAppender(factoryLogs);
        try {
            if (expectedCause == null) System.setProperty("light_4j_config_password", "light");
            System.setProperty(Config.LIGHT_4J_CONFIG_DIR, directory.toString());
            if (expectedCause == null) {
                assertNotNull(SingletonServiceFactory.getBean(Decryptor.class));
                assertTrue(before);
                assertTrue(after);
                var map = new HashMap<String, Object>(Map.of("secret",
                        "CRYPT:e6e1b4a099902a2106509e87df909de4:cd9153addc755eb7e00d817c:e499d4501d0372fe37073d88a1025f1970a30cbd9acec7d5"));
                DecryptUtil.decryptMap(map);
                assertEquals("password", map.get("secret"));
                return;
            }
            PrintStream original = System.err;
            var captured = new ByteArrayOutputStream();
            ExceptionInInitializerError failure = null;
            try (var stderr = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
                System.setErr(stderr);
                if (fatal) {
                    failure = assertThrows(ExceptionInInitializerError.class,
                            () -> SingletonServiceFactory.getBean(Object.class));
                } else {
                    assertNotNull(SingletonServiceFactory.getBean(Runnable.class));
                    assertNull(SingletonServiceFactory.getBean(Object.class));
                }
            } finally {
                System.setErr(original);
            }
            String stderr = captured.toString(StandardCharsets.UTF_8);
            assertTrue(factoryLogs.list.stream().noneMatch(event -> event.getLevel().isGreaterOrEqual(Level.ERROR)),
                    "The factory must not log a duplicate error stack");
            assertTrue(before);
            assertFalse(after);
            if (fatal) {
                assertInstanceOf(IllegalStateException.class, failure.getCause());
                assertEquals("Unable to initialize singleton services.", failure.getCause().getMessage());
                boolean found = false;
                for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                    if (expectedCause.isInstance(cause)) found = true;
                }
                assertTrue(found, "Missing expected fatal cause");
                assertFalse(stderr.contains("at com.networknt.service.SingletonServiceFactory"),
                        "The factory must not print a propagated fatal stack trace");
                assertThrows(NoClassDefFoundError.class, () -> SingletonServiceFactory.getBean(Runnable.class));
            } else {
                assertTrue(stderr.contains(expectedCause.getName()), "Legacy startup stderr must contain the cause");
                // Ordinary configuration failures retain the historical partial-map behavior.
                assertNotNull(SingletonServiceFactory.getBean(Runnable.class));
            }
        } finally {
            factoryLogger.detachAppender(factoryLogs);
            factoryLogs.stop();
            if (previousPassword == null) System.clearProperty("light_4j_config_password");
            else System.setProperty("light_4j_config_password", previousPassword);
            if (previous == null) System.clearProperty(Config.LIGHT_4J_CONFIG_DIR);
            else System.setProperty(Config.LIGHT_4J_CONFIG_DIR, previous);
        }
    }

    public static class Before implements Runnable {
        public Before() { before = true; }
        @Override public void run() { }
    }
    public static class After {
        public After() { after = true; }
    }
    public static class ConstructorFallback extends AESSaltDecryptor {
        public ConstructorFallback() { super.getPassword(); }
    }
    public static class FatalSingleton implements Runnable {
        public FatalSingleton() { throw new java.util.ServiceConfigurationError("Synthetic fatal configuration"); }
        @Override public void run() { }
    }
    public static class BrokenSingleton implements Runnable {
        public BrokenSingleton() { throw new IllegalArgumentException("Synthetic constructor failure"); }
        @Override public void run() { }
    }
}
