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

    @Test
    void customMapObjectReadsPreserveFirstReaderDispatch() throws Exception {
        Config config = customConfig();
        Map<?, ?> objectMap = (Map<?, ?>) config.getJsonObjectConfig("custom", Map.class);
        assertEquals("object", objectMap.get("value"));
        assertSame(objectMap, config.getJsonMapConfig("custom"));
        config.clearConfigCache("custom");
        Map<?, ?> rawMap = config.getJsonMapConfig("custom");
        assertEquals("map", rawMap.get("value"));
        assertSame(rawMap, config.getJsonObjectConfig("custom", Map.class));
    }

    @Test
    void derivedBindingFailureNamesTheConfigurationAndKeepsCause() throws Exception {
        Config config = freshConfig();
        config.putInConfigCache("bad-binding", Map.of("unknownProperty", "value"));
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> config.getJsonObjectConfig("bad-binding", TestConfig.class));
        assertTrue(failure.getMessage().contains("bad-binding"));
        assertInstanceOf(IllegalArgumentException.class, failure.getCause());
    }

    @Test
    void classLoaderSwapRetainsInjectionsButRebuildsDerivedViews() throws Exception {
        Config config = freshConfig();
        Map<String, Object> map = new HashMap<>(Map.of("value", "injected"));
        TestConfig injectedPojo = new TestConfig();
        config.putInConfigCache("only-in-memory", map);
        config.putInConfigCache("injected-pojo", injectedPojo);
        Object derived = config.getJsonObjectConfig("only-in-memory", TestConfig.class);
        config.setClassLoader(getClass().getClassLoader());
        assertSame(map, config.getJsonMapConfig("only-in-memory"));
        assertSame(injectedPojo, config.getJsonObjectConfig("injected-pojo", TestConfig.class));
        Object rebuilt = config.getJsonObjectConfig("only-in-memory", TestConfig.class);
        assertNotSame(derived, rebuilt);
        assertEquals("injected", ((TestConfig) rebuilt).getValue());
    }

    @Test
    void missingAndFailedLoadsDoNotRetainEmptyEntries() throws Exception {
        Config config = freshConfig();
        var field = config.getClass().getSuperclass().getDeclaredField("configCache");
        field.setAccessible(true);
        Map<?, ?> entries = (Map<?, ?>) field.get(config);
        for (int i = 0; i < 10; i++) {
            String name = "missing-config-" + i;
            assertNull(config.getJsonMapConfig(name));
            assertNull(config.getJsonObjectConfig(name, TestConfig.class));
            assertFalse(entries.containsKey(name));
        }
        Files.writeString(directory.resolve("invalid.yml"), "value: [invalid\n");
        assertThrows(RuntimeException.class, () -> config.getJsonMapConfig("invalid"));
        assertFalse(entries.containsKey("invalid"));
        Files.writeString(directory.resolve("missing-config-0.yml"), "value: appears\n");
        Map<?, ?> found = config.getJsonMapConfig("missing-config-0");
        assertEquals("appears", found.get("value"));
        assertSame(found, config.getJsonMapConfig("missing-config-0"));
    }

    @Test
    void aMissingLoadDoesNotDetachReadersWaitingToRetry() throws Exception {
        Files.writeString(directory.resolve("config.yml"), "configLoaderClass: " + RetryingLoader.class.getName() + "\n");
        Config config = freshConfig();
        RetryingLoader.calls.set(0);
        RetryingLoader.firstEntered = new CountDownLatch(1);
        RetryingLoader.firstRelease = new CountDownLatch(1);
        RetryingLoader.secondEntered = new CountDownLatch(1);
        RetryingLoader.secondRelease = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(3);
        var secondThread = new java.util.concurrent.atomic.AtomicReference<Thread>();
        var thirdThread = new java.util.concurrent.atomic.AtomicReference<Thread>();
        try {
            Future<?> first = pool.submit(() -> config.getJsonMapConfig("retry"));
            assertTrue(RetryingLoader.firstEntered.await(5, TimeUnit.SECONDS));
            Future<?> second = pool.submit(() -> {
                secondThread.set(Thread.currentThread());
                return config.getJsonMapConfig("retry");
            });
            awaitBlocked(secondThread);
            RetryingLoader.firstRelease.countDown();
            assertNull(first.get(5, TimeUnit.SECONDS));
            assertTrue(RetryingLoader.secondEntered.await(5, TimeUnit.SECONDS));
            Future<?> third = pool.submit(() -> {
                thirdThread.set(Thread.currentThread());
                return config.getJsonMapConfig("retry");
            });
            awaitBlocked(thirdThread);
            RetryingLoader.secondRelease.countDown();
            Object value = second.get(5, TimeUnit.SECONDS);
            assertSame(value, third.get(5, TimeUnit.SECONDS));
            assertSame(value, config.getJsonMapConfig("retry"));
            assertEquals(2, RetryingLoader.calls.get());
        } finally {
            RetryingLoader.firstRelease.countDown();
            RetryingLoader.secondRelease.countDown();
            pool.shutdownNow();
        }
    }

    private void awaitBlocked(java.util.concurrent.atomic.AtomicReference<Thread> thread) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while ((thread.get() == null || thread.get().getState() != Thread.State.BLOCKED)
                && System.nanoTime() < deadline) {
            java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
        }
        assertNotNull(thread.get());
        assertEquals(Thread.State.BLOCKED, thread.get().getState());
    }

    public static class RetryingLoader extends ControlledLoader {
        static final AtomicInteger calls = new AtomicInteger();
        static CountDownLatch firstEntered;
        static CountDownLatch firstRelease;
        static CountDownLatch secondEntered;
        static CountDownLatch secondRelease;

        @Override public Map<String, Object> loadMapConfig(String name, String path) {
            int call = calls.incrementAndGet();
            CountDownLatch entered = call == 1 ? firstEntered : secondEntered;
            CountDownLatch release = call == 1 ? firstRelease : secondRelease;
            entered.countDown();
            try {
                if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            return call == 1 ? null : new HashMap<>(Map.of("value", "retry"));
        }
    }

    @Test
    void injectedHitsDoNotConstructTheCustomLoader() throws Exception {
        Files.writeString(directory.resolve("config.yml"), "configLoaderClass: " + ConstructorReadingLoader.class.getName() + "\n");
        Config config = freshConfig();
        ConstructorReadingLoader.constructions.set(0);
        Map<String, Object> map = Map.of("value", "injected");
        TestConfig typed = new TestConfig();
        config.putInConfigCache("map-hit", map);
        config.putInConfigCache("object-hit", typed);
        assertSame(map, config.getJsonMapConfig("map-hit"));
        assertSame(map, config.getJsonObjectConfig("map-hit", Map.class));
        assertSame(typed, config.getJsonObjectConfig("object-hit", TestConfig.class));
        assertEquals(0, ConstructorReadingLoader.constructions.get());
    }

    @Test
    void loaderConstructorCanReadCachedSettingsWhileAnotherNameLoads() throws Exception {
        Files.writeString(directory.resolve("config.yml"), "configLoaderClass: " + ConstructorReadingLoader.class.getName() + "\n");
        Files.writeString(directory.resolve("settings.yml"), "value: bootstrap\n");
        Files.writeString(directory.resolve("other-settings.yml"), "value: recursive-bootstrap\n");
        Config config = freshConfig();
        ConstructorReadingLoader.target = config;
        config.putInConfigCache("injected-settings", Map.of("value", "in-memory"));
        ConstructorReadingLoader.entered = new CountDownLatch(1);
        ConstructorReadingLoader.proceed = new CountDownLatch(1);
        ConstructorReadingLoader.constructions.set(0);
        ExecutorService pool = Executors.newFixedThreadPool(2, work -> {
            Thread thread = new Thread(work, "config-constructor-regression");
            thread.setDaemon(true); // A regression must fail the test, not hang the test JVM.
            return thread;
        });
        var secondThread = new java.util.concurrent.atomic.AtomicReference<Thread>();
        try {
            Future<?> first = pool.submit(() -> config.getJsonMapConfig("first"));
            assertTrue(ConstructorReadingLoader.entered.await(5, TimeUnit.SECONDS));
            Future<?> second = pool.submit(() -> {
                secondThread.set(Thread.currentThread());
                return config.getJsonMapConfig("settings");
            });
            awaitBlocked(secondThread);
            ConstructorReadingLoader.proceed.countDown();
            assertNotNull(first.get(5, TimeUnit.SECONDS));
            assertNotNull(second.get(5, TimeUnit.SECONDS));
            assertEquals(1, ConstructorReadingLoader.constructions.get());
            assertEquals("map", config.getJsonMapConfig("settings").get("value"));
            assertEquals("map", config.getJsonMapConfig("other-settings").get("value"));
        } finally {
            ConstructorReadingLoader.proceed.countDown();
            pool.shutdownNow();
            ConstructorReadingLoader.target = null;
        }
    }

    public static class ConstructorReadingLoader extends ControlledLoader {
        static Config target;
        static CountDownLatch entered;
        static CountDownLatch proceed;
        static final AtomicInteger constructions = new AtomicInteger();

        public ConstructorReadingLoader() {
            constructions.incrementAndGet();
            entered.countDown();
            try {
                if (!proceed.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            assertEquals("in-memory", target.getDefaultJsonMapConfig("injected-settings").get("value"));
            assertEquals("bootstrap", target.getDefaultJsonMapConfig("settings").get("value"));
            assertEquals("recursive-bootstrap", target.getJsonMapConfig("other-settings").get("value"));
        }
    }

    @Test
    void mutuallyReferencingLoaderCallsDoNotAcquireEachOthersEntryLocks() throws Exception {
        for (boolean objects : new boolean[]{false, true}) {
            Files.writeString(directory.resolve("config.yml"), "configLoaderClass: " + CrossReadingLoader.class.getName() + "\n");
            Config config = freshConfig();
            CrossReadingLoader.target = config;
            CrossReadingLoader.objects = objects;
            CrossReadingLoader.barrier = new CyclicBarrier(2);
            config.getJsonMapConfig("warmup"); // Construct the loader before testing its callbacks.
            ExecutorService pool = Executors.newFixedThreadPool(2, work -> {
                Thread thread = new Thread(work, "config-cross-read-regression");
                thread.setDaemon(true);
                return thread;
            });
            try {
                Future<?> left = pool.submit(() -> CrossReadingLoader.read("left"));
                Future<?> right = pool.submit(() -> CrossReadingLoader.read("right"));
                Object leftValue = left.get(5, TimeUnit.SECONDS);
                Object rightValue = right.get(5, TimeUnit.SECONDS);
                assertSame(leftValue, CrossReadingLoader.read("left"));
                assertSame(rightValue, CrossReadingLoader.read("right"));
                if (objects) {
                    assertEquals("left-root", ((TestConfig) leftValue).getValue());
                    assertEquals("right-root", ((TestConfig) rightValue).getValue());
                } else {
                    assertEquals("left-root", ((Map<?, ?>) leftValue).get("value"));
                    assertEquals("right-root", ((Map<?, ?>) rightValue).get("value"));
                }
            } finally {
                pool.shutdownNow();
            }
        }
    }

    public static class CrossReadingLoader extends ControlledLoader {
        static Config target;
        static boolean objects;
        static CyclicBarrier barrier;
        private final ThreadLocal<Boolean> nested = new ThreadLocal<>();

        static Object read(String name) {
            return objects ? target.getJsonObjectConfig(name, TestConfig.class) : target.getJsonMapConfig(name);
        }

        private String value(String name) {
            if (!"left".equals(name) && !"right".equals(name)) return name;
            if (Boolean.TRUE.equals(nested.get())) return name + "-nested";
            nested.set(true);
            try {
                barrier.await(5, TimeUnit.SECONDS);
                assertNotNull(read("left".equals(name) ? "right" : "left"));
                return name + "-root";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } catch (BrokenBarrierException | TimeoutException e) {
                throw new IllegalStateException(e);
            } finally {
                nested.remove();
            }
        }

        @Override public Map<String, Object> loadMapConfig(String name, String path) {
            return new HashMap<>(Map.of("value", value(name)));
        }

        @Override public <T> Object loadObjectConfig(String name, Class<T> type, String path) {
            TestConfig config = new TestConfig();
            config.setValue(value(name));
            return config;
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
            if (type == Map.class) return new HashMap<>(Map.of("value", "object"));
            TestConfig value = new TestConfig();
            value.setValue("object");
            return value;
        }
        public <T> Object loadObjectConfig(String name, Class<T> type) { return loadObjectConfig(name, type, ""); }
    }
}
