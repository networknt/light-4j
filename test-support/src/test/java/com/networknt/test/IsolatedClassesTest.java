package com.networknt.test;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

public class IsolatedClassesTest {
    @Test void freshClassesDoNotShareStaticStateWithEachOtherOrTheRunner() throws Exception {
        int parentValue = Counter.next();
        for (int i = 0; i < 2; i++) {
            var classes = new IsolatedClasses(getClass().getClassLoader(), "com.networknt.test.");
            assertEquals(1, classes.invokeStatic(Counter.class.getName(), "next", new Class<?>[0]));
        }
        assertEquals(parentValue + 1, Counter.next());
    }

    @Test void failedInitializationDoesNotPoisonAnotherInvocationAndContextLoaderIsRestored() {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        for (int i = 0; i < 2; i++) {
            var classes = new IsolatedClasses(getClass().getClassLoader(), "com.networknt.test.");
            String brokenClass = Broken.class.getName();
            var failure = assertThrows(ExceptionInInitializerError.class,
                    () -> classes.invokeStatic(brokenClass, "run", new Class<?>[0]));
            assertInstanceOf(IllegalStateException.class, failure.getCause());
            assertSame(previous, Thread.currentThread().getContextClassLoader());
        }
    }

    @Test void checkedScenarioFailuresRetainTheirOriginalCause() {
        var classes = new IsolatedClasses(getClass().getClassLoader(), "com.networknt.test.");
        String checkedClass = CheckedFailure.class.getName();
        var failure = assertThrows(InvocationTargetException.class,
                () -> classes.invokeStatic(checkedClass, "run", new Class<?>[0]));
        assertInstanceOf(IOException.class, failure.getCause());
        assertEquals("Synthetic checked failure", failure.getCause().getMessage());
    }

    public static class CheckedFailure {
        public static void run() throws IOException { throw new IOException("Synthetic checked failure"); }
    }

    public static class Counter {
        private static int value;
        public static int next() { return ++value; }
    }
    public static class Broken {
        static { if (true) throw new IllegalStateException("Synthetic initialization failure"); }
        public static void run() {
            // Unreachable: the fixture deliberately fails during class initialization.
        }
    }
}
