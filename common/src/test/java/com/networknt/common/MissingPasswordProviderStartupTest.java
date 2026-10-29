package com.networknt.common;

import org.junit.jupiter.api.Test;

class MissingPasswordProviderStartupTest extends ServiceStartupSupport {
    @Test
    void validatesStartupBindings() throws Exception {
        check("com.networknt.decrypt.Decryptor", "com.networknt.decrypt.AESSaltDecryptor", com.networknt.decrypt.MissingMasterPasswordProviderException.class);
    }
}
