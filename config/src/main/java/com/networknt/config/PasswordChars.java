package com.networknt.config;

import java.util.Arrays;
import java.util.Objects;

/**
 * An owned temporary password copy. Use with try-with-resources and do not retain
 * value() beyond the scope. Closing does not erase the source String, a borrowed
 * configuration array, or copies retained by a security provider.
 */
public final class PasswordChars implements AutoCloseable {
    private final char[] value;
    private boolean closed;

    private PasswordChars(char[] value) {
        this.value = value;
    }

    /** Copies a required password while keeping nullable-password handling explicit. */
    public static PasswordChars required(String value) {
        return copyOf(Objects.requireNonNull(value, "Password is required"));
    }

    public static PasswordChars copyOf(String value) {
        return new PasswordChars(value == null ? null : value.toCharArray());
    }

    public static PasswordChars copyOf(char[] value) {
        return new PasswordChars(value == null ? null : value.clone());
    }

    public char[] value() {
        if (closed) throw new IllegalStateException("Password copy is closed");
        return value;
    }

    @Override
    public void close() {
        if (value != null) Arrays.fill(value, '\0');
        closed = true;
    }
}
