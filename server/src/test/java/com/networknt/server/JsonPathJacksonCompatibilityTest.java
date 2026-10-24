package com.networknt.server;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonPathJacksonCompatibilityTest {
    public static class Primitive { public int count = 7; }

    @Test
    void providersRetainTrailingTokenNullPrimitiveAndDepthBehavior() {
        Configuration previous = Configuration.defaultConfiguration();
        try {
            JsonPathStartupHookProvider.configJsonPath();
            assertEquals(1, JsonPath.parse("{\"a\":1} garbage").read("$.a", Integer.class));
            Primitive value = JsonPath.parse("{\"count\":null}").read("$", Primitive.class);
            assertEquals(0, value.count);
            String nested = "[".repeat(600) + "0" + "]".repeat(600);
            assertNotNull(Configuration.defaultConfiguration().jsonProvider().parse(nested));
        } finally {
            Configuration.setDefaults(new Configuration.Defaults() {
                public com.jayway.jsonpath.spi.json.JsonProvider jsonProvider() { return previous.jsonProvider(); }
                public com.jayway.jsonpath.spi.mapper.MappingProvider mappingProvider() { return previous.mappingProvider(); }
                public Set<Option> options() { return previous.getOptions(); }
            });
        }
    }
}
