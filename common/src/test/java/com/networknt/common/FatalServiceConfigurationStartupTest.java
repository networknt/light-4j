package com.networknt.common;

import org.junit.jupiter.api.Test;

class FatalServiceConfigurationStartupTest extends ServiceStartupSupport {
    @Test
    void honorsStandardFatalSignalWithoutDecryptorSpecificHandling() throws Exception {
        check("java.lang.Runnable", FatalSingleton.class.getName(), java.util.ServiceConfigurationError.class);
    }
}
