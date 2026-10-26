package com.networknt.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.networknt.jackson.testing.NativeBooleanBindings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NativeBooleanBindingsTest {
    static class Valid {
        @JsonProperty private boolean enabled;
        @JsonSetter("enabled") private void setEnabled(boolean value) { enabled = value; }
    }

    static class NewFieldWithoutSetter {
        @JsonProperty private boolean enabled;
        @JsonProperty private boolean newlyAdded;
        @JsonSetter("enabled") private void setEnabled(boolean value) { enabled = value; }
    }

    @Test
    void acceptsARegisteredPrivatePrimitiveSetter() {
        assertDoesNotThrow(() -> NativeBooleanBindings.verify(Valid.class, metadata(Valid.class, "boolean")));
    }

    @Test
    void rejectsANewBooleanEvenWhenExistingSettersAreValid() {
        assertThrows(AssertionError.class,
                () -> NativeBooleanBindings.verify(NewFieldWithoutSetter.class,
                        metadata(NewFieldWithoutSetter.class, "boolean")));
    }

    @Test
    void rejectsMissingAndBoxedNativeRegistrations() {
        assertThrows(AssertionError.class, () -> NativeBooleanBindings.verify(Valid.class,
                JacksonDefaults.mapper().readTree("[{\"name\":\"" + Valid.class.getName() + "\"}]")));
        assertThrows(AssertionError.class,
                () -> NativeBooleanBindings.verify(Valid.class, metadata(Valid.class, "java.lang.Boolean")));
    }

    private tools.jackson.databind.JsonNode metadata(Class<?> type, String parameter) {
        return JacksonDefaults.mapper().readTree("[{\"name\":\"" + type.getName()
                + "\",\"methods\":[{\"name\":\"setEnabled\",\"parameterTypes\":[\"" + parameter + "\"]}]}]");
    }
}
