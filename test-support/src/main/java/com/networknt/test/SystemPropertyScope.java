package com.networknt.test;

import java.util.Objects;

/** Restores one system property when a test scope ends, including exceptional exits. */
public final class SystemPropertyScope implements AutoCloseable {
    private final String name;
    private final String previous;
    private boolean closed;

    /**
     * Sets or clears a property for this scope. Callers must serialize property-mutating tests.
     * @param name property name
     * @param value scoped value, or null to clear
     */
    public SystemPropertyScope(String name, String value) {
        this.name = Objects.requireNonNull(name);
        this.previous = System.getProperty(name);
        if (value == null) System.clearProperty(name);
        else System.setProperty(name, value);
    }

    /** Restores the original value once. */
    @Override public void close() {
        if (!closed) {
            closed = true;
            if (previous == null) System.clearProperty(name);
            else System.setProperty(name, previous);
        }
    }
}
