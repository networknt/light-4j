package com.networknt.status;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

import static java.lang.ClassLoader.getSystemClassLoader;

public class SeparateClassloaderTestRunner  extends BlockJUnit4ClassRunner {

    public SeparateClassloaderTestRunner(Class<?> clazz) throws InitializationError {
        super(getFromTestClassloader(clazz));
    }

    private static Class<?> getFromTestClassloader(Class<?> clazz) throws InitializationError {
        try {
            ClassLoader testClassLoader = new TestClassLoader();
            return Class.forName(clazz.getName(), true, testClassLoader);
        } catch (ClassNotFoundException e) {
            throw new InitializationError(e);
        }
    }

    public static class TestClassLoader extends URLClassLoader {
        public TestClassLoader() {
            super(getSystemURLs(), getSystemClassLoader());
//            super(((URLClassLoader)getSystemClassLoader()).getURLs());
        }

        private static URL[] getSystemURLs() {
            String classpath = System.getProperty("java.class.path");
            String[] entries = classpath.split(File.pathSeparator);
            URL[] result = new URL[entries.length];
            for(int i = 0; i < entries.length; i++) {
                try {
                    result[i] = Paths.get(entries[i]).toAbsolutePath().toUri().toURL();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
            return result;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.startsWith("com.networknt.")) {
                return super.findClass(name);
            }
            return super.loadClass(name);
        }
    }
}
