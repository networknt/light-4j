package com.networknt.client;

import com.networknt.config.Config;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuthSignConfigBindingTest {
    @Test
    void omittedPropertyRetainsEnabledDefault() {
        assertTrue(read("{}").isEnableHttp2());
    }

    @Test
    void preservesPrimitiveBooleanCoercion() {
        for (String value : new String[]{"true", "1", "2", "\"true\""}) {
            assertTrue(read("{\"enableHttp2\":" + value + "}").isEnableHttp2(), value);
        }
        for (String value : new String[]{"false", "null", "0", "\"false\""}) {
            assertFalse(read("{\"enableHttp2\":" + value + "}").isEnableHttp2(), value);
        }
    }

    @Test
    void preservesInvalidInputRejection() {
        assertThrows(InvalidFormatException.class, () -> read("{\"enableHttp2\":\"yes\"}"));
        assertThrows(MismatchedInputException.class, () -> read("{\"enableHttp2\":[]}"));
    }

    private OAuthSignConfig read(String json) {
        return Config.getInstance().getMapper().readValue(json, OAuthSignConfig.class);
    }
}
