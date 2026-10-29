package com.networknt.test;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** JUnit fixture property setup and restoration, independent of Maven configuration. */
public final class SystemPropertyExtension implements BeforeEachCallback, AfterEachCallback {
    private final String name;
    private final String value;
    private final ExtensionContext.Namespace namespace = ExtensionContext.Namespace.create(this);

    /**
     * Defines a per-test property. Use a SYSTEM_PROPERTIES resource lock for parallel tests.
     * @param name property name
     * @param value fixture value, or null to clear
     */
    public SystemPropertyExtension(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override public void beforeEach(ExtensionContext context) {
        context.getStore(namespace).put("scope", new SystemPropertyScope(name, value));
    }

    @Override public void afterEach(ExtensionContext context) {
        var scope = context.getStore(namespace).remove("scope", SystemPropertyScope.class);
        if (scope != null) scope.close();
    }
}
