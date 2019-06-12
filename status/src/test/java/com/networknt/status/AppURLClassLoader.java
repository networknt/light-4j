package com.networknt.status;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Nicholas Azar
 *
 * Instead of using reflection to gain access to addURL for testing purposes.
 */
public class AppURLClassLoader extends URLClassLoader {

    public AppURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public void addURL(URL url) {
        super.addURL(url);
    }
}
