package com.networknt.config;

import com.networknt.test.IsolatedClasses;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import java.nio.file.Path;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class ExplicitPasswordProviderStartupTest {
    @TempDir Path directory;

    @Test
    void explicitProviderLoadsCryptThroughPublicConfig() throws Exception {
        new IsolatedClasses(getClass().getClassLoader(), "com.networknt.").invokeStatic(
                ConfigStartupScenarios.class.getName(), "run", new Class<?>[] {String.class, Path.class}, "valid", directory);
    }
}
