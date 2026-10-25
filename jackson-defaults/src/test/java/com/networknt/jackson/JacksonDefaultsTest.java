package com.networknt.jackson;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class JacksonDefaultsTest {
    public static class Input {
        public int value;
    }

    @Test
    void retainsFrozenJackson2CommonFeatureProfile() throws Exception {
        var expected = new Properties();
        try (var input = getClass().getResourceAsStream("/jackson-2.22.3-features.properties")) {
            assertNotNull(input);
            expected.load(input);
        }
        assertFalse(expected.isEmpty());
        for (ObjectMapper mapper : new ObjectMapper[]{JacksonDefaults.mapper(), JacksonDefaults.builder().build()}) {
            for (String name : expected.stringPropertyNames()) {
                String[] parts = name.split("\\.", 2);
                String namespace = parts[0].startsWith("Stream") ? "tools.jackson.core." : "tools.jackson.databind.";
                Class<?> type = Class.forName(namespace + parts[0]);
                Object feature = Arrays.stream(type.getEnumConstants())
                        .filter(value -> ((Enum<?>) value).name().equals(parts[1])).findFirst().orElseThrow();
                Method enabled = Arrays.stream(mapper.getClass().getMethods())
                        .filter(method -> method.getName().equals("isEnabled") && method.getParameterCount() == 1
                                && method.getParameterTypes()[0].isInstance(feature)).findFirst().orElseThrow();
                assertEquals(Boolean.parseBoolean(expected.getProperty(name)), enabled.invoke(mapper, feature), name);
            }
        }
    }

    @Test
    void streamLimitsAcceptLegacyDepthAndRejectExcessDepthAndNumbers() {
        var mapper = JacksonDefaults.mapper();
        assertDoesNotThrow(() -> mapper.readTree("[".repeat(600) + "0" + "]".repeat(600)));
        assertThrows(JacksonException.class, () -> mapper.readTree("[".repeat(1001) + "0" + "]".repeat(1001)));
        assertDoesNotThrow(() -> mapper.readTree("1".repeat(1000)));
        assertThrows(JacksonException.class, () -> mapper.readTree("1".repeat(1001)));
        assertEquals(20_000_000, mapper.tokenStreamFactory().streamReadConstraints().getMaxStringLength());
        assertEquals(50_000, mapper.tokenStreamFactory().streamReadConstraints().getMaxNameLength());
        assertEquals(1000, mapper.tokenStreamFactory().streamWriteConstraints().getMaxNestingDepth());
    }

    @Test
    void privateBuilderOverridesDoNotChangeSharedMapper() {
        var custom = JacksonDefaults.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
        assertEquals(7, custom.readValue("{\"value\":7,\"extra\":true}", Input.class).value);
        assertThrows(JacksonException.class, () -> JacksonDefaults.mapper()
                .readValue("{\"value\":7,\"extra\":true}", Input.class));
    }
}
