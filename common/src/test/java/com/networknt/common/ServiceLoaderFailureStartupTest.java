package com.networknt.common;

import org.junit.jupiter.api.Test;

class ServiceLoaderFailureStartupTest extends ServiceStartupSupport {
    @Test
    void genuineServiceLoaderErrorDoesNotOptIntoFatalContract() throws Exception {
        checkLegacy("java.lang.Runnable", ServiceLoaderSingleton.class.getName(), java.util.ServiceConfigurationError.class);
    }
}
