package com.networknt.common;

import org.junit.jupiter.api.Test;

class MissingSingletonClassStartupTest extends ServiceStartupSupport {
    @Test
    void preservesLegacyHandling() throws Exception {
        checkLegacy("java.lang.Runnable", "missing.Singleton", ClassNotFoundException.class);
    }
}
