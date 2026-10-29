package com.networknt.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import static org.junit.jupiter.api.Assertions.*;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class SystemPropertyScopeTest {
    private static final String KEY = "networknt.scope.fixture";

    @Test void restoresAfterExceptionalExit() {
        try (var outer = new SystemPropertyScope(KEY, "outer")) {
            assertThrows(IllegalStateException.class, () -> {
                try (var inner = new SystemPropertyScope(KEY, "inner")) {
                    assertEquals("inner", System.getProperty(KEY));
                    throw new IllegalStateException("Synthetic failure");
                }
            });
            assertEquals("outer", System.getProperty(KEY));
        }
    }

    @Test void restoresAClearedProperty() {
        try (var outer = new SystemPropertyScope(KEY, "outer")) {
            try (var inner = new SystemPropertyScope(KEY, null)) {
                assertNull(System.getProperty(KEY));
            }
            assertEquals("outer", System.getProperty(KEY));
        }
    }

    @Test void repeatedCloseDoesNotOverwriteLaterChanges() {
        try (var outer = new SystemPropertyScope(KEY, "outer")) {
            var inner = new SystemPropertyScope(KEY, "inner");
            inner.close();
            System.setProperty(KEY, "later");
            inner.close();
            assertEquals("later", System.getProperty(KEY));
        }
    }
}
