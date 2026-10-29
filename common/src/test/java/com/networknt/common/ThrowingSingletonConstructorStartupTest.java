package com.networknt.common;

import org.junit.jupiter.api.Test;

class ThrowingSingletonConstructorStartupTest extends ServiceStartupSupport {
    @Test
    void validatesStartupBindings() throws Exception {
        check("java.lang.Runnable", BrokenSingleton.class.getName(), IllegalArgumentException.class);
    }
}
