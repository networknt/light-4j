package com.networknt.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMixedRepresentationTest {
    @TempDir Path directory;

    @Test
    void mixedReadsKeepBothIdentitiesUntilInvalidated() throws Exception {
        for (boolean defaults : new boolean[]{false, true}) {
            for (boolean mapFirst : new boolean[]{false, true}) {
                Config config = freshConfig();
                if (mapFirst) map(config, defaults); else object(config, defaults);
                Map<String, Object> mapped = map(config, defaults);
                TestConfig typed = (TestConfig) object(config, defaults);
                assertEquals(mapped.get("value"), typed.getValue());
                assertSame(mapped, map(config, defaults));
                assertSame(typed, object(config, defaults));
                assertSame(mapped, config.getJsonObjectConfig("test", Map.class));
                assertSame(mapped, map(config, !defaults));
                assertSame(typed, object(config, !defaults));
                assertNotSame(typed, config.getJsonObjectConfigNoCache("test", TestConfig.class));
                config.clearConfigCache("test");
                assertNotSame(mapped, map(config, defaults));
                assertNotSame(typed, object(config, defaults));
            }
        }
    }

    @Test
    void typedReadUsesCachedSnapshotEvenAfterFileBecomesInvalid() throws Exception {
        Files.writeString(directory.resolve("snapshot.yml"), "value: first\n");
        Config config = freshConfig();
        Map<String, Object> map = config.getJsonMapConfig("snapshot");
        Files.writeString(directory.resolve("snapshot.yml"), "value: [invalid\n");
        TestConfig object = (TestConfig) config.getJsonObjectConfig("snapshot", TestConfig.class);
        assertEquals("first", object.getValue());
        assertSame(object, config.getJsonObjectConfig("snapshot", TestConfig.class));
        assertSame(map, config.getJsonMapConfig("snapshot"));
        assertThrows(RuntimeException.class, () -> config.getJsonObjectConfigNoCache("snapshot", TestConfig.class));
        Files.writeString(directory.resolve("snapshot.yml"), "value: second\n");
        config.clearConfigCache("snapshot");
        assertEquals("second", ((TestConfig) config.getJsonObjectConfig("snapshot", TestConfig.class)).getValue());
        assertNotSame(map, config.getJsonMapConfig("snapshot"));
    }

    @Test
    void replacementAndClearInvalidateEveryRepresentation() throws Exception {
        Config config = freshConfig();
        Map<String, Object> injected = new HashMap<>(Map.of("value", "injected"));
        config.putInConfigCache("test", injected);
        TestConfig typed = (TestConfig) object(config, false);
        assertEquals("injected", typed.getValue());
        injected.put("value", "mutated");
        assertSame(injected, map(config, false));
        assertEquals("mutated", map(config, false).get("value"));
        assertEquals("injected", typed.getValue()); // POJOs are snapshots, not live map views.
        config.putInConfigCache("test", new HashMap<>(Map.of("value", "replacement")));
        assertEquals("replacement", ((TestConfig) object(config, false)).getValue());
        TestConfig replacement = (TestConfig) object(config, false);
        config.clear();
        assertNotSame(replacement, object(config, false));
        config.putInConfigCache("test", typed);
        assertSame(typed, object(config, false));
        assertNotNull(map(config, false));
    }

    @Test
    void classLoaderSwapInvalidatesBothRepresentations() throws Exception {
        Config config = freshConfig();
        Object typed = object(config, false);
        Object mapped = map(config, false);
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[0], getClass().getClassLoader())) {
            config.setClassLoader(loader);
            assertNotSame(typed, object(config, false));
            assertNotSame(mapped, map(config, false));
        }
    }

    @Test
    void identicalClassNamesFromDifferentLoadersHaveSeparateEntries() throws Exception {
        Config config = freshConfig();
        String name = TestConfig.class.getName();
        byte[] bytes;
        try (var input = TestConfig.class.getResourceAsStream("TestConfig.class")) {
            bytes = input.readAllBytes();
        }
        ClassLoader firstLoader = new ClassLoader(null) {
            @Override protected Class<?> findClass(String requested) throws ClassNotFoundException {
                if (!name.equals(requested)) throw new ClassNotFoundException(requested);
                return defineClass(requested, bytes, 0, bytes.length);
            }
        };
        ClassLoader secondLoader = new ClassLoader(null) {
            @Override protected Class<?> findClass(String requested) throws ClassNotFoundException {
                if (!name.equals(requested)) throw new ClassNotFoundException(requested);
                return defineClass(requested, bytes, 0, bytes.length);
            }
        };
        Class<?> firstType = firstLoader.loadClass(name);
        Class<?> secondType = secondLoader.loadClass(name);
        Object first = config.getJsonObjectConfig("test", firstType);
        Object second = config.getJsonObjectConfig("test", secondType);
        assertNotSame(firstType, secondType);
        assertEquals(firstType, first.getClass());
        assertEquals(secondType, second.getClass());
        assertSame(first, config.getJsonObjectConfig("test", firstType));
        assertSame(second, config.getJsonObjectConfig("test", secondType));
    }

    @Test
    void customLoaderBindingAndDefaultSharingArePreserved() throws Exception {
        Files.writeString(directory.resolve("custom.yml"), "value: default\n");
        for (boolean defaultsFirst : new boolean[]{false, true}) {
            Config config = customConfig();
            Object typed = defaultsFirst ? config.getDefaultJsonObjectConfig("custom", TestConfig.class)
                    : config.getJsonObjectConfig("custom", TestConfig.class);
            Map<String, Object> mapped = defaultsFirst ? config.getDefaultJsonMapConfig("custom")
                    : config.getJsonMapConfig("custom");
            assertEquals(defaultsFirst ? "default" : "object", ((TestConfig) typed).getValue());
            assertEquals(defaultsFirst ? "default" : "map", mapped.get("value"));
            assertSame(typed, config.getJsonObjectConfig("custom", TestConfig.class));
            assertSame(typed, config.getDefaultJsonObjectConfig("custom", TestConfig.class));
            assertSame(mapped, config.getJsonMapConfig("custom"));
            assertSame(mapped, config.getDefaultJsonMapConfig("custom"));
            assertSame(mapped, config.getJsonObjectConfig("custom", Map.class));
        }
    }

    @Test
    void wrongLoaderTypeIsRejectedWithoutPoisoningCache() throws Exception {
        Config config = customConfig();
        ControlledLoader.wrongType = true;
        try {
            assertThrows(ConfigException.class, () -> config.getJsonObjectConfig("custom", TestConfig.class));
        } finally {
            ControlledLoader.wrongType = false;
        }
        assertInstanceOf(TestConfig.class, config.getJsonObjectConfig("custom", TestConfig.class));
    }

    @Test
    void concurrentLoadsConvergeOnOneIdentity() throws Exception {
        Config config = customConfig();
        ControlledLoader.objectLoads.set(0);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<Object>> results = new ArrayList<>();
            for (int i = 0; i < 40; i++) results.add(pool.submit(() -> config.getJsonObjectConfig("custom", TestConfig.class)));
            Object first = results.get(0).get(10, TimeUnit.SECONDS);
            for (Future<Object> result : results) assertSame(first, result.get(10, TimeUnit.SECONDS));
            assertEquals(1, ControlledLoader.objectLoads.get());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void invalidationDoesNotWaitForOrRepublishAnOldLoad() throws Exception {
        for (int operation = 0; operation < 4; operation++) {
            Config config = customConfig();
            ControlledLoader.started = new CountDownLatch(1);
            ControlledLoader.release = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                Future<Object> old = pool.submit(() -> config.getJsonObjectConfig("blocked", TestConfig.class));
                assertTrue(ControlledLoader.started.await(5, TimeUnit.SECONDS));
                // Loading another name must not wait behind the blocked load.
                assertNotNull(pool.submit(() -> config.getJsonMapConfig("other")).get(5, TimeUnit.SECONDS));
                final int action = operation;
                TestConfig injected = new TestConfig();
                injected.setValue("replacement");
                pool.submit(() -> {
                    if (action == 0) config.clearConfigCache("blocked");
                    if (action == 1) config.clear();
                    if (action == 2) config.putInConfigCache("blocked", injected);
                    if (action == 3) config.setClassLoader(getClass().getClassLoader());
                }).get(5, TimeUnit.SECONDS);
                ControlledLoader.release.countDown();
                Object detached = old.get(5, TimeUnit.SECONDS);
                Object current = config.getJsonObjectConfig("blocked", TestConfig.class);
                assertNotSame(detached, current);
                if (action == 2) assertSame(injected, current);
                assertSame(current, config.getJsonObjectConfig("blocked", TestConfig.class));
            } finally {
                ControlledLoader.release.countDown();
                pool.shutdownNow();
            }
        }
    }

    @Test
    void exclusionsKeepDirectBindingAndCacheTheResult() throws Exception {
        Path file = directory.resolve("test_exclusion.yml");
        Files.writeString(file, "value: first\n");
        Config config = freshConfig();
        Map<String, Object> map = config.getJsonMapConfig("test_exclusion");
        Files.writeString(file, "value: second\n");
        TestConfig typed = (TestConfig) config.getJsonObjectConfig("test_exclusion", TestConfig.class);
        assertEquals("second", typed.getValue()); // Exclusions retain the separate loadAs path.
        Files.writeString(file, "value: [invalid\n");
        assertSame(typed, config.getJsonObjectConfig("test_exclusion", TestConfig.class));
        assertSame(map, config.getJsonMapConfig("test_exclusion"));
        assertEquals("first", map.get("value"));
    }

    @Test
    void invalidationMatchesExactNamesWithoutPrefixCollisions() throws Exception {
        Config config = freshConfig();
        String otherName = "test#" + TestConfig.class.getName();
        config.putInConfigCache("test", new HashMap<>(Map.of("value", "one")));
        config.putInConfigCache(otherName, new HashMap<>(Map.of("value", "two")));
        Object other = config.getJsonObjectConfig(otherName, TestConfig.class);
        object(config, false);
        config.clearConfigCache("test");
        assertSame(other, config.getJsonObjectConfig(otherName, TestConfig.class));
    }

    @Test
    void concurrentUncachedObjectParsingCoversExclusionsToo() throws Exception {
        Files.writeString(directory.resolve("test_exclusion.yml"), "value: '${unexpanded}'\n");
        Config config = freshConfig();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> work = new ArrayList<>();
            for (int i = 0; i < 8; i++) work.add(() -> {
                for (int j = 0; j < 50; j++) {
                    TestConfig typed = (TestConfig) config.getJsonObjectConfigNoCache("test_exclusion", TestConfig.class);
                    assertEquals("${unexpanded}", typed.getValue());
                    assertNotNull(config.getJsonObjectConfigNoCache("test", TestConfig.class));
                    assertNotNull(config.getJsonMapConfigNoCache("test"));
                }
                return null;
            });
            for (Future<Void> result : pool.invokeAll(work, 20, TimeUnit.SECONDS)) result.get(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
    }

    private Config customConfig() throws Exception {
        Files.writeString(directory.resolve("config.yml"), "configLoaderClass: " + ControlledLoader.class.getName() + "\n");
        return freshConfig();
    }

    private Config freshConfig() throws Exception {
        String previous = System.getProperty(Config.LIGHT_4J_CONFIG_DIR);
        try {
            System.setProperty(Config.LIGHT_4J_CONFIG_DIR, directory.toString());
            // Test the built-in implementation, regardless of a ServiceLoader override.
            var constructor = Class.forName("com.networknt.config.Config$FileConfigImpl").getDeclaredConstructor();
            constructor.setAccessible(true);
            return (Config) constructor.newInstance();
        } finally {
            if (previous == null) System.clearProperty(Config.LIGHT_4J_CONFIG_DIR);
            else System.setProperty(Config.LIGHT_4J_CONFIG_DIR, previous);
        }
    }

    private Map<String, Object> map(Config config, boolean defaults) {
        return defaults ? config.getDefaultJsonMapConfig("test") : config.getJsonMapConfig("test");
    }

    private Object object(Config config, boolean defaults) {
        return defaults ? config.getDefaultJsonObjectConfig("test", TestConfig.class)
                : config.getJsonObjectConfig("test", TestConfig.class);
    }

    public static class ControlledLoader implements ConfigLoader {
        static volatile boolean wrongType;
        static volatile CountDownLatch started;
        static volatile CountDownLatch release;
        static final AtomicInteger objectLoads = new AtomicInteger();

        public Map<String, Object> loadMapConfig(String name, String path) {
            return new HashMap<>(Map.of("value", "map"));
        }
        public Map<String, Object> loadMapConfig(String name) { return loadMapConfig(name, ""); }
        public <T> Object loadObjectConfig(String name, Class<T> type, String path) {
            objectLoads.incrementAndGet();
            if ("blocked".equals(name)) {
                started.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
            if (wrongType) return Map.of("value", "wrong");
            TestConfig value = new TestConfig();
            value.setValue("object");
            return value;
        }
        public <T> Object loadObjectConfig(String name, Class<T> type) { return loadObjectConfig(name, type, ""); }
    }
}
