package com.networknt.token.exchange;

import com.networknt.jackson.JacksonDefaults;
import com.networknt.token.exchange.schema.RequestSchema;
import com.networknt.token.exchange.schema.UpdateSchema;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.exc.MismatchedInputException;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativePrimitiveBindingTest {
    @Test
    void primitiveSettersRemainPrivate() throws Exception {
        for (Class<?> type : new Class<?>[]{TokenExchangeConfig.class, RequestSchema.class, UpdateSchema.class}) {
            for (var method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(com.fasterxml.jackson.annotation.JsonSetter.class)) {
                    assertTrue(Modifier.isPrivate(method.getModifiers()), method.getName());
                    assertArrayEquals(new Class<?>[]{boolean.class}, method.getParameterTypes());
                }
            }
        }
    }

    @Test
    void preservesPrimitiveStrictNullAndUpdateSkipPolicy() {
        for (boolean strict : new boolean[]{false, true}) {
            var mapper = JacksonDefaults.builder()
                    .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, strict).build();
            assertTrue(mapper.readValue("{\"updateExpirationFromTtl\":null}", UpdateSchema.class).isUpdateExpirationFromTtl());
            if (strict) {
                assertThrows(MismatchedInputException.class,
                        () -> mapper.readValue("{\"updateExpirationFromTtl\":\"\"}", UpdateSchema.class));
            } else {
                assertFalse(mapper.readValue("{\"updateExpirationFromTtl\":\"\"}", UpdateSchema.class).isUpdateExpirationFromTtl());
            }
            if (strict) {
                assertThrows(MismatchedInputException.class,
                        () -> mapper.readValue("{\"enableHttp2\":null}", RequestSchema.class));
            } else {
                assertFalse(mapper.readValue("{\"enableHttp2\":null}", RequestSchema.class).isEnableHttp2());
            }
            var request = mapper.readValue("{\"cacheHttpClient\":true,\"cacheSSLContext\":true,\"enableHttp2\":true,\"proxyPort\":8080}", RequestSchema.class);
            assertTrue(request.isCacheHttpClient());
            assertTrue(request.isCacheSSLContext());
            assertTrue(request.isEnableHttp2());
            assertEquals(8080, request.getProxyPort());
            assertTrue(mapper.readTree(mapper.writer().without(SerializationFeature.FAIL_ON_EMPTY_BEANS).writeValueAsString(request)).get("enableHttp2").booleanValue());
        }
    }
}
