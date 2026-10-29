package com.networknt.common;

import org.junit.jupiter.api.Test;

class ConstructorFallbackStartupTest extends ServiceStartupSupport {
    @Test
    void validatesStartupBindings() throws Exception {
        check("com.networknt.decrypt.Decryptor", ConstructorFallback.class.getName(), com.networknt.decrypt.MissingMasterPasswordProviderException.class);
    }
}
