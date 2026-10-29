package com.networknt.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class SystemPropertyExtensionTest {
    private static final String KEY = "networknt.extension.fixture";
    private static final String ORIGINAL = System.getProperty(KEY);
    @RegisterExtension final SystemPropertyExtension fixture = new SystemPropertyExtension(KEY, "fixture");

    @Test void fixtureIsInstalledForEachTest() { assertEquals("fixture", System.getProperty(KEY)); }
    @Test void testChangesDoNotEscapeTheScope() { System.setProperty(KEY, "changed"); }
    @AfterAll static void restoresOriginalAfterAllTests() {
        assertTrue(Objects.equals(ORIGINAL, System.getProperty(KEY)), "Extension must restore the original property");
    }
}
