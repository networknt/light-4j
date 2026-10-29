package com.networknt.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import com.networknt.config.Config;
import com.networknt.test.IsolatedClasses;
import com.networknt.test.SystemPropertyScope;
import com.networknt.service.api.FatalServiceConfiguration;
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

/** Module-local scenarios run with fresh application classes regardless of the test runner. */
@org.junit.jupiter.api.parallel.ResourceLock(org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES)
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
        new IsolatedClasses(getClass().getClassLoader(), "com.networknt.").invokeStatic(
                ServiceStartupSupport.class.getName(), "run",
                new Class<?>[] {String.class, String.class, String.class, boolean.class, Path.class},
                binding, implementation, expectedCause == null ? null : expectedCause.getName(), fatal, directory);
    }

    public static void run(String binding, String implementation, String causeName, boolean fatal, Path directory) throws Exception {
        Class<?> expectedCause = causeName == null ? null : Class.forName(causeName);
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
        Logger factoryLogger = (Logger) LoggerFactory.getLogger("com.networknt.service.SingletonServiceFactory");
        var factoryLogs = new ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        factoryLogs.start();
        factoryLogger.addAppender(factoryLogs);
        try (var _ = new SystemPropertyScope(Config.LIGHT_4J_CONFIG_DIR, directory.toString());
             var _ = new SystemPropertyScope("light_4j_config_password", "light")) {
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
                assertTrue(stderr.contains(expectedCause.getName()), "Fatal startup failures must reach stderr before logging is ready");
                assertThrows(NoClassDefFoundError.class, () -> SingletonServiceFactory.getBean(Runnable.class));
            } else {
                assertTrue(stderr.contains(expectedCause.getName()), "Legacy startup stderr must contain the cause");
                // Ordinary configuration failures retain the historical partial-map behavior.
                assertNotNull(SingletonServiceFactory.getBean(Runnable.class));
            }
        } finally {
            factoryLogger.detachAppender(factoryLogs);
            factoryLogs.stop();
        }
    }

    public static class Before implements Runnable {
        public Before() { before = true; }
        @Override public void run() {
            // Construction-only fixture; the test never executes this Runnable.
        }
    }
    public static class After {
        public After() { after = true; }
    }
    public static class ConstructorFallback extends AESSaltDecryptor {
        public ConstructorFallback() { super.getPassword(); }
    }
    public static class FatalConfigurationException extends IllegalStateException implements FatalServiceConfiguration {
        public FatalConfigurationException() { super("Synthetic explicit fatal configuration"); }
    }
    public static class FatalSingleton implements Runnable {
        public FatalSingleton() { throw new FatalConfigurationException(); }
        @Override public void run() {
            // Construction-only fixture; the test never executes this Runnable.
        }
    }
    public static class ServiceLoaderSingleton implements Runnable {
        public ServiceLoaderSingleton() { throw new java.util.ServiceConfigurationError("Synthetic ordinary ServiceLoader failure"); }
        @Override public void run() {
            // Construction-only fixture; the test never executes this Runnable.
        }
    }
    public static class BrokenSingleton implements Runnable {
        public BrokenSingleton() { throw new IllegalArgumentException("Synthetic constructor failure"); }
        @Override public void run() {
            // Construction-only fixture; the test never executes this Runnable.
        }
    }
}
