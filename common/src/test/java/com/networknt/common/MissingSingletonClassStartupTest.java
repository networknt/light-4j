package com.networknt.common;

import org.junit.jupiter.api.Test;

class MissingSingletonClassStartupTest extends ServiceStartupSupport {
    @Test
    void validatesStartupBindings() throws Exception {
        check("java.lang.Runnable", "missing.Singleton", ClassNotFoundException.class);
    }
}
