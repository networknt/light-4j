package com.networknt.common;

import org.junit.jupiter.api.Test;

class ExplicitPasswordProviderStartupTest extends ServiceStartupSupport {
    @Test
    void validatesStartupBindings() throws Exception {
        check("com.networknt.decrypt.Decryptor", "com.networknt.decrypt.AutoAESSaltDecryptor", null);
    }
}
