package com.networknt.common;

import org.junit.jupiter.api.Test;

class FatalServiceConfigurationStartupTest extends ServiceStartupSupport {
    @Test
    void honorsExplicitFatalContractWithoutDecryptorSpecificHandling() throws Exception {
        check("java.lang.Runnable", FatalSingleton.class.getName(), FatalConfigurationException.class);
    }
}
