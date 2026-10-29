package com.networknt.test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Test classloader that gives selected application classes fresh static state.
 * Class bytes and resources come from the actual runner classloader, including IDE
 * and Surefire loaders. No process, reconstructed classpath, or private API is used.
 */
public final class IsolatedClasses extends ClassLoader {
    private final String prefix;

    /**
     * Creates one isolation boundary. Unselected classes delegate to the runner.
     * @param source actual test-runner classloader
     * @param prefix application package prefix to isolate
     */
    public IsolatedClasses(ClassLoader source, String prefix) {
        if (prefix == null || prefix.isBlank()) throw new IllegalArgumentException("A package prefix is required");
        super(source);
        this.prefix = prefix;
    }

    @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!name.startsWith(prefix)) return super.loadClass(name, resolve);
        synchronized (getClassLoadingLock(name)) {
            Class<?> type = findLoadedClass(name);
            if (type == null) {
                String resource = name.replace('.', '/') + ".class";
                try (var input = getParent().getResourceAsStream(resource)) {
                    if (input == null) throw new ClassNotFoundException(name);
                    byte[] bytes = input.readAllBytes();
                    type = defineClass(name, bytes, 0, bytes.length);
                } catch (IOException failure) {
                    throw new ClassNotFoundException(name, failure);
                }
            }
            if (resolve) resolveClass(type);
            return type;
        }
    }

    /**
     * Calls a public static scenario with this loader as the context loader.
     * Runtime exceptions and errors propagate directly. Checked scenario failures
     * remain the cause of InvocationTargetException.
     * @param className scenario class
     * @param method public static method
     * @param parameterTypes shared parameter types, normally JDK classes
     * @param arguments scenario arguments
     * @return scenario result
     * @throws ReflectiveOperationException if reflection fails or the scenario throws a checked exception
     */
    public Object invokeStatic(String className, String method, Class<?>[] parameterTypes, Object... arguments) throws ReflectiveOperationException {
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(this);
        try {
            return loadClass(className).getMethod(method, parameterTypes).invoke(null, arguments);
        } catch (InvocationTargetException wrapper) {
            Throwable failure = wrapper.getCause();
            if (failure instanceof Error error) throw error;
            if (failure instanceof RuntimeException exception) throw exception;
            throw wrapper;
        } finally {
            thread.setContextClassLoader(previous);
        }
    }
}
