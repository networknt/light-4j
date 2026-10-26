package com.networknt.token.exchange;

import com.networknt.config.Config;
import com.networknt.jackson.testing.NativeBooleanBindings;
import com.networknt.jackson.testing.PrimitiveBooleanCases;
import com.networknt.token.exchange.schema.RequestSchema;
import com.networknt.token.exchange.schema.UpdateSchema;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

class NativePrimitiveBindingTest {
    @Test
    void everyBooleanHasAPrivatePrimitiveSetterAndNativeRegistration() throws Exception {
        for (Class<?> type : new Class<?>[]{TokenExchangeConfig.class, RequestSchema.class, UpdateSchema.class}) {
            NativeBooleanBindings.verify(type,
                    "/META-INF/native-image/com.networknt/token-exchange/reflect-config.json");
        }
    }

    @Test
    void preservesSharedCoercionDefaultsAndSerialization() {
        var config = Config.getInstance();
        var previous = config.getJsonMapConfig("token-exchange");
        config.putInConfigCache("token-exchange", new HashMap<String, Object>());
        try {
            PrimitiveBooleanCases.verify(TokenExchangeConfig.class, "enabled", TokenExchangeConfig::isEnabled, false, false, true);
            PrimitiveBooleanCases.verify(TokenExchangeConfig.class, "enableHttp2", TokenExchangeConfig::isEnableHttp2, false, false, true);
            PrimitiveBooleanCases.verify(RequestSchema.class, "cacheHttpClient", RequestSchema::isCacheHttpClient, false, false, true);
            PrimitiveBooleanCases.verify(RequestSchema.class, "cacheSSLContext", RequestSchema::isCacheSSLContext, false, false, true);
            PrimitiveBooleanCases.verify(RequestSchema.class, "enableHttp2", RequestSchema::isEnableHttp2, false, false, true);
            PrimitiveBooleanCases.verify(UpdateSchema.class, "updateExpirationFromTtl", UpdateSchema::isUpdateExpirationFromTtl, true, true, true);
        } finally {
            if (previous == null) config.clearConfigCache("token-exchange");
            else config.putInConfigCache("token-exchange", previous);
        }
    }
}
