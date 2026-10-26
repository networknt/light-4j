package com.networknt.client;

import com.networknt.jackson.JacksonDefaults;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuthSignConfigBindingTest {
    @Test
    void omittedPropertyRetainsEnabledDefault() {
        assertTrue(JacksonDefaults.mapper().readValue("{}", OAuthSignConfig.class).isEnableHttp2());
    }

    @Test
    void preservesPrimitiveCoercionAndSerializationWithEitherNullPolicy() {
        for (boolean strict : new boolean[]{false, true}) {
            var mapper = JacksonDefaults.builder()
                    .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, strict).build();
            for (String value : new String[]{"true", "1", "2", "\"true\""}) {
                var config = mapper.readValue("{\"enableHttp2\":" + value + "}", OAuthSignConfig.class);
                assertTrue(config.isEnableHttp2(), value);
                assertTrue(mapper.readTree(mapper.writeValueAsString(config)).get("enableHttp2").booleanValue());
            }
            for (String value : new String[]{"false", "0", "\"false\""}) {
                var config = mapper.readValue("{\"enableHttp2\":" + value + "}", OAuthSignConfig.class);
                assertFalse(config.isEnableHttp2(), value);
                assertFalse(mapper.readTree(mapper.writeValueAsString(config)).get("enableHttp2").booleanValue());
            }
            for (String value : new String[]{"null", "\"\""}) {
                if (strict) {
                    assertThrows(MismatchedInputException.class,
                            () -> mapper.readValue("{\"enableHttp2\":" + value + "}", OAuthSignConfig.class));
                } else {
                    assertFalse(mapper.readValue("{\"enableHttp2\":" + value + "}", OAuthSignConfig.class).isEnableHttp2());
                }
            }
            for (String value : new String[]{"\"yes\"", "\"1\""}) {
                assertThrows(InvalidFormatException.class,
                        () -> mapper.readValue("{\"enableHttp2\":" + value + "}", OAuthSignConfig.class));
            }
            for (String value : new String[]{"[]", "0.0"}) {
                assertThrows(MismatchedInputException.class,
                        () -> mapper.readValue("{\"enableHttp2\":" + value + "}", OAuthSignConfig.class));
            }
        }
    }

    @Test
    void bindingMutatorIsPrivateAndPrimitive() throws Exception {
        var setter = OAuthSignConfig.class.getDeclaredMethod("setEnableHttp2", boolean.class);
        assertTrue(Modifier.isPrivate(setter.getModifiers()));
        assertThrows(NoSuchMethodException.class,
                () -> OAuthSignConfig.class.getMethod("setEnableHttp2", boolean.class));
    }
}
