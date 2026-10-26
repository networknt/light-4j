package com.networknt.client;

import com.networknt.jackson.testing.NativeBooleanBindings;
import com.networknt.jackson.testing.PrimitiveBooleanCases;
import org.junit.jupiter.api.Test;

class OAuthSignConfigBindingTest {
    @Test
    void preservesCoercionDefaultsAndSerialization() {
        PrimitiveBooleanCases.verify(OAuthSignConfig.class, "enableHttp2",
                OAuthSignConfig::isEnableHttp2, true, false, false);
    }

    @Test
    void everyBooleanHasAPrivatePrimitiveSetterAndNativeRegistration() throws Exception {
        NativeBooleanBindings.verify(OAuthSignConfig.class,
                "/META-INF/native-image/com.networknt/client-config/reflect-config.json");
    }
}
