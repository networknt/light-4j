package com.networknt.config;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class Jackson2FeatureDefaultsTest {
    private void assertSnapshot(ObjectMapper mapper, String resource) throws Exception {
        Properties expected = new Properties();
        try (InputStream input = getClass().getResourceAsStream(resource)) {
            assertNotNull(input, resource);
            expected.load(input);
        }
        assertFalse(expected.isEmpty());
        for (String name : expected.stringPropertyNames()) {
            String[] parts = name.split("\\.", 2);
            String namespace = parts[0].startsWith("Stream") ? "tools.jackson.core."
                    : "tools.jackson.databind." + (parts[0].equals("EnumFeature")
                    || parts[0].equals("JsonNodeFeature") || parts[0].equals("DateTimeFeature") ? "cfg." : "");
            Class<?> type = Class.forName(namespace + parts[0]);
            Object feature = java.util.Arrays.stream(type.getEnumConstants())
                    .filter(value -> ((Enum<?>) value).name().equals(parts[1])).findFirst().orElseThrow();
            boolean checked = false;
            for (Method method : mapper.getClass().getMethods()) {
                if (method.getName().equals("isEnabled") && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isInstance(feature)) {
                    assertEquals(Boolean.parseBoolean(expected.getProperty(name)), method.invoke(mapper, feature), name);
                    checked = true;
                    break;
                }
            }
            assertTrue(checked, "Feature must be checked: " + name);
        }
    }

    @Test
    void retainsMeasuredJackson2FeatureDefaults() throws Exception {
        assertSame(com.networknt.jackson.JacksonDefaults.mapper(), Jackson2Defaults.mapper());
        assertSnapshot(Jackson2Defaults.builder().build(), "/jackson-2.22.3-config-features.properties");
        assertSnapshot(Config.getInstance().getMapper(), "/jackson-2.22.3-config-features.properties");
        assertSnapshot(JsonMapper.objectMapper, "/jackson-2.22.3-jsonmapper-features.properties");
    }
}
